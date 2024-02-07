package io.openems.edge.pvinverter.sungrow.string;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "Pure Grid Inverter Sungrow", //
		description = "Implements the Sungrow Grid Inverter")
@interface OutputType {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pvinverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Invert Active Power", description = "If enabled, the active power value is inverted.")
	boolean invertActivePower() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Meter-Type", description = "Meter-Type of this Meter (Only provides the meter types PRODUCTION).")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "String Inverter Sungrow [{id}]";

}