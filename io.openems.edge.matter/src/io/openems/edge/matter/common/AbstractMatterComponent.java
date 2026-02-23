package io.openems.edge.matter.common;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.matter.api.BridgeMatter;
import io.openems.edge.bridge.matter.api.MatterAttributeUpdate;
import io.openems.edge.bridge.matter.api.MatterComponent;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Abstract base class for Matter device components.
 *
 * <p>
 * Provides common functionality for subscribing to Matter attribute updates
 * through a {@link BridgeMatter} bridge.
 */
public abstract class AbstractMatterComponent extends AbstractOpenemsComponent implements MatterComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractMatterComponent.class);

	private boolean subscribed = false;

	protected AbstractMatterComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Gets the Matter bridge reference.
	 *
	 * @return the {@link BridgeMatter}
	 */
	protected abstract BridgeMatter getMatterBridge();

	/**
	 * Gets the Matter node ID.
	 *
	 * @return the node ID
	 */
	protected abstract long getMatterNodeId();

	/**
	 * Gets the Matter endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	protected abstract int getMatterEndpointId();

	/**
	 * Subscribes to attributes on the configured Matter device.
	 *
	 * @param clusterId    the cluster ID
	 * @param attributeIds the attribute IDs to subscribe to
	 * @param callback     the callback for attribute updates
	 */
	protected void subscribeToAttributes(int clusterId, List<Integer> attributeIds,
			Consumer<MatterAttributeUpdate> callback) {
		var bridge = this.getMatterBridge();
		if (bridge == null || !bridge.isConnected()) {
			this.log.debug("Matter bridge not connected, will retry subscription");
			this.subscribed = false;
			this.channel(MatterComponent.ChannelId.MATTER_COMMUNICATION_FAILED).setNextValue(true);
			return;
		}

		bridge.subscribeAttributes(this.getMatterNodeId(), this.getMatterEndpointId(), clusterId, attributeIds,
				callback)//
				.thenRun(() -> {
					this.subscribed = true;
					this.channel(MatterComponent.ChannelId.MATTER_COMMUNICATION_FAILED).setNextValue(false);
					this.log.info("Subscribed to Matter attributes on node {} endpoint {} cluster {}", //
							this.getMatterNodeId(), this.getMatterEndpointId(), clusterId);
				})//
				.exceptionally(e -> {
					this.subscribed = false;
					this.channel(MatterComponent.ChannelId.MATTER_COMMUNICATION_FAILED).setNextValue(true);
					this.log.error("Failed to subscribe to Matter attributes: {}", e.getMessage());
					return null;
				});
	}

	/**
	 * Checks if this component has an active subscription.
	 *
	 * @return true if subscribed
	 */
	protected boolean isSubscribed() {
		return this.subscribed;
	}
}
