package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.messages.MessageFactory;
import org.nem.core.serialization.*;

import java.util.*;

/**
 * A NEM account.
 */
public class Account implements SerializableEntity {

	private final KeyPair keyPair;
	private final Address address;
	private final List<Message> messages;
	private String label;
	private Amount balance = Amount.ZERO;
	private BlockAmount foragedBlocks = BlockAmount.ZERO;

	private final WeightedBalances weightedBalances;
	private final AccountImportance importance;

	private BlockHeight height;

	/**
	 * Creates an account around a key pair.
	 *
	 * @param keyPair The key pair.
	 */
	public Account(final KeyPair keyPair) {
		this(keyPair, getAddressFromKeyPair(keyPair));
	}

	/**
	 * Creates an account around an address. This constructor should only
	 * be used if the account's public key is not known.
	 *
	 * @param address The address.
	 */
	public Account(final Address address) {
		this(getKeyPairFromAddress(address), address);
	}

	private static KeyPair getKeyPairFromAddress(final Address address) {
		return null == address.getPublicKey() ? null : new KeyPair(address.getPublicKey());
	}

	private static Address getAddressFromKeyPair(final KeyPair keyPair) {
		return Address.fromPublicKey(keyPair.getPublicKey());
	}

	/**
	 * Creates an account around a key pair and address.
	 *
	 * @param keyPair The key pair.
	 * @param address The address.
	 */
	protected Account(final KeyPair keyPair, final Address address) {
		this.keyPair = keyPair;
		this.address = address;
		this.messages = new ArrayList<>();
		this.weightedBalances = new WeightedBalances();
		this.importance = new AccountImportance();
	}

	private Account(final Account rhs) {
		this.keyPair = rhs.getKeyPair();
		this.address = rhs.getAddress();

		this.balance = rhs.getBalance();
		this.label = rhs.getLabel();
		this.foragedBlocks = rhs.getForagedBlocks();

		this.messages = new ArrayList<>();
		this.messages.addAll(rhs.getMessages());
		this.weightedBalances = rhs.weightedBalances.copy();
		this.importance = rhs.importance.copy();

		this.height = rhs.getHeight();
	}

	private Account(final Account rhs, final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.address = getAddressFromKeyPair(keyPair);

		this.balance = rhs.getBalance();
		this.label = rhs.getLabel();
		this.foragedBlocks = rhs.getForagedBlocks();

		this.messages = rhs.getMessages();
		this.weightedBalances = rhs.weightedBalances;
		this.importance = rhs.importance;

		this.height = rhs.getHeight();
	}

	/**
	 * Creates an unlinked copy of this account.
	 *
	 * @return An unlinked copy of this account.
	 */
	public Account copy() {
		return new Account(this);
	}

	/**
	 * Creates a shallow copy of this account with the specified address.
	 *
	 * @param address The desired address of the new account.
	 * @return The shallow copy.
	 */
	public Account shallowCopyWithAddress(final Address address) {
		return this.shallowCopyWithKeyPair(getKeyPairFromAddress(address));
	}

	/**
	 * Creates a shallow copy of this account the the specified key pair.
	 *
	 * @param keyPair The desired key pair of the new account.
	 * @return The shallow copy.
	 */
	public Account shallowCopyWithKeyPair(final KeyPair keyPair) {
		return new Account(this, keyPair);
	}

	/**
	 * Deserializes an account.
	 * Currently this deserializer is used ONLY by NCC
	 *
	 * @param deserializer The deserializer.
	 */
	public Account(final Deserializer deserializer) {
		this(deserializeAddress(deserializer));

		this.balance = Amount.readFrom(deserializer, "balance");
		this.foragedBlocks = BlockAmount.readFrom(deserializer, "foragedBlocks");
		this.label = deserializer.readString("label");

		final BlockHeight importanceHeight = BlockHeight.readFrom(deserializer, "importanceHeight");
		final Double importanceValue = deserializer.readDouble("importance");
		this.importance.setImportance(importanceHeight, importanceValue);
		this.messages.addAll(deserializer.readObjectArray("messages", MessageFactory.DESERIALIZER));
	}

	private static Address deserializeAddress(final Deserializer deserializer) {
		final Address addressWithoutPublicKey = readAddress(deserializer, "address", AccountEncoding.ADDRESS);
		final Address addressWithPublicKey = readAddress(deserializer, "publicKey", AccountEncoding.PUBLIC_KEY);
		return null != addressWithPublicKey ? addressWithPublicKey : addressWithoutPublicKey;
	}

