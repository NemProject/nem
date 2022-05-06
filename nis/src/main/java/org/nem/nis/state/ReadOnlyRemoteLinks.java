package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;

public interface ReadOnlyRemoteLinks {

	/**
	 * Gets a value indicating whether or not any links are associated with the owning account.
	 *
	 * @return true if links are associated with the owning account.
	 */
	boolean isEmpty();

	/**
	 * Gets a value indicating whether or the owning account has enabled remote harvesting and is forwarding its harvesting power to a
	 * remote account.
	 *
	 * @return true if the owning account has enabled remote harvesting.
	 */
	boolean isHarvestingRemotely();

	/**
	 * Gets a value indicating whether or not the owning account is a remote harvester account.
	 *
	 * @return true if the owning account is a remote harvester.
	 */
	boolean isRemoteHarvester();

	/**
	 * Gets the current remote link.
	 *
	 * @return The current remote link.
	 */
	RemoteLink getCurrent();

	/**
	 * Gets the remote status at the specified block height.
	 *
	 * @param height The block height.
	 * @return The remote status.
	 */
	RemoteStatus getRemoteStatus(final BlockHeight height);
}
