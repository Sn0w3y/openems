package io.openems.edge.controller.io.heatingelement;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String outputChannelPhaseL1;
		private String outputChannelPhaseL2;
		private String outputChannelPhaseL3;
		private int powerOfPhase;
		private Mode mode;
		private WorkMode workMode;
		private int minEnergylimit;
		private String endTimeWithMeter;
		private String meterId;
		private int minTime;
		private String endTime;
		private Level defaultLevel;
		private int minimumSwitchingTime;
		private boolean zeroFeedInMode = false;
		private int zeroFeedInSocLevel1 = 92;
		private int zeroFeedInSocLevel2 = 94;
		private int zeroFeedInSocLevel3 = 96;
		private int zeroFeedInProbeDuration = 10;
		private int zeroFeedInMaxBatteryDischarge = 300;
		private int zeroFeedInMaxGridImport = 300;
		private String scheduler;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setOutputChannelPhaseL1(String outputChannelPhaseL1) {
			this.outputChannelPhaseL1 = outputChannelPhaseL1;
			return this;
		}

		public Builder setOutputChannelPhaseL2(String outputChannelPhaseL2) {
			this.outputChannelPhaseL2 = outputChannelPhaseL2;
			return this;
		}

		public Builder setOutputChannelPhaseL3(String outputChannelPhaseL3) {
			this.outputChannelPhaseL3 = outputChannelPhaseL3;
			return this;
		}

		public Builder setPowerOfPhase(int powerOfPhase) {
			this.powerOfPhase = powerOfPhase;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setWorkMode(WorkMode workMode) {
			this.workMode = workMode;
			return this;
		}

		public Builder setMinEnergylimit(int minEnergylimit) {
			this.minEnergylimit = minEnergylimit;
			return this;
		}

		public Builder setMeterid(String meterid) {
			this.meterId = meterid;
			return this;
		}

		public Builder setMinTime(int minTime) {
			this.minTime = minTime;
			return this;
		}

		public Builder setEndTime(String endTime) {
			this.endTime = endTime;
			return this;
		}

		public Builder setEndTimeWithMeter(String endTimeWithMeter) {
			this.endTimeWithMeter = endTimeWithMeter;
			return this;
		}

		public Builder setDefaultLevel(Level defaultLevel) {
			this.defaultLevel = defaultLevel;
			return this;
		}

		public Builder setMinimumSwitchingTime(int minimumSwitchingTime) {
			this.minimumSwitchingTime = minimumSwitchingTime;
			return this;
		}

		public Builder setZeroFeedInMode(boolean zeroFeedInMode) {
			this.zeroFeedInMode = zeroFeedInMode;
			return this;
		}

		public Builder setZeroFeedInSocLevel1(int zeroFeedInSocLevel1) {
			this.zeroFeedInSocLevel1 = zeroFeedInSocLevel1;
			return this;
		}

		public Builder setZeroFeedInSocLevel2(int zeroFeedInSocLevel2) {
			this.zeroFeedInSocLevel2 = zeroFeedInSocLevel2;
			return this;
		}

		public Builder setZeroFeedInSocLevel3(int zeroFeedInSocLevel3) {
			this.zeroFeedInSocLevel3 = zeroFeedInSocLevel3;
			return this;
		}

		public Builder setZeroFeedInProbeDuration(int zeroFeedInProbeDuration) {
			this.zeroFeedInProbeDuration = zeroFeedInProbeDuration;
			return this;
		}

		public Builder setZeroFeedInMaxBatteryDischarge(int zeroFeedInMaxBatteryDischarge) {
			this.zeroFeedInMaxBatteryDischarge = zeroFeedInMaxBatteryDischarge;
			return this;
		}

		public Builder setZeroFeedInMaxGridImport(int zeroFeedInMaxGridImport) {
			this.zeroFeedInMaxGridImport = zeroFeedInMaxGridImport;
			return this;
		}

		public Builder setScheduler(String scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String outputChannelPhaseL1() {
		return this.builder.outputChannelPhaseL1;
	}

	@Override
	public String outputChannelPhaseL2() {
		return this.builder.outputChannelPhaseL2;
	}

	@Override
	public String outputChannelPhaseL3() {
		return this.builder.outputChannelPhaseL3;
	}

	@Override
	public Level defaultLevel() {
		return this.builder.defaultLevel;
	}

	@Override
	public String endTime() {
		return this.builder.endTime;
	}

	@Override
	public WorkMode workMode() {
		return this.builder.workMode;
	}

	@Override
	public int minEnergylimit() {
		return this.builder.minEnergylimit;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public int minTime() {
		return this.builder.minTime;
	}

	@Override
	public int powerPerPhase() {
		return this.builder.powerOfPhase;
	}

	@Override
	public int minimumSwitchingTime() {
		return this.builder.minimumSwitchingTime;
	}

	@Override
	public boolean zeroFeedInMode() {
		return this.builder.zeroFeedInMode;
	}

	@Override
	public int zeroFeedInSocLevel1() {
		return this.builder.zeroFeedInSocLevel1;
	}

	@Override
	public int zeroFeedInSocLevel2() {
		return this.builder.zeroFeedInSocLevel2;
	}

	@Override
	public int zeroFeedInSocLevel3() {
		return this.builder.zeroFeedInSocLevel3;
	}

	@Override
	public int zeroFeedInProbeDuration() {
		return this.builder.zeroFeedInProbeDuration;
	}

	@Override
	public int zeroFeedInMaxBatteryDischarge() {
		return this.builder.zeroFeedInMaxBatteryDischarge;
	}

	@Override
	public int zeroFeedInMaxGridImport() {
		return this.builder.zeroFeedInMaxGridImport;
	}

	@Override
	public String endTimeWithMeter() {
		return this.builder.endTimeWithMeter;
	}

	@Override
	public String schedule() {
		return this.builder.scheduler;
	}
}