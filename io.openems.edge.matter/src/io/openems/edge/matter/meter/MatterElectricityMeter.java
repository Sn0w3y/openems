package io.openems.edge.matter.meter;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * A Matter-based Electricity Meter.
 *
 * <p>
 * Maps Matter Electrical Measurement / Electrical Power Measurement cluster
 * attributes to OpenEMS {@link ElectricityMeter} channels.
 */
public interface MatterElectricityMeter extends ElectricityMeter, OpenemsComponent {

}
