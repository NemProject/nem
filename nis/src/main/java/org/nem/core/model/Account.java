package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;

import java.util.*;

/**
 * A NEM account.
 */
public class Account {
	private KeyPair keyPair;
	private Address address;
	private final List<Message> messages;
	private String label;
	private Amount balance = Amount.ZERO;
	private BlockAmount foragedBlocks = BlockAmount.ZERO;
	private ReferenceCount refCount = ReferenceCount.ZERO;

	/**
	 * Creates an account around a key pair.
	 *
	 * @param keyPair The key pair.
	 */
	public Account(final KeyPair keyPair) {
		this(keyPair, getAddressFromKeyPair(keyPair));
	}

	/**
	 * Creates an account around an address. This constructor should only be
	 * used if the account's public key is not known.
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
	 * This method is public, but it should be used very carefully.
	 * TODO: revisit
	 *
	 * @param address An address that matches account's address but includes a public key.
	 */
	public void _setPublicKey(final Address address) {
		if (!Address.fromPublicKey(address.getPublicKey()).getEncoded().equals(address.getEncoded())) {
			throw new IllegalArgumentException("most probably trying to set public key for wrong account");
		}

		this.keyPair = new KeyPair(address.getPublicKey());
		this.address = Address.fromPublicKey(address.getPublicKey());
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
	}

	private Account(final Account rhs) {
		this.keyPair = rhs.getKeyPair();
		this.address = rhs.getAddress();

		this.balance = rhs.getBalance();
		this.label = rhs.getLabel();
		this.foragedBlocks = rhs.getForagedBlocks();

		this.messages = new ArrayList<>();
		this.messages.addAll(rhs.getMessages());

		this.refCount = rhs.getReferenceCount();
	}

	private Account(final Account rhs, final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.address = getAddressFromKeyPair(keyPair);
		this.balance = rhs.getBalance();
		this.label = rhs.getLabel();
		this.foragedBlocks = rhs.getForagedBlocks();

		this.messages = rhs.getMessages();
		this.refCount = rhs.getReferenceCount();
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
	 * 
	 * Creates a shallow copy of this account the the specified key pair.
	 * 
	 * @param keyPair The desired key pair of the new account.
	 * @return The shallow copy.
	 */
	public Account shallowCopyWithKeyPair(final KeyPair keyPair) {
		return new Account(this, keyPair);
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
	 * Returns the reference count.
	 * 
	 * @return The reference count.
	 */
	public ReferenceCount getReferenceCount() {
		return this.refCount;
	}
	
	/**
	 * Increments the reference count.
	 *
	 * @return The new value of the reference count.
	 */
	public ReferenceCount incrementReferenceCount() {
		this.refCount = this.refCount.increment();
		return this.refCount;
	}
	
	/**
	 * Decrements the reference count.
	 *
	 * @return The new value of the reference count.
	 */
	public ReferenceCount decrementReferenceCount() {
		this.refCount = this.refCount.decrement();
		return this.refCount;
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Account))
			return false;

		final Account rhs = (Account)obj;
		return this.address.equals(rhs.address);
	}

	//region inline serialization

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param account The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Account account) {
		writeTo(serializer, label, account, AddressEncoding.COMPRESSED);
	}

	/**
	 * Writes an account object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param account The object.
	 * @param encoding The account encoding mode.
	 */
	public static void writeTo(
			final Serializer serializer,
			final String label,
			final Account account,
			final AddressEncoding encoding) {
		Address.writeTo(serializer, label, account.getAddress(), encoding);
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(final Deserializer deserializer, final String label) {
		return readFrom(deserializer, label, AddressEncoding.COMPRESSED);
	}

	/**
	 * Reads an account object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param encoding The account encoding.
	 * @param label The optional label.
	 *
	 * @return The read object.
	 */
	public static Account readFrom(
			final Deserializer deserializer,
			final String label,
			final AddressEncoding encoding) {
		final Address address = Address.readFrom(deserializer, label, encoding);
		return deserializer.getContext().findAccountByAddress(address);
	}

	//endregion
}
