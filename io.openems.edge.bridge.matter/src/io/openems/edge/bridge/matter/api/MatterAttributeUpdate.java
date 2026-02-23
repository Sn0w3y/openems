package io.openems.edge.bridge.matter.api;

/**
 * An attribute update received from a Matter device.
 *
 * @param nodeId      the Matter node ID
 * @param endpointId  the endpoint ID
 * @param clusterId   the cluster ID
 * @param attributeId the attribute ID
 * @param value       the attribute value (may be null)
 */
public record MatterAttributeUpdate(//
		long nodeId, //
		int endpointId, //
		int clusterId, //
		int attributeId, //
		Object value //
) {
}
