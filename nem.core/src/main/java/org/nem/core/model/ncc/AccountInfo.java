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
	private final Amount vestedBalance;
	private final BlockAmount numHarvestedBlocks;
	private final String label;
	private final double importance;
	private final MultisigInfo multisigInfo;

	// Since in most cases, multisig info will be null, I think
	// it's justifiable to have two separate ctors

	/**
	 * Creates a new account view model.
	 *
	 * @param address The address.
	 * @param balance The balance.
	 * @param vestedBalance The vested balance.
	 * @param numHarvestedBlocks The number of harvested blocks.
	 * @param label The label.
	 * @param importance The importance.
	 */
	public AccountInfo(
			final Address address,
			final Amount balance,
			final Amount vestedBalance,
			final BlockAmount numHarvestedBlocks,
			final String label,
			final double importance) {
		this(address, balance, vestedBalance, numHarvestedBlocks, label, importance, null);
	}

	/**
	 * Creates a new account view model.
	 *
	 * @param address The address.
	 * @param balance The balance.
	 * @param vestedBalance The vested balance.
	 * @param numHarvestedBlocks The number of harvested blocks.
	 * @param label The label.
	 * @param importance The importance.
	 */
	public AccountInfo(
			final Address address,
			final Amount balance,
			final Amount vestedBalance,
			final BlockAmount numHarvestedBlocks,
			final String label,
			final double importance,
			final MultisigInfo multisigInfo) {
		this.address = address;
		this.keyPair = null == this.address.getPublicKey() ? null : new KeyPair(this.address.getPublicKey());
		this.balance = balance;
		this.vestedBalance = vestedBalance;
		this.numHarvestedBlocks = numHarvestedBlocks;
		this.label = label;
		this.importance = importance;
		this.multisigInfo = multisigInfo;
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
		this.vestedBalance = Amount.readFrom(deserializer, "vestedBalance");
		this.numHarvestedBlocks = BlockAmount.readFrom(deserializer, "harvestedBlocks");
		this.label = deserializer.readOptionalString("label");
		this.importance = deserializer.readDouble("importance");

		this.multisigInfo = deserializer.readOptionalObject("multisigInfo", MultisigInfo::new);
	}

	private static Address deserializeAddress(final Deserializer deserializer) {
		final Address addressWithoutPublicKey = Address.readFrom(deserializer, "address", AddressEncoding.COMPRESSED);
		final Address addressWithPublicKey = Address.readFromOptional(deserializer, "publicKey", AddressEncoding.PUBLIC_KEY);
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
	 * Gets the account's vested balance.
	 *
	 * @return This account's vested balance.
	 */
	public Amount getVestedBalance() {
		return this.vestedBalance;
	}

	/**
	 * Gets number of harvested blocks.
	 *
	 * @return Number of blocks harvested by this account.
	 */
	public BlockAmount getNumHarvestedBlocks() {
		return this.numHarvestedBlocks;
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

	/**
	 * Gets the multisig info associated with this account.
	 *
	 * @return The multisig info associated with this account.
	 */
	public MultisigInfo getMultisigInfo() {
		return this.multisigInfo;
	}

	@Override
	public void serialize(final Serializer serializer) {
		Address.writeTo(serializer, "address", this.getAddress(), AddressEncoding.COMPRESSED);
		Address.writeTo(serializer, "publicKey", this.getAddress(), AddressEncoding.PUBLIC_KEY);

		Amount.writeTo(serializer, "balance", this.getBalance());
		Amount.writeTo(serializer, "vestedBalance", this.getVestedBalance());
		BlockAmount.writeTo(serializer, "harvestedBlocks", this.getNumHarvestedBlocks());
		serializer.writeString("label", this.getLabel());

		serializer.writeDouble("importance", this.getImportance());

		serializer.writeObject("multisigInfo", this.multisigInfo);
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