	@Override
	public void serialize(final Serializer serializer) {
		writeTo(serializer, "address", this, AccountEncoding.ADDRESS);
		writeTo(serializer, "publicKey", this, AccountEncoding.PUBLIC_KEY);

		Amount.writeTo(serializer, "balance", this.getBalance());
		BlockAmount.writeTo(serializer, "foragedBlocks", this.getForagedBlocks());
		serializer.writeString("label", this.getLabel());

		final BlockHeight importanceHeight = this.getImportanceInfo().getHeight();
		if (importanceHeight != null) {
			BlockHeight.writeTo(serializer, "importanceHeight", importanceHeight);
			serializer.writeDouble("importance", this.getImportanceInfo().getImportance(importanceHeight));
		} else {
			BlockHeight.writeTo(serializer, "importanceHeight", BlockHeight.ONE);
			serializer.writeDouble("importance", 0.0);
		}
		serializer.writeObjectArray("messages", this.getMessages());
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
	 * Adds amount to the account's balance.
	 *
	 * @param amount The amount by which to increment the balance.
	 */
	public void incrementBalance(final Amount amount) {
		this.balance = this.balance.add(amount);
	}

	/**
	 * Subtracts amount from the account's balance.
	 *
	 * @param amount The amount by which to decrement the balance.
	 */
	public void decrementBalance(final Amount amount) {
		this.balance = this.balance.subtract(amount);
	}

	/**
	 * Gets number of foraged blocks.
	 *
	 * @return Number of blocks foraged by this account.
	 */
	public BlockAmount getForagedBlocks() {
		return foragedBlocks;
	}

	/**
	 * Increments number of foraged blocks by this account by one.
	 */
	public void incrementForagedBlocks() {
		this.foragedBlocks = this.foragedBlocks.increment();
	}

	/**
	 * Decrements number of foraged blocks by this account by one.
	 */
	public void decrementForagedBlocks() {
		this.foragedBlocks = this.foragedBlocks.decrement();
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
	 * Sets the account's label.
	 *
	 * @param label The desired label.
	 */
	public void setLabel(final String label) {
		this.label = label;
	}

	/**
	 * Gets all messages associated with the account.
	 *
	 * @return All messages associated with the account.
	 */
	public List<Message> getMessages() {
		return this.messages;
	}

	/**
	 * Associates message with the account.
	 *
	 * @param message The message to associate with this account.
	 */
	public void addMessage(final Message message) {
		this.messages.add(message);
	}

	/**
	 * Removes the last occurrence of the specified message from this account.
	 *
	 * @param message The message to remove from this account.
	 */
	public void removeMessage(final Message message) {
		for (int i = this.messages.size() - 1; i >= 0; --i) {
			if (message.equals(this.messages.get(i))) {
				this.messages.remove(i);
				break;
			}
		}
	}

	/**
	 * Returns height of an account.
	 * @return The height of an account - when the account has been created.
	 */
	public BlockHeight getHeight() {
		return height;
	}

	/**
	 * Sets height of an account.
	 * @param height
	 */
	public void setHeight(final BlockHeight height) {
		this.height = height;
	}

	/**
	 * Gets the weighted balances associated with this account.
	 *
	 * @return The weighted balances
	 */
	public WeightedBalances getWeightedBalances() {
		return this.weightedBalances;
	}

	/**
	 * Gets the importance information associated with this account.
	 *
	 * @return The importance information associated with this account.
	 */
	public AccountImportance getImportanceInfo() {
		return this.importance;
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Account))
			return false;

		Account rhs = (Account)obj;
		return this.address.equals(rhs.address);
	}

	//region inline serialization

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label      The optional label.
	 * @param account    The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Account account) {
		writeTo(serializer, label, account, AccountEncoding.ADDRESS);
	}

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label      The optional label.
	 * @param account    The object.
	 * @param encoding   The account encoding mode.
	 */
	public static void writeTo(
			final Serializer serializer,
			final String label,
			final Account account,
			final AccountEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				final KeyPair keyPair = account.getKeyPair();
				serializer.writeBytes(label, null != keyPair ? keyPair.getPublicKey().getRaw() : null);
				break;

			case ADDRESS:
			default:
				Address.writeTo(serializer, label, account.getAddress());
				break;
		}
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label        The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(final Deserializer deserializer, final String label) {
		return readFrom(deserializer, label, AccountEncoding.ADDRESS);
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param encoding     The account encoding.
	 * @param label        The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(
			final Deserializer deserializer,
			final String label,
			final AccountEncoding encoding) {
		final Address address = readAddress(deserializer, label, encoding);
		return deserializer.getContext().findAccountByAddress(address);
	}

	private static Address readAddress(
			final Deserializer deserializer,
			final String label,
			final AccountEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				final byte[] publicKeyBytes = deserializer.readBytes(label);
				return null == publicKeyBytes ? null : Address.fromPublicKey(new PublicKey(publicKeyBytes));

			case ADDRESS:
			default:
				return Address.readFrom(deserializer, label);
		}
	}

	//endregion
}