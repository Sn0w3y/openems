package io.openems.edge.matter.meter;

import static io.openems.edge.matter.common.MatterAttributeId.ElectricalPowerMeasurement.ACTIVE_CURRENT;
import static io.openems.edge.matter.common.MatterAttributeId.ElectricalPowerMeasurement.ACTIVE_POWER;
import static io.openems.edge.matter.common.MatterAttributeId.ElectricalPowerMeasurement.FREQUENCY;
import static io.openems.edge.matter.common.MatterAttributeId.ElectricalPowerMeasurement.VOLTAGE;
import static io.openems.edge.matter.common.MatterClusterId.ELECTRICAL_POWER_MEASUREMENT;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.matter.api.BridgeMatter;
import io.openems.edge.bridge.matter.api.MatterAttributeUpdate;
import io.openems.edge.bridge.matter.api.MatterComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.matter.common.AbstractMatterComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Matter.Meter.ElectricityMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class MatterElectricityMeterImpl extends AbstractMatterComponent
		implements MatterElectricityMeter, ElectricityMeter, MatterComponent, OpenemsComponent, TimedataProvider,
		EventHandler {

	private static final List<Integer> SUBSCRIBED_ATTRIBUTES = List.of(//
			ACTIVE_POWER, //
			VOLTAGE, //
			ACTIVE_CURRENT, //
			FREQUENCY //
	);

	private final Logger log = LoggerFactory.getLogger(MatterElectricityMeterImpl.class);

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private MeterType meterType = MeterType.PRODUCTION;
	private Config config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BridgeMatter matterBridge;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	public MatterElectricityMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				MatterComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculatePhasesFromActivePower(this);
		ElectricityMeter.calculatePhasesFromVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.meterType = config.type();

		if (!config.enabled()) {
			return;
		}

		// Update reference filter for Matter bridge
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Matter", config.matter_id())) {
			return;
		}

		// Subscribe to Matter attributes
		this.setupSubscription();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected BridgeMatter getMatterBridge() {
		return this.matterBridge;
	}

	@Override
	protected long getMatterNodeId() {
		return this.config.matterNodeId();
	}

	@Override
	protected int getMatterEndpointId() {
		return this.config.matterEndpointId();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			// Retry subscription if not yet established
			if (!this.isSubscribed() && this.matterBridge != null && this.matterBridge.isConnected()) {
				this.setupSubscription();
			}
			this.calculateEnergy();
		}
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	/**
	 * Sets up the subscription to Matter Electrical Power Measurement attributes.
	 */
	private void setupSubscription() {
		this.subscribeToAttributes(ELECTRICAL_POWER_MEASUREMENT, SUBSCRIBED_ATTRIBUTES,
				this::handleAttributeUpdate);
	}

	/**
	 * Handles an attribute update from the Matter device.
	 *
	 * @param update the attribute update
	 */
	private void handleAttributeUpdate(MatterAttributeUpdate update) {
		var value = update.value();
		if (value == null) {
			return;
		}

		var numValue = toNumber(value);
		if (numValue == null) {
			return;
		}

		switch (update.attributeId()) {
		case ACTIVE_POWER -> {
			// Electrical Power Measurement reports in milliwatts, convert to watts
			this._setActivePower(Math.round(numValue.floatValue() / 1000f));
		}
		case VOLTAGE -> {
			// Reported in millivolts
			this._setVoltage(Math.round(numValue.floatValue()));
		}
		case ACTIVE_CURRENT -> {
			// Reported in milliamps
			this._setCurrent(Math.round(numValue.floatValue()));
		}
		case FREQUENCY -> {
			// Reported in millihertz
			this._setFrequency(Math.round(numValue.floatValue()));
		}
		default -> {
			// Unknown attribute
		}
		}
	}

	/**
	 * Calculates energy values from active power.
	 */
	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	private static Number toNumber(Object value) {
		if (value instanceof Number n) {
			return n;
		}
		if (value instanceof String s) {
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
