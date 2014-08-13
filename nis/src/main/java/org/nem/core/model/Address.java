package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.*;

import java.util.Arrays;

/**
 * A NEM address.
 */
public class Address {
	private static final int NUM_CHECKSUM_BYTES = 4;
	private static final int NUM_DECODED_BYTES_LENGTH = 40;
	private static final int NUM_ENCODED_BYTES_LENGTH = 25;
	private final String encoded; // base-32 encoded address
	private final PublicKey publicKey;

	/**
	 * Creates an Address from a public key.
	 *
	 * @param publicKey The public key.
	 * @return An address object.
	 */
	public static Address fromPublicKey(final PublicKey publicKey) {
		return new Address(NetworkInfo.getDefault().getVersion(), publicKey);
	}

	/**
	 * Creates an Address from an encoded address string.
	 *
	 * @param encoded The encoded address string.
	 * @return An address object.
	 */
	public static Address fromEncoded(final String encoded) {
		return new Address(encoded.toUpperCase());
	}

	/**
	 * Creates an address object from an encoded address.
	 *
	 * @param encoded The encoded address.
	 */
	private Address(final String encoded) {
		this.encoded = encoded;
		this.publicKey = null;
	}

	/**
	 * Creates an address object from a version and public key.
	 *
	 * @param version The address version.
	 * @param publicKey The address public key.
	 */
	private Address(final byte version, final PublicKey publicKey) {
		this.encoded = generateEncoded(version, publicKey.getRaw());
		this.publicKey = publicKey;
	}

	private static String generateEncoded(final byte version, final byte[] publicKey) {
		// step 1: sha3 hash of the public key
		final byte[] sha3PublicKeyHash = Hashes.sha3(publicKey);

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
		final byte[] sha3StepThreeHash = Hashes.sha3(input);

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
	 * Determines if the address is valid.
	 *
	 * @return true if the address is valid.
	 */
	public boolean isValid() {
		// this check should prevent leading and trailing whitespace
		// TODO: we can't release this change now, as there is a bug in current nemesis block,
		// quote: "I've added such (invalid) account by mistake to nemesis block"
		// you can check it by un-commenting and running test added to NemesisBlockTest
		//		if (NUM_DECODED_BYTES_LENGTH != this.encoded.length())
		//			return false;

		final byte[] encodedBytes;

		try {
			encodedBytes = Base32Encoder.getBytes(this.encoded);
		} catch (final IllegalArgumentException e) {
			return false;
		}
		if (NUM_ENCODED_BYTES_LENGTH != encodedBytes.length) {
			return false;
		}

		if (NetworkInfo.getDefault().getVersion() != encodedBytes[0]) {
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
		return this.encoded.toLowerCase().hashCode();
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
				serializer.writeBytes(label, null == address.publicKey ? null : address.publicKey.getRaw());
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
				final byte[] publicKeyBytes = deserializer.readOptionalBytes(label);
				return null == publicKeyBytes ? null : Address.fromPublicKey(new PublicKey(publicKeyBytes));

			case COMPRESSED:
			default:
				final String encodedAddress = deserializer.readString(label);
				return Address.fromEncoded(encodedAddress);
		}
	}
}
