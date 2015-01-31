package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents a multisig modification.
 */
public class MultisigModification implements SerializableEntity, Comparable<MultisigModification> {
	private final MultisigModificationType modificationType;
	private final Account cosignatoryAccount;

	/**
	 * Creates a multisig modification.
	 *
	 * @param modificationType The modification type.
	 * @param cosignatoryAccount The cosignatory account.
	 */
	public MultisigModification(final MultisigModificationType modificationType, final Account cosignatoryAccount) {
		this.modificationType = modificationType;
		this.cosignatoryAccount = cosignatoryAccount;

		this.validate();
	}

	/**
	 * Deserializes a multisig modification.
	 *
	 * @param deserializer The deserializer.
	 */
	public MultisigModification(final Deserializer deserializer) {
		this.modificationType = MultisigModificationType.fromValueOrDefault(deserializer.readInt("modificationType"));
		this.cosignatoryAccount = Account.readFrom(deserializer, "cosignatoryAccount", AddressEncoding.PUBLIC_KEY);

		this.validate();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("modificationType", this.modificationType.value());
		Account.writeTo(serializer, "cosignatoryAccount", this.cosignatoryAccount, AddressEncoding.PUBLIC_KEY);
	}

	/**
	 * Gets cosignatory account.
	 *
	 * @return The cosignatory account.
	 */
	public Account getCosignatory() {
		return this.cosignatoryAccount;
	}

	/**
	 * Gets type of multisig signer modification transaction.
	 *
	 * @return The modification type.
	 */
	public MultisigModificationType getModificationType() {
		return this.modificationType;
	}

	private void validate() {
		if (null == this.cosignatoryAccount) {
			throw new IllegalArgumentException("cosignatoryAccount is required");
		}

		if (!this.modificationType.isValid()) {
			throw new IllegalArgumentException("invalid mode");
		}
	}

	@Override
	public int compareTo(final MultisigModification rhs) {
		final int typeCompareResult = Integer.compare(this.modificationType.value(), rhs.modificationType.value());
		return 0 != typeCompareResult
				? typeCompareResult
				: this.cosignatoryAccount.getAddress().compareTo(rhs.cosignatoryAccount.getAddress());
	}
}
