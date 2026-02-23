package io.openems.edge.bridge.matter;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.bridge.matter.api.MatterAttributeUpdate;
import io.openems.edge.bridge.matter.api.MatterDeviceInfo;

/**
 * WebSocket client for communicating with the Node.js matter-server via
 * JSON-RPC 2.0.
 */
public class MatterWebSocketClient extends WebSocketClient {

	private static final Logger LOG = LoggerFactory.getLogger(MatterWebSocketClient.class);
	private static final long REQUEST_TIMEOUT_SECONDS = 30;

	private final Map<String, CompletableFuture<JsonElement>> pendingRequests = new ConcurrentHashMap<>();
	private final Map<String, Consumer<MatterAttributeUpdate>> attributeCallbacks = new ConcurrentHashMap<>();
	private final boolean debugMode;
	private final Runnable onConnected;
	private final Runnable onDisconnected;

	/**
	 * Creates a new {@link MatterWebSocketClient}.
	 *
	 * @param uri            the WebSocket server URI
	 * @param debugMode      enable debug logging
	 * @param onConnected    callback when connection is established
	 * @param onDisconnected callback when connection is lost
	 */
	public MatterWebSocketClient(URI uri, boolean debugMode, Runnable onConnected, Runnable onDisconnected) {
		super(uri);
		this.debugMode = debugMode;
		this.onConnected = onConnected;
		this.onDisconnected = onDisconnected;
		this.setConnectionLostTimeout(10);
	}

	@Override
	public void onOpen(ServerHandshake handshake) {
		LOG.info("WebSocket connected to matter-server");
		this.onConnected.run();
	}

