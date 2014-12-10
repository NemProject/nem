package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

/**
 * A NEM account.
 */
public class Account {
	private KeyPair keyPair;
	private Address address;

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
	 * Sets the public key associated with this account.
	 * The public key must be consistent with this account's address.
	 * This function should be used sparingly, and avoided if possible.
	 *
	 * @param publicKey The public key.
	 */
	public void setPublicKey(final PublicKey publicKey) {
		if (!Address.fromPublicKey(publicKey).equals(this.address)) {
			throw new IllegalArgumentException("most probably trying to set public key for wrong account");
		}

		// only update the public key if it is not already set
		if (null != this.keyPair) {
			return;
		}

		this.keyPair = new KeyPair(publicKey);
		this.address = Address.fromPublicKey(publicKey);
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
	}

	private Account(final Account rhs) {
		this.keyPair = rhs.keyPair;
		this.address = rhs.getAddress();
	}

	private Account(final Account rhs, final KeyPair keyPair) {
		this.keyPair = keyPair;
		this.address = getAddressFromKeyPair(keyPair);
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
	 * Creates a shallow copy of this account the the specified key pair.
	 *
	 * @param keyPair The desired key pair of the new account.
	 * @return The shallow copy.
	 */
	public Account shallowCopyWithKeyPair(final KeyPair keyPair) {
		return new Account(this, keyPair);
	}

	/**
	 * Gets the account's address.
	 *
	 * @return This account's address.
	 */
	public Address getAddress() {
		return this.address;
	}

	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Account)) {
			return false;
		}

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

	public Signer createSigner() {
		if (!this.hasPublicKey()) {
			throw new IllegalArgumentException("in order to create a signer, an account must have a public key");
		}

		return new Signer(this.keyPair);
	}

	public Cipher createCipher(final Account other, boolean encrypt) {
		if (!this.hasPrivateKey() && !other.hasPrivateKey()) {
			throw new IllegalArgumentException("in order to create a cipher, at least one account must have a private key");
		}

		return this.hasPrivateKey() && !encrypt
				? new Cipher(other.keyPair, this.keyPair)
				: new Cipher(this.keyPair, other.keyPair);
	}

	public boolean hasPrivateKey() {
		return null != this.keyPair && this.keyPair.hasPrivateKey();
	}

	public boolean hasPublicKey() {
		return null != this.keyPair;
	}
}
