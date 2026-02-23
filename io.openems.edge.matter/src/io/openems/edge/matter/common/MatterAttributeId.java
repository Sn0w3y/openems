package io.openems.edge.matter.common;

/**
 * Constants for Matter attribute IDs within clusters.
 *
 * @see <a href=
 *      "https://csa-iot.org/developer-resource/specifications-download-request/">Matter
 *      Specification</a>
 */
public final class MatterAttributeId {

	private MatterAttributeId() {
		// Constants class
	}

	/**
	 * Attribute IDs for the Electrical Measurement cluster (0x0B04).
	 */
	public static final class ElectricalMeasurement {

		/** Total active power in Watts. */
		public static final int ACTIVE_MEASURED_POWER = 0x050B;

		/** RMS Voltage in Volts (x10). */
		public static final int RMS_VOLTAGE = 0x0505;

		/** RMS Current in Amps (x10). */
		public static final int RMS_CURRENT = 0x0508;

		/** AC Frequency in Hz (x10). */
		public static final int AC_FREQUENCY = 0x0300;

		/** Phase A - Active Power. */
		public static final int ACTIVE_POWER_PHASE_A = 0x050B;

		/** Phase B - Active Power. */
		public static final int ACTIVE_POWER_PHASE_B = 0x090B;

		/** Phase C - Active Power. */
		public static final int ACTIVE_POWER_PHASE_C = 0x0A0B;

		/** Phase A - RMS Voltage. */
		public static final int RMS_VOLTAGE_PHASE_A = 0x0505;

		/** Phase B - RMS Voltage. */
		public static final int RMS_VOLTAGE_PHASE_B = 0x0905;

		/** Phase C - RMS Voltage. */
		public static final int RMS_VOLTAGE_PHASE_C = 0x0A05;

		/** Phase A - RMS Current. */
		public static final int RMS_CURRENT_PHASE_A = 0x0508;

		/** Phase B - RMS Current. */
		public static final int RMS_CURRENT_PHASE_B = 0x0908;

		/** Phase C - RMS Current. */
		public static final int RMS_CURRENT_PHASE_C = 0x0A08;

		private ElectricalMeasurement() {
		}
	}

	/**
	 * Attribute IDs for the Electrical Power Measurement cluster (0x0090).
	 */
	public static final class ElectricalPowerMeasurement {

		/** Active power in milliwatts. */
		public static final int ACTIVE_POWER = 0x0008;

		/** Voltage in millivolts. */
		public static final int VOLTAGE = 0x0004;

		/** Active current in milliamps. */
		public static final int ACTIVE_CURRENT = 0x0005;

		/** Frequency in millihertz. */
		public static final int FREQUENCY = 0x0007;

		private ElectricalPowerMeasurement() {
		}
	}

	/**
	 * Attribute IDs for the Electrical Energy Measurement cluster (0x0091).
	 */
	public static final class ElectricalEnergyMeasurement {

		/** Cumulative energy imported in milliwatt-hours. */
		public static final int CUMULATIVE_ENERGY_IMPORTED = 0x0001;

		/** Cumulative energy exported in milliwatt-hours. */
		public static final int CUMULATIVE_ENERGY_EXPORTED = 0x0002;

		private ElectricalEnergyMeasurement() {
		}
	}
}
