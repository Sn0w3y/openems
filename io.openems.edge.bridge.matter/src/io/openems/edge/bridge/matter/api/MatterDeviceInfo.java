package io.openems.edge.bridge.matter.api;

import java.util.List;

/**
 * Information about a commissioned Matter device.
 *
 * @param nodeId      the Matter node ID
 * @param vendorName  the vendor name
 * @param productName the product name
 * @param serialNumber the serial number (may be null)
 * @param endpoints   the list of endpoint IDs on this device
 */
public record MatterDeviceInfo(//
		long nodeId, //
		String vendorName, //
		String productName, //
		String serialNumber, //
		List<Integer> endpoints //
) {
}
