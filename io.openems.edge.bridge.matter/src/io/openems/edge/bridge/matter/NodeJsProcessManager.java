package io.openems.edge.bridge.matter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParser;

/**
 * Manages the Node.js matter-server subprocess.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Detect and validate Node.js installation
 * <li>Extract embedded JS sources from the bundle JAR
 * <li>Run npm install if node_modules is missing
 * <li>Start the Node.js subprocess
 * <li>Monitor stdout/stderr and detect crashes
 * <li>Auto-restart with exponential backoff
 * </ul>
 */
public class NodeJsProcessManager {

	private static final Logger LOG = LoggerFactory.getLogger(NodeJsProcessManager.class);

	private static final int MIN_NODEJS_VERSION = 18;
	private static final long INITIAL_RESTART_DELAY_MS = 1000;
	private static final long MAX_RESTART_DELAY_MS = 60_000;
	private static final String[] NODEJS_SEARCH_PATHS = { //
			"/usr/bin/node", //
			"/usr/local/bin/node", //
			"/usr/bin/nodejs", //
			"/snap/bin/node", //
	};

	private static final String[] RESOURCE_FILES = { //
			"package.json", //
			"src/index.js", //
			"src/matter-controller.js", //
			"src/ws-handler.js", //
	};

	private final String configuredNodejsPath;
	private final Path storagePath;
	private final int websocketPort;
	private final boolean debugMode;
	private final IntConsumer onPortReady;
	private final Runnable onCrash;
	private final Consumer<String> onStdout;
	private final Consumer<String> onStderr;

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
		var t = new Thread(r, "Matter-ProcessManager");
		t.setDaemon(true);
		return t;
	});

	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicInteger restartCount = new AtomicInteger(0);

	private volatile Process process;
	private volatile String nodejsPath;
	private volatile ScheduledFuture<?> restartFuture;

	/**
	 * Creates a new {@link NodeJsProcessManager}.
	 *
	 * @param configuredNodejsPath configured path to Node.js binary (may be empty)
	 * @param storagePath          path for runtime data storage
	 * @param websocketPort        port for WebSocket server (0 = random)
	 * @param debugMode            enable debug logging
	 * @param onPortReady          callback when the port is known
	 * @param onCrash              callback when the process crashes
	 * @param onStdout             callback for stdout lines
	 * @param onStderr             callback for stderr lines
	 */
	public NodeJsProcessManager(String configuredNodejsPath, Path storagePath, int websocketPort, boolean debugMode,
			IntConsumer onPortReady, Runnable onCrash, Consumer<String> onStdout, Consumer<String> onStderr) {
		this.configuredNodejsPath = configuredNodejsPath;
		this.storagePath = storagePath;
		this.websocketPort = websocketPort;
		this.debugMode = debugMode;
		this.onPortReady = onPortReady;
		this.onCrash = onCrash;
		this.onStdout = onStdout;
		this.onStderr = onStderr;
	}

	/**
	 * Detects the Node.js binary path.
	 *
	 * @return the path to the Node.js binary
	 * @throws IOException if Node.js cannot be found
	 */
	public String detectNodeJs() throws IOException {
		// 1. Use configured path
		if (this.configuredNodejsPath != null && !this.configuredNodejsPath.isEmpty()) {
			var file = new File(this.configuredNodejsPath);
			if (file.exists() && file.canExecute()) {
				this.nodejsPath = this.configuredNodejsPath;
				return this.nodejsPath;
			}
			throw new IOException("Configured Node.js path not found or not executable: " + this.configuredNodejsPath);
		}

		// 2. Try PATH lookup via 'which'
		try {
			var whichProcess = new ProcessBuilder("which", "node").redirectErrorStream(true).start();
			var path = new String(whichProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (whichProcess.waitFor(5, TimeUnit.SECONDS) && whichProcess.exitValue() == 0 && !path.isEmpty()) {
				this.nodejsPath = path;
				return this.nodejsPath;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// 3. Try common locations
		for (var candidate : NODEJS_SEARCH_PATHS) {
			var file = new File(candidate);
			if (file.exists() && file.canExecute()) {
				this.nodejsPath = candidate;
				return this.nodejsPath;
			}
		}

		throw new IOException("Node.js runtime not found. Install Node.js >= " + MIN_NODEJS_VERSION
				+ " or configure the path in the component settings.");
	}

	/**
	 * Verifies that the detected Node.js version meets the minimum requirement.
	 *
	 * @return the detected version string
	 * @throws IOException if version check fails
	 */
	public String verifyNodeJsVersion() throws IOException {
		if (this.nodejsPath == null) {
			throw new IOException("Node.js path not set. Call detectNodeJs() first.");
		}
		try {
			var versionProcess = new ProcessBuilder(this.nodejsPath, "--version").redirectErrorStream(true).start();
			var versionString = new String(versionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
					.trim();
			if (!versionProcess.waitFor(5, TimeUnit.SECONDS) || versionProcess.exitValue() != 0) {
				throw new IOException("Failed to get Node.js version");
			}
			// Parse version like "v18.17.1" or "v20.0.0"
			var version = versionString.replaceFirst("^v", "");
			var majorVersion = Integer.parseInt(version.split("\\.")[0]);
			if (majorVersion < MIN_NODEJS_VERSION) {
				throw new IOException("Node.js version " + versionString + " is too old. Minimum required: v"
						+ MIN_NODEJS_VERSION);
			}
			LOG.info("Detected Node.js {}", versionString);
			return versionString;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Node.js version check interrupted", e);
		}
	}

	/**
	 * Extracts the embedded Node.js sources from the bundle to the runtime
	 * directory.
	 *
	 * @param bundleClass a class from the bundle to resolve resources
	 * @throws IOException if extraction fails
	 */
	public void extractResources(Class<?> bundleClass) throws IOException {
		var runtimePath = this.storagePath.resolve("runtime");
		Files.createDirectories(runtimePath);
		Files.createDirectories(runtimePath.resolve("src"));

		for (var resourceFile : RESOURCE_FILES) {
			var resourcePath = "/node/" + resourceFile;
			try (var is = bundleClass.getResourceAsStream(resourcePath)) {
				if (is == null) {
					throw new IOException("Resource not found in bundle: " + resourcePath);
				}
				var targetFile = runtimePath.resolve(resourceFile);
				Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
				if (this.debugMode) {
					LOG.debug("Extracted: {}", targetFile);
				}
			}
		}
	}

	/**
	 * Runs npm install if node_modules directory is missing.
	 *
	 * @return a {@link CompletableFuture} that completes when npm install finishes
	 */
	public CompletableFuture<Void> ensureNpmInstalled() {
		var runtimePath = this.storagePath.resolve("runtime");
		var nodeModulesDir = runtimePath.resolve("node_modules");

		if (Files.exists(nodeModulesDir)) {
			LOG.info("node_modules already exists, skipping npm install");
			return CompletableFuture.completedFuture(null);
		}

		return CompletableFuture.runAsync(() -> {
			try {
				LOG.info("Running npm install in {}", runtimePath);
				var npmProcess = new ProcessBuilder("npm", "install", "--production")//
						.directory(runtimePath.toFile())//
						.redirectErrorStream(true)//
						.start();

				// Log npm output
				try (var reader = new BufferedReader(
						new InputStreamReader(npmProcess.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						LOG.info("[npm] {}", line);
					}
				}

				if (!npmProcess.waitFor(120, TimeUnit.SECONDS)) {
					npmProcess.destroyForcibly();
					throw new IOException("npm install timed out after 120 seconds");
				}

				if (npmProcess.exitValue() != 0) {
					throw new IOException("npm install failed with exit code " + npmProcess.exitValue());
				}

				LOG.info("npm install completed successfully");
			} catch (IOException | InterruptedException e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new RuntimeException("npm install failed", e);
			}
		}, this.executor);
	}

	/**
	 * Starts the Node.js matter-server subprocess.
	 */
	public void start() {
		if (!this.running.compareAndSet(false, true)) {
			LOG.warn("Process manager already running");
			return;
		}
		this.restartCount.set(0);
		this.executor.execute(this::startProcess);
	}

	/**
	 * Stops the Node.js matter-server subprocess.
	 */
	public void stop() {
		this.running.set(false);

		if (this.restartFuture != null) {
			this.restartFuture.cancel(false);
			this.restartFuture = null;
		}

		var proc = this.process;
		if (proc != null && proc.isAlive()) {
			LOG.info("Stopping Node.js matter-server process");
			proc.destroy();
			try {
				if (!proc.waitFor(5, TimeUnit.SECONDS)) {
					proc.destroyForcibly();
				}
			} catch (InterruptedException e) {
				proc.destroyForcibly();
				Thread.currentThread().interrupt();
			}
		}
		this.process = null;
	}

	/**
	 * Shuts down the process manager and its executor.
	 */
	public void shutdown() {
		this.stop();
		this.executor.shutdownNow();
	}

	/**
	 * Checks if the Node.js process is currently alive.
	 *
	 * @return true if the process is running
	 */
	public boolean isProcessAlive() {
		var proc = this.process;
		return proc != null && proc.isAlive();
	}

	private void startProcess() {
		if (!this.running.get()) {
			return;
		}

		var runtimePath = this.storagePath.resolve("runtime");
		var entryPoint = runtimePath.resolve("src/index.js");

		try {
			var pb = new ProcessBuilder(this.nodejsPath, entryPoint.toString(), //
					"--port", String.valueOf(this.websocketPort), //
					"--storage", this.storagePath.toString());
			pb.directory(runtimePath.toFile());
			pb.redirectErrorStream(false);

			if (this.debugMode) {
				LOG.debug("Starting: {} {} --port {} --storage {}", this.nodejsPath, entryPoint, this.websocketPort,
						this.storagePath);
			}

			this.process = pb.start();

			// Monitor stdout for port announcement and log output
			startStreamReader("matter-stdout", this.process.getInputStream(), line -> {
				// Look for port announcement: {"port":12345}
				if (line.startsWith("{") && line.contains("\"port\"")) {
					try {
						var json = JsonParser.parseString(line).getAsJsonObject();
						var port = json.get("port").getAsInt();
						LOG.info("Matter server listening on port {}", port);
						this.restartCount.set(0); // successful start resets backoff
						this.onPortReady.accept(port);
					} catch (Exception e) {
						LOG.warn("Failed to parse port from stdout: {}", line);
					}
				} else {
					this.onStdout.accept(line);
				}
			});

			// Monitor stderr
			startStreamReader("matter-stderr", this.process.getErrorStream(), this.onStderr);

			// Monitor process exit
			this.process.onExit().thenAcceptAsync(p -> {
				var exitCode = p.exitValue();
				LOG.warn("Node.js matter-server exited with code {}", exitCode);
				this.process = null;
				this.onCrash.run();
				this.scheduleRestart();
			}, this.executor);

		} catch (IOException e) {
			LOG.error("Failed to start Node.js matter-server: {}", e.getMessage());
			this.onCrash.run();
			this.scheduleRestart();
		}
	}

	private void scheduleRestart() {
		if (!this.running.get()) {
			return;
		}

		var count = this.restartCount.incrementAndGet();
		var delay = Math.min(INITIAL_RESTART_DELAY_MS * (1L << Math.min(count - 1, 6)), MAX_RESTART_DELAY_MS);
		LOG.info("Scheduling restart attempt {} in {} ms", count, delay);
		this.restartFuture = this.executor.schedule(this::startProcess, delay, TimeUnit.MILLISECONDS);
	}

	private static void startStreamReader(String name, InputStream stream, Consumer<String> lineConsumer) {
		var thread = new Thread(() -> {
			try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					lineConsumer.accept(line);
				}
			} catch (IOException e) {
				// Stream closed, process likely terminated
			}
		}, name);
		thread.setDaemon(true);
		thread.start();
	}
}
