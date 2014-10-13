package org.nem.core.model.ncc;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about an account required by ncc.
 */
public class AccountMetaData implements SerializableEntity {
	private final AccountStatus status;
	private final AccountRemoteStatus remoteStatus;

	/**
	 * Creates a new meta data.
	 *
	 * @param status The account status.
	 * @param remoteStatus The remote status.
	 */
	public AccountMetaData(final AccountStatus status, final AccountRemoteStatus remoteStatus) {
		this.status = status;
		this.remoteStatus = remoteStatus;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountMetaData(final Deserializer deserializer) {
		this(AccountStatus.readFrom(deserializer, "status"), AccountRemoteStatus.readFrom(deserializer, "remoteStatus"));
	}

	/**
	 * Returns The status of the account (locked/unlocked).
	 *
	 * @return The status.
	 */
	public AccountStatus getStatus() {
		return this.status;
	}

	/**
	 * Gets the remote account status
	 *
	 * @return The remote account status.
	 */
	public AccountRemoteStatus getRemoteStatus() {
		return this.remoteStatus;
	}

	@Override
	public void serialize(final Serializer serializer) {
		AccountStatus.writeTo(serializer, "status", this.status);
		AccountRemoteStatus.writeTo(serializer, "remoteStatus", this.getRemoteStatus());
	}
}
