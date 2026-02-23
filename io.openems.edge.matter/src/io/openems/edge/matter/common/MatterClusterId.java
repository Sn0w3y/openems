package io.openems.edge.matter.common;

/**
 * Constants for Matter cluster IDs used in OpenEMS device implementations.
 *
 * @see <a href=
 *      "https://csa-iot.org/developer-resource/specifications-download-request/">Matter
 *      Specification</a>
 */
public final class MatterClusterId {

	/** Basic Information cluster (0x0028). */
	public static final int BASIC_INFORMATION = 0x0028;

	/** Power Source cluster (0x002F). */
	public static final int POWER_SOURCE = 0x002F;

	/** Electrical Measurement cluster (0x0B04). */
	public static final int ELECTRICAL_MEASUREMENT = 0x0B04;

	/** Electrical Energy Measurement cluster (0x0091). */
	public static final int ELECTRICAL_ENERGY_MEASUREMENT = 0x0091;

	/** Electrical Power Measurement cluster (0x0090). */
	public static final int ELECTRICAL_POWER_MEASUREMENT = 0x0090;

	/** Energy EVSE cluster (0x0099). */
	public static final int ENERGY_EVSE = 0x0099;

	/** Device Energy Management cluster (0x0098). */
	public static final int DEVICE_ENERGY_MANAGEMENT = 0x0098;

	private MatterClusterId() {
		// Constants class
	}
}
