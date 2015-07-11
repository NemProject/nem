package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

/**
 * A NEM account.
 */
public class Account {
	private final KeyPair keyPair;
	private final Address address;

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
	 * Creates an account around a key pair and address.
	 *
	 * @param keyPair The key pair.
	 * @param address The address.
	 */
	protected Account(final KeyPair keyPair, final Address address) {
		this.keyPair = keyPair;
		this.address = address;
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

	@Override
	public String toString() {
		return this.address.toString();
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
		return null == address.getPublicKey() ? deserializer.getContext().findAccountByAddress(address) : new Account(address);
	}

	//endregion

	//region crypto

	/**
	 * Gets a value indicating whether or not this account has a private key.
	 *
	 * @return true if this account has a private key.
	 */
	public boolean hasPrivateKey() {
		return null != this.keyPair && this.keyPair.hasPrivateKey();
	}

	/**
	 * Gets a value indicating whether or not this account has a public key.
	 *
	 * @return true if this account has a private key.
	 */
	public boolean hasPublicKey() {
		return null != this.keyPair;
	}

	/**
	 * Creates a signer around this account's key-pair.
	 *
	 * @return The signer.
	 */
	public Signer createSigner() {
		if (!this.hasPublicKey()) {
			throw new CryptoException("in order to create a signer, an account must have a public key");
		}

		return new Signer(this.keyPair);
	}

	/**
	 * Creates a cipher around the key-pairs of this account and another account.
	 *
	 * @param other The other account.
	 * @param encrypt true if the cipher should be used for encrypting data, false otherwise.
	 * @return The cipher.
	 */
	public Cipher createCipher(final Account other, final boolean encrypt) {
		if (!this.hasPrivateKey() && !other.hasPrivateKey() || !this.hasPublicKey() || !other.hasPublicKey()) {
			throw new CryptoException("in order to create a cipher, at least one account must have a private key and both accounts must have a public key");
		}

		final KeyPair keyPairWithPrivateKey = this.hasPrivateKey() ? this.keyPair : other.keyPair;
		final KeyPair otherKeyPair = !this.hasPrivateKey() ? this.keyPair : other.keyPair;

		return encrypt
				? new Cipher(keyPairWithPrivateKey, otherKeyPair)
				: new Cipher(otherKeyPair, keyPairWithPrivateKey);
	}

	//endregion
}
