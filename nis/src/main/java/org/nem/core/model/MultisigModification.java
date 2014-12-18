package org.nem.core.model;

import org.nem.core.serialization.*;


// TODO 20141218 J-G what is the point of this entity?

public class MultisigModification implements SerializableEntity {
	private final MultisigModificationType modificationType;
	private final Account cosignatoryAccount;

	public MultisigModification(final MultisigModificationType modificationType, final Account cosignatoryAccount) {
		this.modificationType = modificationType;
		this.cosignatoryAccount = cosignatoryAccount;

		this.validate();
	}

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

	public void validate() {
		if (null == this.cosignatoryAccount) {
			throw new IllegalArgumentException("cosignatoryAccount is required");
		}

		if (!this.modificationType.isValid()) {
			throw new IllegalArgumentException("invalid mode");
		}
	}
}
