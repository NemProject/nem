package org.nem.nis.state;

import org.nem.core.model.AccountRemoteStatus;

/**
 * An enumeration of possible remote statuses.
 */
public enum RemoteStatus {

	/**
	 * Queried account has not participated in remote harvesting; neither as an owner nor a remote.
	 */
	NOT_SET,

	/**
	 * Queried account is an owner, remote harvesting is not-active/deactivated.
	 */
	OWNER_INACTIVE,

	/**
	 * Queried account is an an owner, remote harvesting is activating.
	 */
	OWNER_ACTIVATING,

	/**
	 * Queried account is an owner, remote harvesting is active.
	 */
	OWNER_ACTIVE,

	/**
	 * Queried account is an owner, remote harvesting is being deactivated.
	 */
	OWNER_DEACTIVATING,

	/**
	 * Queried account is a remote, remote harvesting is inactive. <br>
	 * note: this indicates, that account was at some point used as a remote account.
	 */
	REMOTE_INACTIVE,

	/**
	 * Queried account is a remote, remote harvesting is activating.
	 */
	REMOTE_ACTIVATING,

	/**
	 * Queried account is a remote, remote harvesting is active.
	 */
	REMOTE_ACTIVE,

	/**
	 * Queried account is a remote, remote harvesting is being deactivated.
	 */
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

			default :
				return AccountRemoteStatus.REMOTE;
		}
	}
}
