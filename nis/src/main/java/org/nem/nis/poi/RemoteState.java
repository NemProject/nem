package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

/*
 * Used for tracking association between account and "remote account" (and the other way around too).
 */
public class RemoteState {
	private boolean isRemote;
	private Address remoteAddress;
	private BlockHeight remoteHeight;

	// if isRemote is true, remoteAddress should point to "owner"
	// otherwise remoteAddress should point to "remote harvesting" address,
	// remoteAddress might be null = no associated remote
	public RemoteState(final Address address, final BlockHeight height, final boolean isRemote) {
		this.remoteAddress = address;
		this.remoteHeight = height;
		this.isRemote = isRemote;
	}
}