	@Override
	public void onMessage(String message) {
		if (this.debugMode) {
			LOG.debug("WS received: {}", message);
		}

		try {
			var json = JsonParser.parseString(message).getAsJsonObject();

			if (json.has("id") && !json.get("id").isJsonNull()) {
				// This is a response to a request
				var id = json.get("id").getAsString();
				var future = this.pendingRequests.remove(id);
				if (future != null) {
					if (json.has("error")) {
						var error = json.getAsJsonObject("error");
						future.completeExceptionally(
								new RuntimeException(error.get("message").getAsString()));
					} else {
						future.complete(json.get("result"));
					}
				}
			} else if (json.has("method")) {
				// This is a notification
				this.handleNotification(json);
			}
		} catch (Exception e) {
			LOG.error("Failed to parse WebSocket message: {}", e.getMessage());
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		LOG.info("WebSocket closed: code={}, reason={}, remote={}", code, reason, remote);
		// Fail all pending requests
		this.pendingRequests.forEach((id, future) -> {
			future.completeExceptionally(new RuntimeException("WebSocket connection closed"));
		});
		this.pendingRequests.clear();
		this.onDisconnected.run();
	}

	@Override
	public void onError(Exception ex) {
		LOG.error("WebSocket error: {}", ex.getMessage());
	}

	/**
	 * Sends a JSON-RPC request and returns a future for the result.
	 *
	 * @param method the method name
	 * @param params the parameters (may be null)
	 * @return a {@link CompletableFuture} with the result
	 */
	public CompletableFuture<JsonElement> sendRequest(String method, JsonObject params) {
		var id = UUID.randomUUID().toString();
		var request = new JsonObject();
		request.addProperty("jsonrpc", "2.0");
		request.addProperty("id", id);
		request.addProperty("method", method);
		if (params != null) {
			request.add("params", params);
		}

		var future = new CompletableFuture<JsonElement>();
		this.pendingRequests.put(id, future);

		if (this.debugMode) {
			LOG.debug("WS sending: {}", request);
		}

		try {
			this.send(request.toString());
		} catch (Exception e) {
			this.pendingRequests.remove(id);
			future.completeExceptionally(e);
			return future;
		}

		// Add timeout
		future.orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

		return future;
	}

	/**
	 * Gets the list of discovered devices.
	 *
	 * @return a {@link CompletableFuture} with the device list
	 */
	public CompletableFuture<List<MatterDeviceInfo>> getDevices() {
		return this.sendRequest("getDevices", null).thenApply(result -> {
			var devices = new java.util.ArrayList<MatterDeviceInfo>();
			if (result != null && result.isJsonArray()) {
				for (var element : result.getAsJsonArray()) {
					devices.add(parseDeviceInfo(element.getAsJsonObject()));
				}
			}
			return devices;
		});
	}

	/**
	 * Commissions a new device.
	 *
	 * @param pairingCode the pairing code
	 * @return a {@link CompletableFuture} with the device info
	 */
	public CompletableFuture<MatterDeviceInfo> commissionDevice(String pairingCode) {
		var params = new JsonObject();
		params.addProperty("pairingCode", pairingCode);
		return this.sendRequest("commissionDevice", params)
				.thenApply(result -> parseDeviceInfo(result.getAsJsonObject()));
	}

	/**
	 * Decommissions a device.
	 *
	 * @param nodeId the node ID
	 * @return a {@link CompletableFuture}
	 */
	public CompletableFuture<Void> decommissionDevice(long nodeId) {
		var params = new JsonObject();
		params.addProperty("nodeId", nodeId);
		return this.sendRequest("decommissionDevice", params).thenApply(r -> null);
	}

	/**
	 * Subscribes to attribute updates.
	 *
	 * @param nodeId       the node ID
	 * @param endpointId   the endpoint ID
	 * @param clusterId    the cluster ID
	 * @param attributeIds the attribute IDs
	 * @param callback     the callback for updates
	 * @return a {@link CompletableFuture}
	 */
	public CompletableFuture<Void> subscribeAttributes(long nodeId, int endpointId, int clusterId,
			List<Integer> attributeIds, Consumer<MatterAttributeUpdate> callback) {
		// Register callback
		var key = nodeId + ":" + endpointId + ":" + clusterId;
		this.attributeCallbacks.put(key, callback);

		var params = new JsonObject();
		params.addProperty("nodeId", nodeId);
		params.addProperty("endpointId", endpointId);
		params.addProperty("clusterId", clusterId);
		var attrArray = new JsonArray();
		for (var attrId : attributeIds) {
			attrArray.add(attrId);
		}
		params.add("attributeIds", attrArray);

		return this.sendRequest("subscribeAttributes", params).thenApply(r -> null);
	}

	/**
	 * Reads a single attribute.
	 *
	 * @param nodeId      the node ID
	 * @param endpointId  the endpoint ID
	 * @param clusterId   the cluster ID
	 * @param attributeId the attribute ID
	 * @return a {@link CompletableFuture} with the attribute value
	 */
	public CompletableFuture<Object> readAttribute(long nodeId, int endpointId, int clusterId, int attributeId) {
		var params = new JsonObject();
		params.addProperty("nodeId", nodeId);
		params.addProperty("endpointId", endpointId);
		params.addProperty("clusterId", clusterId);
		params.addProperty("attributeId", attributeId);

		return this.sendRequest("readAttribute", params).thenApply(result -> {
			if (result != null && result.isJsonObject()) {
				var value = result.getAsJsonObject().get("value");
				return jsonToJava(value);
			}
			return null;
		});
	}

	private void handleNotification(JsonObject json) {
		var method = json.get("method").getAsString();
		var params = json.has("params") ? json.getAsJsonObject("params") : null;

		switch (method) {
		case "attributeUpdate" -> {
			if (params != null) {
				var update = parseAttributeUpdate(params);
				var key = update.nodeId() + ":" + update.endpointId() + ":" + update.clusterId();
				var callback = this.attributeCallbacks.get(key);
				if (callback != null) {
					try {
						callback.accept(update);
					} catch (Exception e) {
						LOG.error("Error in attribute callback: {}", e.getMessage());
					}
				}
			}
		}
		case "deviceStateChange" -> {
			if (params != null) {
				LOG.info("Device state change: nodeId={}, state={}", //
						params.get("nodeId"), params.get("state"));
			}
		}
		case "ready" -> LOG.info("Matter server reports ready");
		default -> {
			if (this.debugMode) {
				LOG.debug("Unknown notification: {}", method);
			}
		}
		}
	}

	private static MatterDeviceInfo parseDeviceInfo(JsonObject json) {
		var nodeId = Long.parseLong(json.get("nodeId").getAsString());
		var vendorName = json.has("vendorName") ? json.get("vendorName").getAsString() : "Unknown";
		var productName = json.has("productName") ? json.get("productName").getAsString() : "Unknown";
		var serialNumber = json.has("serialNumber") && !json.get("serialNumber").isJsonNull()
				? json.get("serialNumber").getAsString()
				: null;

		var endpoints = new java.util.ArrayList<Integer>();
		if (json.has("endpoints") && json.get("endpoints").isJsonArray()) {
			for (var ep : json.getAsJsonArray("endpoints")) {
				endpoints.add(ep.getAsInt());
			}
		}

		return new MatterDeviceInfo(nodeId, vendorName, productName, serialNumber, endpoints);
	}

	private static MatterAttributeUpdate parseAttributeUpdate(JsonObject json) {
		var nodeId = Long.parseLong(json.get("nodeId").getAsString());
		var endpointId = json.get("endpointId").getAsInt();
		var clusterId = json.get("clusterId").getAsInt();
		var attributeId = json.get("attributeId").getAsInt();
		var value = json.has("value") ? jsonToJava(json.get("value")) : null;
		return new MatterAttributeUpdate(nodeId, endpointId, clusterId, attributeId, value);
	}

	private static Object jsonToJava(JsonElement element) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		if (element.isJsonPrimitive()) {
			var prim = element.getAsJsonPrimitive();
			if (prim.isNumber()) {
				return prim.getAsNumber();
			} else if (prim.isBoolean()) {
				return prim.getAsBoolean();
			} else {
				return prim.getAsString();
			}
		}
		return element.toString();
	}
}
