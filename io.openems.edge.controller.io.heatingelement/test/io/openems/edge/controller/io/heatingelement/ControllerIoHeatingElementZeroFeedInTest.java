package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT0;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT1;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT2;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatingElementZeroFeedInTest {

	private static MyConfig.Builder config() {
		return MyConfig.create() //
				.setId("ctrl0") //
				.setOutputChannelPhaseL1("io0/InputOutput0") //
				.setOutputChannelPhaseL2("io0/InputOutput1") //
				.setOutputChannelPhaseL3("io0/InputOutput2") //
				.setEndTime("17:00") //
				.setPowerOfPhase(2000) //
				.setMode(Mode.AUTOMATIC) //
				.setDefaultLevel(Level.LEVEL_1) //
				.setWorkMode(WorkMode.NONE) //
				.setMinTime(1) //
				.setMinimumSwitchingTime(0) //
				.setMinEnergylimit(5000) //
				.setEndTimeWithMeter("17:00") //
				.setMeterid("") //
				.setScheduler("") //
				.setZeroFeedInMode(true) //
				.setZeroFeedInSocLevel1(92) //
				.setZeroFeedInSocLevel2(94) //
				.setZeroFeedInSocLevel3(96) //
				.setZeroFeedInProbeDuration(10) //
				.setZeroFeedInMaxBatteryDischarge(300) //
				.setZeroFeedInMaxGridImport(300);
	}

	@Test
	public void zeroFeedInSequentialBootstrapTest() throws Exception {
		final var clock = createDummyClock();

		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(config().build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 91) //
						.input(PRODUCTION_ACTIVE_POWER, 4000) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 92) //
						.input(PRODUCTION_ACTIVE_POWER, 4000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 5, SECONDS) //
						.input(GRID_ACTIVE_POWER, 1500) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 92) //
						.input(PRODUCTION_ACTIVE_POWER, 4000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 6, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 92) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 94) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 94) //
						.input(PRODUCTION_ACTIVE_POWER, 8000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 8000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true)) //
				.next(new TestCase() //
						.timeleap(clock, 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 10000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, true) //
						.output("io0", INPUT_OUTPUT2, true)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 6000) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 0) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void failedProbeRollsBackImmediatelyAndUsesCooldownTest() throws Exception {
		final var clock = createDummyClock();

		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(config() //
						.setMinimumSwitchingTime(60) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, 1500) //
						.input(ESS_DISCHARGE_POWER, 500) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.next(new TestCase() //
						.timeleap(clock, 60, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 96) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, true) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}

	@Test
	public void disabledZeroFeedInModeDoesNotBootstrapTest() throws Exception {
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(config() //
						.setZeroFeedInMode(false) //
						.build()) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(ESS_SOC, 100) //
						.input(PRODUCTION_ACTIVE_POWER, 6000) //
						.output("io0", INPUT_OUTPUT0, false) //
						.output("io0", INPUT_OUTPUT1, false) //
						.output("io0", INPUT_OUTPUT2, false)) //
				.deactivate();
	}
}
