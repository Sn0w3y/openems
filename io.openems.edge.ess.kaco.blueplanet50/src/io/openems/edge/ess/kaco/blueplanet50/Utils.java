package io.openems.edge.ess.kaco.blueplanet50;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.symmetric.api.SymmetricEss;
import io.openems.edge.ess.symmetric.readonly.api.SymmetricEssReadonly;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssKacoBlueplanet50 c) {
		// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
		// we are missing an Enum value.
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(Ess.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
						return new IntegerReadChannel(c, channelId);
					case MAX_ACTIVE_POWER:
						return new IntegerReadChannel(c, channelId, EssKacoBlueplanet50.MAX_APPARENT_POWER);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, Ess.GridMode.UNDEFINED.ordinal());
					}
					return null;
				}), Arrays.stream(SymmetricEssReadonly.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER:
					case CHARGE_ACTIVE_POWER:
					case DISCHARGE_ACTIVE_POWER:
					case REACTIVE_POWER:
					case CHARGE_REACTIVE_POWER:
					case DISCHARGE_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
					// }), Arrays.stream(EssKacoBlueplanet50.ChannelId.values()).map(channelId -> {
					// switch (channelId) {
					// }
					// return null;
				}) //
		).flatMap(channel -> channel);
	}
}
