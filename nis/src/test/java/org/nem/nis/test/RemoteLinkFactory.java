package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.RemoteLink;

/**
 * Static helper class containing factory functions for creating remote links.
 */
public class RemoteLinkFactory {

	/**
	 * Creates a remote link that activates a remote harvester.
	 *
	 * @param address The address.
	 * @param height The effective block height.
	 * @return The remote link.
	 */
	public static RemoteLink activateRemoteHarvester(final Address address, final BlockHeight height) {
		return new RemoteLink(address, height, ImportanceTransferMode.Activate, RemoteLink.Owner.RemoteHarvester);
	}

	/**
	 * Creates a remote link that activates a node harvesting remotely.
	 *
	 * @param address The address.
	 * @param height The effective block height.
	 * @return The remote link.
	 */
	public static RemoteLink activateHarvestingRemotely(final Address address, final BlockHeight height) {
		return new RemoteLink(address, height, ImportanceTransferMode.Activate, RemoteLink.Owner.HarvestingRemotely);
	}

	/**
	 * Creates a remote link that deactivates a node harvesting remotely.
	 *
	 * @param address The address.
	 * @param height The effective block height.
	 * @return The remote link.
	 */
	public static RemoteLink deactivateHarvestingRemotely(final Address address, final BlockHeight height) {
		return new RemoteLink(address, height, ImportanceTransferMode.Deactivate, RemoteLink.Owner.HarvestingRemotely);
	}
}
