package org.nem.nis.poi;

import org.nem.core.utils.CircularStack;

/**
 * A collection of remote states associated with an account.
 */
public class RemoteLinks {
	private static final int REMOTE_LINKS_SIZE = 2;

	// The following rules will apply:
	//  1. one will have to wait 1440 blocks to  activate/deactivate remote account (before it'll become operational)
	//  2. in cannot make two SAME subsequent announcements: so let's say I've  announce address X as my remote address.
	//    now if I want to announce address Y. I first need to cancel/deactivate address X first.
	//
	// This makes whole logic a lot simpler
	//
	// I thought we're gonna need 3 elements, but it would work with 2.
	//    1. let's say there is alias created at block 1000, it becomes effective at block 2440
	//    2. now at block 2500 user removes alias, removal will become effective at block 3940
	//    3. user won't be able to create new alias before 3940, so there is no need, for this to have 3 elements
	//        as eventual (blockchain) rollback won't change anything, so I'm changing REMOTE_STATE_SIZE to 2
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

	/**
	 * Gets a value indicating whether or not any links are associated with the owning account.
	 *
	 * @return true if links are associated with the owning account.
	 */
	public boolean isEmpty() {
		return 0 == this.remoteLinks.size();
	}

	/**
	 * Gets a value indicating whether or the owning account has enabled remote harvesting
	 * and is forwarding its foraging power to a remote account.
	 *
	 * @return true if the owning account has enabled remote harvesting.
	 */
	public boolean isHarvestingRemotely() {
		return !this.isEmpty() && RemoteLink.Owner.HarvestingRemotely == this.remoteLinks.peek().getOwner();
	}

	/**
	 * Gets a value indicating whether or not the owning account is a remote harvester account.
	 *
	 * @return true if the owning account is a remote harvester.
	 */
	public boolean isRemoteHarvester() {
		return !this.isEmpty() && RemoteLink.Owner.RemoteHarvester == this.remoteLinks.peek().getOwner();
	}

	/**
	 * Gets the current remote link.
	 *
	 * @return The current remote link.
	 */
	public RemoteLink getCurrent() {
		return this.isEmpty() ? null : this.remoteLinks.peek();
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
