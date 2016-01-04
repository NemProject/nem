package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.*;

import java.util.Arrays;

/**
 * A NEM address.
 */
public class Address implements Comparable<Address> {
	private static final int NUM_CHECKSUM_BYTES = 4;
	private static final int NUM_DECODED_BYTES_LENGTH = 40;
	private static final int NUM_ENCODED_BYTES_LENGTH = 25;
	private final String encoded; // base-32 encoded address
	private final PublicKey publicKey;
	private Boolean isValid;

	/**
	 * Creates an Address from a public key.
	 *
	 * @param publicKey The public key.
	 * @return An address object.
	 */
	public static Address fromPublicKey(final PublicKey publicKey) {
		return fromPublicKey(NetworkInfos.getDefault().getVersion(), publicKey);
	}

	/**
	 * Creates an address object from a version and public key.
	 *
	 * @param version The address version.
	 * @param publicKey The address public key.
	 * @return An address object.
	 */
	public static Address fromPublicKey(final byte version, final PublicKey publicKey) {
		if (null == publicKey) {
			throw new IllegalArgumentException("public key cannot be null");
		}

		return new Address(version, publicKey);
	}

	/**
	 * Creates an Address from an encoded address string.
	 *
	 * @param encoded The encoded address string.
	 * @return An address object.
	 */
	public static Address fromEncoded(final String encoded) {
		if (null == encoded) {
			throw new IllegalArgumentException("encoded address cannot be null");
		}

		return new Address(encoded.toUpperCase());
	}

	/**
	 * Creates an Address from both public key and an encoded address string.
	 * This is protected because consistency is not checked.
	 *
	 * @param publicKey The public key.
	 * @param encoded The encoded address string.
	 * @param isValid true if the encoded address string is valid; false otherwise.
	 */
	protected Address(final PublicKey publicKey, final String encoded, final Boolean isValid) {
		this.publicKey = publicKey;
		this.encoded = encoded;
		this.isValid = isValid;
	}

	private Address(final String encoded) {
		this(null, encoded, null);
	}

	private Address(final byte version, final PublicKey publicKey) {
		this(publicKey, generateEncoded(version, publicKey.getRaw()), true);
	}

	private static String generateEncoded(final byte version, final byte[] publicKey) {
		// step 1: sha3 hash of the public key
		final byte[] sha3PublicKeyHash = Hashes.sha3_256(publicKey);

		// step 2: ripemd160 hash of (1)
		final byte[] ripemd160StepOneHash = Hashes.ripemd160(sha3PublicKeyHash);

		// step 3: add version byte in front of (2)
		final byte[] versionPrefixedRipemd160Hash = ArrayUtils.concat(new byte[] { version }, ripemd160StepOneHash);

		// step 4: get the checksum of (3)
		final byte[] stepThreeChecksum = generateChecksum(versionPrefixedRipemd160Hash);

		// step 5: concatenate (3) and (4)
		final byte[] concatStepThreeAndStepSix = ArrayUtils.concat(versionPrefixedRipemd160Hash, stepThreeChecksum);

		// step 6: base32 encode (5)
		return Base32Encoder.getString(concatStepThreeAndStepSix);
	}

	private static byte[] generateChecksum(final byte[] input) {
		// step 1: sha3 hash of (input
		final byte[] sha3StepThreeHash = Hashes.sha3_256(input);

		// step 2: get the first X bytes of (1)
		return Arrays.copyOfRange(sha3StepThreeHash, 0, NUM_CHECKSUM_BYTES);
	}

	/**
	 * Gets the encoded address string.
	 *
	 * @return The encoded address string.
	 */
	public String getEncoded() {
		return this.encoded;
	}

	/**
	 * Gets the address public key.
	 *
	 * @return The address public key.
	 */
	public PublicKey getPublicKey() {
		return this.publicKey;
	}

