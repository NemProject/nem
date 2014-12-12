package org.nem.nis.state;

// TODO 20141014 J-G: comments

import org.nem.core.model.AccountRemoteStatus;

public enum RemoteStatus {
	NOT_SET,

	OWNER_INACTIVE,
	OWNER_ACTIVATING,
	OWNER_ACTIVE,
	OWNER_DEACTIVATING,
	REMOTE_INACTIVE,
	REMOTE_ACTIVATING,
	REMOTE_ACTIVE,
	REMOTE_DEACTIVATING;

	/**
	 * Converts this RemoteStatus into an AccountRemoteStatus.
	 *
	 * @return The account remote status.
	 */
	public AccountRemoteStatus toAccountRemoteStatus() {
		switch (this) {
			case NOT_SET:
				return AccountRemoteStatus.INACTIVE;
			case OWNER_INACTIVE:
				return AccountRemoteStatus.INACTIVE;
			case OWNER_ACTIVATING:
				return AccountRemoteStatus.ACTIVATING;
			case OWNER_ACTIVE:
				return AccountRemoteStatus.ACTIVE;
			case OWNER_DEACTIVATING:
				return AccountRemoteStatus.DEACTIVATING;

			default:
				return AccountRemoteStatus.REMOTE;
		}
	}
}
