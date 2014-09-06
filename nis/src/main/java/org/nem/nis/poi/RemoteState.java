package org.nem.nis.poi;

import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;

/*
 * Used for tracking association between account and "remote account" (and the other way around too).
 */
public class RemoteState {
	private final boolean isRemote;
	private final Address remoteAddress;
	private final BlockHeight remoteHeight;
	private int direction;

	// if isRemote is true, remoteAddress should point to "owner"
	// otherwise remoteAddress should point to "remote harvesting" address,
	// remoteAddress might be null = no associated remote
	public RemoteState(final Address address, final BlockHeight height, final int direction, final boolean isRemote) {
		this.remoteAddress = address;
		this.remoteHeight = height;
		this.direction = direction;
		this.isRemote = isRemote;
	}

	public boolean hasRemote() {
		return this.remoteAddress != null;
	}

	public boolean isOwner() {
		return !isRemote;
	}

	public Address getRemoteAddress() {
		return remoteAddress;
	}

	public BlockHeight getRemoteHeight() {
		return remoteHeight;
	}

	public int getDirection() {
		return direction;
	}
}