	/**
	 * Gets the address version.
	 *
	 * @return The address version.
	 */
	public byte getVersion() {
		return Base32Encoder.getBytes(this.encoded)[0];
	}

	/**
	 * Determines if the address is valid.
	 *
	 * @return true if the address is valid.
	 */
	public boolean isValid() {
		if (null == this.isValid) {
			this.isValid = isEncodedAddressValid(this.encoded);
		}

		return this.isValid;
	}

	private static boolean isEncodedAddressValid(final String encoded) {
		// this check should prevent leading and trailing whitespace
		if (NUM_DECODED_BYTES_LENGTH != encoded.length()) {
			return false;
		}

		final byte[] encodedBytes;

		try {
			encodedBytes = Base32Encoder.getBytes(encoded);
		} catch (final IllegalArgumentException e) {
			return false;
		}
		if (NUM_ENCODED_BYTES_LENGTH != encodedBytes.length) {
			return false;
		}

		if (NetworkInfos.getDefault().getVersion() != encodedBytes[0]) {
			return false;
		}

		final int checksumStartIndex = NUM_ENCODED_BYTES_LENGTH - NUM_CHECKSUM_BYTES;
		final byte[] versionPrefixedHash = Arrays.copyOfRange(encodedBytes, 0, checksumStartIndex);
		final byte[] addressChecksum = Arrays.copyOfRange(encodedBytes, checksumStartIndex, checksumStartIndex + NUM_CHECKSUM_BYTES);
		final byte[] calculatedChecksum = generateChecksum(versionPrefixedHash);
		return Arrays.equals(addressChecksum, calculatedChecksum);
	}

	@Override
	public int hashCode() {
		return this.encoded.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof Address)) {
			return false;
		}

		final Address rhs = (Address)obj;
		return this.encoded.equals(rhs.encoded);
	}

	@Override
	public String toString() {
		return this.encoded;
	}

	@Override
	public int compareTo(final Address rhs) {
		return this.getEncoded().compareTo(rhs.getEncoded());
	}

	/**
	 * Writes an address object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param address The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Address address) {
		writeTo(serializer, label, address, AddressEncoding.COMPRESSED);
	}

	/**
	 * Writes an address object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param address The object.
	 * @param encoding The address encoding mode.
	 */
	public static void writeTo(
			final Serializer serializer,
			final String label,
			final Address address,
			final AddressEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				final PublicKey publicKey = address.getPublicKey();
				serializer.writeBytes(label, null == publicKey ? null : publicKey.getRaw());
				break;

			case COMPRESSED:
			default:
				serializer.writeString(label, address.getEncoded());
				break;
		}
	}

	/**
	 * Reads an address object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Address readFrom(final Deserializer deserializer, final String label) {
		return readFrom(deserializer, label, AddressEncoding.COMPRESSED);
	}

	/**
	 * Reads an address object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param encoding The address encoding.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Address readFrom(
			final Deserializer deserializer,
			final String label,
			final AddressEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				return createAddressFromPublicKeyBytes(deserializer.readBytes(label));

			case COMPRESSED:
			default:
				return createAddressFromEncodedAddress(deserializer.readString(label));
		}
	}

	/**
	 * Reads an optional object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param encoding The address encoding.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Address readFromOptional(
			final Deserializer deserializer,
			final String label,
			final AddressEncoding encoding) {
		switch (encoding) {
			case PUBLIC_KEY:
				return createAddressFromPublicKeyBytes(deserializer.readOptionalBytes(label));

			case COMPRESSED:
			default:
				return createAddressFromEncodedAddress(deserializer.readOptionalString(label));
		}
	}

	private static Address createAddressFromPublicKeyBytes(final byte[] bytes) {
		return null == bytes ? null : Address.fromPublicKey(new PublicKey(bytes));
	}

	private static Address createAddressFromEncodedAddress(final String encoded) {
		return null == encoded ? null : Address.fromEncoded(encoded);
	}
}
