package org.nem.core.model.ncc;

import org.nem.core.model.AccountStatus;
import org.nem.core.serialization.*;

/**
 * Class for holding additional information about an account required by ncc.
 */
public class AccountMetaData implements SerializableEntity {

	private final AccountStatus status;

	/**
	 * Creates a new meta data.
	 *
	 * @param status The account status.
	 */
	public AccountMetaData(final AccountStatus status) {
		this.status = status;
	}

	/**
	 * Deserializes a meta data.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountMetaData(final Deserializer deserializer) {
		this(AccountStatus.readFrom(deserializer, "status"));
	}

	/**
	 * Returns The status of the account (locked/unlocked).
	 *
	 * @return The status.
	 */
	public AccountStatus getStatus() {
		return this.status;
	}

	@Override
	public void serialize(Serializer serializer) {
		AccountStatus.writeTo(serializer, "status", this.status);
	}
}
