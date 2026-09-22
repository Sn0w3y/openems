package io.openems.edge.controller.io.heatingelement;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;

@ObjectClassDefinition(//
		name = "Controller IO Heating Element", //
		description = "Controls a three-phase heating element via Relays, according to grid active power")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoHeatingElement0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Output Channel Phase L1", description = "Channel address of the Digital Output for Phase L1")
	String outputChannelPhaseL1() default "io0/Relay1";

	@AttributeDefinition(name = "Output Channel Phase L2", description = "Channel address of the Digital Output for Phase L2")
	String outputChannelPhaseL2() default "io0/Relay2";

	@AttributeDefinition(name = "Output Channel Phase L3", description = "Channel address of the Digital Output for Phase L3")
	String outputChannelPhaseL3() default "io0/Relay3";

	@AttributeDefinition(name = "Default Level", description = "This is the default Level in manual mode and for force-heating in automatic mode")
	Level defaultLevel() default Level.LEVEL_1;

	@AttributeDefinition(name = "End Time", description = "End time for minimum run time")
	String endTime() default "17:00";

	@AttributeDefinition(name = "Work-Mode Time or None", description = "Sets the Work-Mode to Time (= run at least Minimum Time), to Energy (= run with a energy limit) or None (only run on excess power)")
	WorkMode workMode() default WorkMode.NONE;

	@AttributeDefinition(name = "Minimum Time [h]", description = "For Work-Mode 'Time': Minimum Time in hours for activating 'Levels'")
	int minTime() default 1;

	@AttributeDefinition(name = "Power per Phase", description = "Power of one single phase of the heating element in [W]")
	int powerPerPhase() default 2000;

	@AttributeDefinition(name = "Minimum switching time between two states", description = "Minimum time (Seconds) is applied to avoid continuous switching on threshold")
	int minimumSwitchingTime() default 60;

	@AttributeDefinition(name = "Zero Feed-In Mode", description = "Enables SOC-based staged probing so a curtailed PV inverter can release additional power for the heating element even when grid export is held at 0 W")
	boolean zeroFeedInMode() default false;

	@AttributeDefinition(name = "Zero Feed-In SOC Bootstrap Level 1 [%]", description = "Minimum ESS SOC that allows probing heating level 1")
	int zeroFeedInSocLevel1() default 92;

	@AttributeDefinition(name = "Zero Feed-In SOC Bootstrap Level 2 [%]", description = "Minimum ESS SOC that allows probing heating level 2")
	int zeroFeedInSocLevel2() default 94;

	@AttributeDefinition(name = "Zero Feed-In SOC Bootstrap Level 3 [%]", description = "Minimum ESS SOC that allows probing heating level 3")
	int zeroFeedInSocLevel3() default 96;

	@AttributeDefinition(name = "Zero Feed-In Probe Duration [s]", description = "Time to wait after enabling the next heating level before validating that PV regulation has taken over the added load")
	int zeroFeedInProbeDuration() default 10;

	@AttributeDefinition(name = "Zero Feed-In Maximum Battery Discharge [W]", description = "Maximum allowed ESS battery discharge while validating a zero-feed-in probe")
	int zeroFeedInMaxBatteryDischarge() default 300;

	@AttributeDefinition(name = "Zero Feed-In Maximum Grid Import [W]", description = "Maximum allowed grid import while validating a zero-feed-in probe")
	int zeroFeedInMaxGridImport() default 300;

	@AttributeDefinition(name = "Minimum energylimit", description = "The minimum Energylimit in [Wh]")
	int minEnergylimit() default 10000;

	@AttributeDefinition(name = "End Time Minimum with meter", description = "End time for the minimum run time with a meter")
	String endTimeWithMeter() default "17:00";

	@AttributeDefinition(name = "Scheduler", description = "Schedule for repeating tasks in JSCalender format.")
	String schedule() default "";

	@AttributeDefinition(name = "Consumption-Meter-ID", description = "ID of the Consumption-Meter.")
	String meter_id() default "";

	String webconsole_configurationFactory_nameHint() default "Controller IO Heating Element [{id}]";
}