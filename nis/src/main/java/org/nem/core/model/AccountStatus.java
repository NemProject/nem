package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Possible account statuses.
 */
public enum AccountStatus {
	/**
	 * The account status is unknown (needed for ncc).
	 */
	UNKNOWN("UNKNOWN"),

	/**
	 * The account is locked.
	 */
	LOCKED("LOCKED"),

	/**
	 * The account is unlocked.
	 */
	UNLOCKED("UNLOCKED");

	private final String status;

	AccountStatus(final String status) {
		this.status = status;
	}

	public static AccountStatus fromString(final String status) {
		if (status != null) {
			for (final AccountStatus accountStatus : values()) {
				if (accountStatus.status.equals(status)) {
					return accountStatus;
				}
			}
		}
		throw new IllegalArgumentException("Invalid account status: " + status);
	}

	//region inline serialization

	/**
	 * Writes an account status.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param status The account status.
	 */
	public static void writeTo(final Serializer serializer, final String label, final AccountStatus status) {
		serializer.writeString(label, status.toString());
	}

	/**
	 * Reads an account status.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read account status.
	 */
	public static AccountStatus readFrom(final Deserializer deserializer, final String label) {
		return AccountStatus.fromString(deserializer.readString(label));
	}

	//endregion
}