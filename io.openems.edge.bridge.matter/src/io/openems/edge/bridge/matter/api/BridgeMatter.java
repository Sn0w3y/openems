package io.openems.edge.bridge.matter.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Matter Bridge for OpenEMS.
 *
 * <p>
 * Provides Matter protocol communication by managing a Node.js subprocess
 * running matter.js. Communication between Java and Node.js uses
 * WebSocket/JSON-RPC.
 *
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
 * private BridgeMatter matterBridge;
 *
 * // Subscribe to attribute updates
 * this.matterBridge.subscribeAttributes(nodeId, endpointId, clusterId,
 *     List.of(attrId1, attrId2), update -> {
 *         System.out.println("Value: " + update.value());
 *     });
 * }
 * </pre>
 */
public interface BridgeMatter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Connected to Node.js matter-server.
		 *
		 * <ul>
		 * <li>Interface: BridgeMatter
		 * <li>Type: State
		 * <li>Level: OK
		 * </ul>
		 */
		CONNECTED(Doc.of(Level.OK)//
				.text("Connected to Matter server")),

		/**
		 * Connection to Node.js matter-server failed.
		 *
		 * <ul>
		 * <li>Interface: BridgeMatter
		 * <li>Type: State
		 * <li>Level: FAULT
		 * </ul>
		 */
		CONNECTION_FAILED(Doc.of(Level.FAULT)//
				.text("Matter server connection failed")),

		/**
		 * Node.js runtime was not found on the system.
		 *
		 * <ul>
		 * <li>Interface: BridgeMatter
		 * <li>Type: State
		 * <li>Level: FAULT
		 * </ul>
		 */
		NODEJS_NOT_FOUND(Doc.of(Level.FAULT)//
				.text("Node.js runtime not found")),

		/**
		 * The Node.js matter-server process crashed.
		 *
		 * <ul>
		 * <li>Interface: BridgeMatter
		 * <li>Type: State
		 * <li>Level: FAULT
		 * </ul>
		 */
		PROCESS_CRASHED(Doc.of(Level.FAULT)//
				.text("Matter server process crashed"));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Checks if the bridge is connected to the Node.js matter-server.
	 *
	 * @return true if connected
	 */
	boolean isConnected();

	/**
	 * Gets the list of discovered/commissioned Matter devices.
	 *
	 * @return a {@link CompletableFuture} with the list of
	 *         {@link MatterDeviceInfo}
	 */
	CompletableFuture<List<MatterDeviceInfo>> getDiscoveredDevices();

	/**
	 * Commissions a new Matter device using a pairing code.
	 *
	 * @param pairingCode the Matter pairing code (e.g. MT:Y.K90-...)
	 * @return a {@link CompletableFuture} with the device info after commissioning
	 */
	CompletableFuture<MatterDeviceInfo> commissionDevice(String pairingCode);

	/**
	 * Subscribes to attribute updates from a specific device endpoint/cluster.
	 *
	 * @param nodeId       the Matter node ID
	 * @param endpointId   the endpoint ID
	 * @param clusterId    the cluster ID
	 * @param attributeIds the list of attribute IDs to subscribe to
	 * @param callback     the callback invoked on attribute updates
	 * @return a {@link CompletableFuture} that completes when the subscription is
	 *         established
	 */
	CompletableFuture<Void> subscribeAttributes(long nodeId, int endpointId, int clusterId,
			List<Integer> attributeIds, Consumer<MatterAttributeUpdate> callback);

	/**
	 * Reads a single attribute from a Matter device.
	 *
	 * @param nodeId      the Matter node ID
	 * @param endpointId  the endpoint ID
	 * @param clusterId   the cluster ID
	 * @param attributeId the attribute ID
	 * @return a {@link CompletableFuture} with the attribute value
	 */
	CompletableFuture<Object> readAttribute(long nodeId, int endpointId, int clusterId, int attributeId);

	/**
	 * Decommissions (removes) a Matter device.
	 *
	 * @param nodeId the Matter node ID
	 * @return a {@link CompletableFuture} that completes when done
	 */
	CompletableFuture<Void> decommissionDevice(long nodeId);
}
