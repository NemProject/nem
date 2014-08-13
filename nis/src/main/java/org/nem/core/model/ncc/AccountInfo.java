package org.nem.core.model.ncc;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;

/**
 * Represents an external view of an account.
 */
public class AccountInfo implements SerializableEntity {
	private final Address address;
	private final KeyPair keyPair;
	private final Amount balance;
	private final BlockAmount numForagedBlocks;
	private final String label;
	private final double importance;

	/**
	 * Creates a new account view model.
	 *
	 * @param address The address.
	 * @param balance The balance.
	 * @param numForagedBlocks The number of foraged blocks.
	 * @param label The label.
	 * @param importance The importance.
	 */
	public AccountInfo(
			final Address address,
			final Amount balance,
			final BlockAmount numForagedBlocks,
			final String label,
			final double importance) {
		this.address = address;
		this.keyPair = null == this.address.getPublicKey() ? null : new KeyPair(this.address.getPublicKey());
		this.balance = balance;
		this.numForagedBlocks = numForagedBlocks;
		this.label = label;
		this.importance = importance;
	}

	/**
	 * Deserializes an account view model.
	 *
	 * @param deserializer The deserializer.
	 */
	public AccountInfo(final Deserializer deserializer) {
		this.address = deserializeAddress(deserializer);
		this.keyPair = null == this.address.getPublicKey() ? null : new KeyPair(this.address.getPublicKey());
		this.balance = Amount.readFrom(deserializer, "balance");
		this.numForagedBlocks = BlockAmount.readFrom(deserializer, "foragedBlocks");
		this.label = deserializer.readOptionalString("label");
		this.importance = deserializer.readDouble("importance");
	}

	private static Address deserializeAddress(final Deserializer deserializer) {
		final Address addressWithoutPublicKey = Address.readFrom(deserializer, "address", AddressEncoding.COMPRESSED);
		final Address addressWithPublicKey = Address.readFrom(deserializer, "publicKey", AddressEncoding.PUBLIC_KEY);
		return null != addressWithPublicKey ? addressWithPublicKey : addressWithoutPublicKey;
	}

	/**
	 * Gets the account's key pair.
	 *
	 * @return This account's key pair.
	 */
	public KeyPair getKeyPair() {
		return this.keyPair;
	}

	/**
	 * Gets the account's address.
	 *
	 * @return This account's address.
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * Gets the account's balance.
	 *
	 * @return This account's balance.
	 */
	public Amount getBalance() {
		return this.balance;
	}

	/**
	 * Gets number of foraged blocks.
	 *
	 * @return Number of blocks foraged by this account.
	 */
	public BlockAmount getNumForagedBlocks() {
		return this.numForagedBlocks;
	}

	/**
	 * Gets the account's label.
	 *
	 * @return The account's label.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * Gets the importance associated with this account.
	 *
	 * @return The importance associated with this account.
	 */
	public double getImportance() {
		return this.importance;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "address", this.getAddress(), AddressEncoding.COMPRESSED);
		Address.writeTo(serializer, "publicKey", this.getAddress(), AddressEncoding.PUBLIC_KEY);

		Amount.writeTo(serializer, "balance", this.getBalance());
		BlockAmount.writeTo(serializer, "foragedBlocks", this.getNumForagedBlocks());
		serializer.writeString("label", this.getLabel());

		serializer.writeDouble("importance", this.getImportance());
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof AccountInfo)) {
			return false;
		}

		final AccountInfo rhs = (AccountInfo)obj;
		return this.address.equals(rhs.address);
	}
}