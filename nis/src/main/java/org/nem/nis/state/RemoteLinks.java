package org.nem.nis.state;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.utils.CircularStack;

/**
 * A collection of remote states associated with an account.
 */
public class RemoteLinks implements ReadOnlyRemoteLinks {
	private static final int REMOTE_LINKS_SIZE = 2;

	// The following rules will apply:
	// 1. one will have to wait REMOTE_HARVESTING_DELAY blocks to activate/deactivate remote account (before it'll become operational)
	// 2. one cannot make two SAME subsequent announcements: so let's say I've announced address X as my remote address.
	// now if I want to announce address Y. I first need to cancel/deactivate address X first.
	//
	// This makes whole logic a lot simpler
	//
	// I thought we're gonna need 3 elements, but it would work with 2.
	// 1. let's say there is alias created at block 1000, it becomes effective at block 2440
	// 2. now at block 2500 user removes alias, removal will become effective at block 3940
	// 3. user won't be able to create new alias before 3940, so there is no need, for this to have 3 elements
	// as eventual (blockchain) rollback won't change anything, so I'm changing REMOTE_STATE_SIZE to 2
	private final CircularStack<RemoteLink> remoteLinks = new CircularStack<>(REMOTE_LINKS_SIZE);

	/**
	 * Adds a remote link.
	 *
	 * @param remoteLink The remote link to add.
	 */
	public void addLink(final RemoteLink remoteLink) {
		this.remoteLinks.push(remoteLink);
	}

	/**
	 * Removes a remote link.
	 *
	 * @param remoteLink The remote link to remove.
	 */
	public void removeLink(final RemoteLink remoteLink) {
		// between changes of remoteState there must be 1440 blocks
		if (this.isEmpty() || !this.remoteLinks.peek().equals(remoteLink)) {
			throw new IllegalArgumentException("call to removeLink must be 'paired' with call to addLink");
		}

		this.remoteLinks.pop();
	}

	@Override
	public boolean isEmpty() {
		return 0 == this.remoteLinks.size();
	}

	@Override
	public boolean isHarvestingRemotely() {
		return !this.isEmpty() && RemoteLink.Owner.HarvestingRemotely == this.remoteLinks.peek().getOwner();
	}

	@Override
	public boolean isRemoteHarvester() {
		return !this.isEmpty() && RemoteLink.Owner.RemoteHarvester == this.remoteLinks.peek().getOwner();
	}

	@Override
	public RemoteLink getCurrent() {
		return this.isEmpty() ? null : this.remoteLinks.peek();
	}

	@Override
	public RemoteStatus getRemoteStatus(final BlockHeight height) {
		if (this.isEmpty()) {
			return RemoteStatus.NOT_SET;
		}

		// currently we can only have Activate and Deactivate, so we're ok to use single boolean for this

		final boolean isActivated = ImportanceTransferMode.Activate == this.getCurrent().getMode();
		final long heightDiff = height.subtract(this.getCurrent().getEffectiveHeight());
		final boolean withinLimit = heightDiff < NemGlobals.getBlockChainConfiguration().getBlockChainRewriteLimit();

		if (this.isHarvestingRemotely()) {
			if (isActivated) {
				return withinLimit ? RemoteStatus.OWNER_ACTIVATING : RemoteStatus.OWNER_ACTIVE;
			} else {
				return withinLimit ? RemoteStatus.OWNER_DEACTIVATING : RemoteStatus.OWNER_INACTIVE;
			}
		} else {
			if (isActivated) {
				return withinLimit ? RemoteStatus.REMOTE_ACTIVATING : RemoteStatus.REMOTE_ACTIVE;
			} else {
				return withinLimit ? RemoteStatus.REMOTE_DEACTIVATING : RemoteStatus.REMOTE_INACTIVE;
			}
		}
	}

	/**
	 * Creates a copy of this collection.
	 *
	 * @return A copy of this collection.
	 */
	public RemoteLinks copy() {
		final RemoteLinks copy = new RemoteLinks();
		this.remoteLinks.shallowCopyTo(copy.remoteLinks);
		return copy;
	}
}
