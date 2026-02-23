package io.openems.edge.bridge.matter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.matter.api.BridgeMatter;
import io.openems.edge.bridge.matter.api.MatterAttributeUpdate;
import io.openems.edge.bridge.matter.api.MatterDeviceInfo;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.Matter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BridgeMatterImpl extends AbstractOpenemsComponent implements BridgeMatter, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeMatterImpl.class);

	private Config config;
	private NodeJsProcessManager processManager;
	private MatterWebSocketClient wsClient;

	public BridgeMatterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMatter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!config.enabled()) {
			return;
		}

		this.startBridge();
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.stopBridge();
		this.config = config;

		if (config.enabled()) {
			this.startBridge();
		}
	}

	@Deactivate
	protected void deactivate() {
		this.stopBridge();
		super.deactivate();
	}

	/**
	 * Starts the Matter bridge: detect Node.js, extract resources, start process,
	 * connect WebSocket.
	 */
	private void startBridge() {
		var storagePath = Path.of(this.config.storagePath());

		this.processManager = new NodeJsProcessManager(//
				this.config.nodejsPath(), //
				storagePath, //
				this.config.websocketPort(), //
				this.config.debugMode(), //
				this::onPortReady, //
				this::onProcessCrash, //
				line -> {
					if (this.config.debugMode()) {
						this.log.info("[matter.js] {}", line);
					}
				}, //
				line -> this.log.warn("[matter.js] {}", line) //
		);

		try {
			// Detect Node.js
			this.processManager.detectNodeJs();
			this._setNodejsNotFound(false);

			// Verify version
			this.processManager.verifyNodeJsVersion();

			// Extract JS resources from bundle
			this.processManager.extractResources(BridgeMatterImpl.class);

			// Install npm dependencies and then start
			this.processManager.ensureNpmInstalled().thenRun(() -> {
				this.processManager.start();
			}).exceptionally(e -> {
				this.log.error("Failed to install npm dependencies: {}", e.getMessage());
				this._setConnectionFailed(true);
				return null;
			});

		} catch (IOException e) {
			this.log.error("Failed to start Matter bridge: {}", e.getMessage());
			if (e.getMessage().contains("not found")) {
				this._setNodejsNotFound(true);
			} else {
				this._setConnectionFailed(true);
			}
		}
	}

	/**
	 * Stops the Matter bridge: disconnect WebSocket, stop process.
	 */
	private void stopBridge() {
		if (this.wsClient != null) {
			try {
				this.wsClient.closeBlocking();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			this.wsClient = null;
		}

		if (this.processManager != null) {
			this.processManager.shutdown();
			this.processManager = null;
		}

		this._setConnected(false);
	}

	/**
	 * Called when the Node.js process announces its WebSocket port.
	 */
	private void onPortReady(int port) {
		this.log.info("Connecting WebSocket to matter-server on port {}", port);

		try {
			var uri = new URI("ws://127.0.0.1:" + port);
			this.wsClient = new MatterWebSocketClient(uri, this.config.debugMode(), //
					() -> {
						// onConnected
						this._setConnected(true);
						this._setConnectionFailed(false);
						this._setProcessCrashed(false);
					}, //
					() -> {
						// onDisconnected
						this._setConnected(false);
					});
			this.wsClient.connect();
		} catch (Exception e) {
			this.log.error("Failed to create WebSocket connection: {}", e.getMessage());
			this._setConnectionFailed(true);
		}
	}

	/**
	 * Called when the Node.js process crashes.
	 */
	private void onProcessCrash() {
		this._setProcessCrashed(true);
		this._setConnected(false);

		// Close existing WebSocket if any
		if (this.wsClient != null) {
			this.wsClient.close();
			this.wsClient = null;
		}
	}

	@Override
	public boolean isConnected() {
		return this.wsClient != null && this.wsClient.isOpen();
	}

	@Override
	public CompletableFuture<List<MatterDeviceInfo>> getDiscoveredDevices() {
		if (this.wsClient == null || !this.wsClient.isOpen()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to matter-server"));
		}
		return this.wsClient.getDevices();
	}

	@Override
	public CompletableFuture<MatterDeviceInfo> commissionDevice(String pairingCode) {
		if (this.wsClient == null || !this.wsClient.isOpen()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to matter-server"));
		}
		return this.wsClient.commissionDevice(pairingCode);
	}

	@Override
	public CompletableFuture<Void> subscribeAttributes(long nodeId, int endpointId, int clusterId,
			List<Integer> attributeIds, Consumer<MatterAttributeUpdate> callback) {
		if (this.wsClient == null || !this.wsClient.isOpen()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to matter-server"));
		}
		return this.wsClient.subscribeAttributes(nodeId, endpointId, clusterId, attributeIds, callback);
	}

	@Override
	public CompletableFuture<Object> readAttribute(long nodeId, int endpointId, int clusterId, int attributeId) {
		if (this.wsClient == null || !this.wsClient.isOpen()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to matter-server"));
		}
		return this.wsClient.readAttribute(nodeId, endpointId, clusterId, attributeId);
	}

	@Override
	public CompletableFuture<Void> decommissionDevice(long nodeId) {
		if (this.wsClient == null || !this.wsClient.isOpen()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Not connected to matter-server"));
		}
		return this.wsClient.decommissionDevice(nodeId);
	}

	@Override
	public String debugLog() {
		return "Matter:" + (this.isConnected() ? "connected" : "disconnected");
	}

	private void _setConnected(boolean value) {
		this.channel(BridgeMatter.ChannelId.CONNECTED).setNextValue(value);
	}

	private void _setConnectionFailed(boolean value) {
		this.channel(BridgeMatter.ChannelId.CONNECTION_FAILED).setNextValue(value);
	}

	private void _setNodejsNotFound(boolean value) {
		this.channel(BridgeMatter.ChannelId.NODEJS_NOT_FOUND).setNextValue(value);
	}

	private void _setProcessCrashed(boolean value) {
		this.channel(BridgeMatter.ChannelId.PROCESS_CRASHED).setNextValue(value);
	}
}
