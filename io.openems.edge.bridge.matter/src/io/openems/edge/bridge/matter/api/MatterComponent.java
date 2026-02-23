package io.openems.edge.bridge.matter.api;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Marker interface for OpenEMS components that use Matter communication.
 *
 * <p>
 * Components implementing this interface communicate with Matter devices through
 * a {@link BridgeMatter}.
 */
public interface MatterComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Matter Communication Failed.
		 *
		 * <ul>
		 * <li>Interface: MatterComponent
		 * <li>Type: State
		 * <li>Level: WARNING
		 * <li>Description: Matter communication with this component failed
		 * </ul>
		 */
		MATTER_COMMUNICATION_FAILED(Doc.of(Level.WARNING)//
				.text("Matter communication failed"));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
