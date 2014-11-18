package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.nis.poi.RemoteStatus;

/**
 * Possible remote account states.
 */
public enum AccountRemoteStatus {
	/**
	 * The account is remote one, and therefore RemoteStatus is not applicable for it.
	 */
	REMOTE("REMOTE"),

	/**
	 * The account has activated remote harvesting (but not yet active).
	 */
	ACTIVATING("ACTIVATING"),

	/**
	 * The account has activated remote harvesting and remote harvesting is active.
	 */
	ACTIVE("ACTIVE"),

	/**
	 * The account has deactivated remote harvesting (but remote harvesting is still active).
	 */
	DEACTIVATING("DEACTIVATING"),

	/**
	 * The account has inactive remote harvesting,
	 * or it has deactivated remote harvesting and deactivation is operational.
	 */
	INACTIVE("INACTIVE");

	private final String status;

	private AccountRemoteStatus(final String status) {
		this.status = status;
	}

	/**
	 * Creates a new AccountRemoteStatus given a string representation.
	 *
	 * @param status The string representation.
	 * @return The account remote status.
	 */
	public static AccountRemoteStatus fromString(final String status) {
		for (final AccountRemoteStatus accountStatus : values()) {
			if (accountStatus.status.equals(status)) {
				return accountStatus;
			}
		}

		throw new IllegalArgumentException(String.format("Invalid account status: %s", status));
	}

	/**
	 * Creates a new AccountRemoteStatus given a RemoteStatus.
	 *
	 * @param status The remote status.
	 * @return The account remote status.
	 */
	public static AccountRemoteStatus fromRemoteStatus(final RemoteStatus status) {
		switch (status) {
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

	//region inline serialization

	/**
	 * Writes an account status.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param status The account status.
	 */
	public static void writeTo(final Serializer serializer, final String label, final AccountRemoteStatus status) {
		serializer.writeString(label, status.toString());
	}

	/**
	 * Reads an account status.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read account status.
	 */
	public static AccountRemoteStatus readFrom(final Deserializer deserializer, final String label) {
		return AccountRemoteStatus.fromString(deserializer.readString(label));
	}

	//endregion
}
