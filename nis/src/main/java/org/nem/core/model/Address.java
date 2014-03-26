package org.nem.core.model;

import java.util.Arrays;

import org.nem.core.crypto.*;
import org.nem.core.utils.*;

/**
 * A NEM address.
 */
public class Address {
    private static final int NUM_CHECKSUM_BYTES = 4;
    private static final int NUM_ENCODED_BYTES_LENGTH = 25;
    private String encoded; // base-32 encoded address
    private PublicKey publicKey;

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
        return new Address(encoded);
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
        byte[] sha3PublicKeyHash = Hashes.sha3(publicKey);

        // step 2: ripemd160 hash of (1)
        byte[] ripemd160StepOneHash = Hashes.ripemd160(sha3PublicKeyHash);

        // step 3: add version byte in front of (2)
        byte[] versionPrefixedRipemd160Hash = ArrayUtils.concat(new byte[] { version }, ripemd160StepOneHash);

        // step 4: get the checksum of (3)
        byte[] stepThreeChecksum = generateChecksum(versionPrefixedRipemd160Hash);

        // step 5: concatenate (3) and (4)
        byte[] concatStepThreeAndStepSix = ArrayUtils.concat(versionPrefixedRipemd160Hash, stepThreeChecksum);

        // step 6: base32 encode (5)
        return Base32Encoder.getString(concatStepThreeAndStepSix);
    }

    private static byte[] generateChecksum(final byte[] input) {
        // step 1: sha3 hash of (input
        byte[] sha3StepThreeHash = Hashes.sha3(input);

        // step 2: get the first X bytes of (1)
        return Arrays.copyOfRange(sha3StepThreeHash, 0, NUM_CHECKSUM_BYTES);
    }

    /**
     * Gets the encoded address string.
     *
     * @return The encoded address string.
     */
    public String getEncoded() {
        return encoded;
    }

    /**
     * Gets the address public key.
     *
     * @return The address public key.
     */
    public PublicKey getPublicKey() { return this.publicKey; }

    /**
     * Determines if the address is valid.
     *
     * @return true if the address is valid.
     */
    public boolean isValid() {
        byte[] encodedBytes = Base32Encoder.getBytes(this.encoded);
        if (NUM_ENCODED_BYTES_LENGTH != encodedBytes.length)
            return false;

        if (NetworkInfo.getDefault().getVersion() != encodedBytes[0])
            return false;

        int checksumStartIndex = NUM_ENCODED_BYTES_LENGTH - NUM_CHECKSUM_BYTES;
        byte[] versionPrefixedHash = Arrays.copyOfRange(encodedBytes, 0, checksumStartIndex);
        byte[] addressChecksum = Arrays.copyOfRange(encodedBytes, checksumStartIndex, checksumStartIndex + NUM_CHECKSUM_BYTES);
        byte[] calculatedChecksum = generateChecksum(versionPrefixedHash);
        return Arrays.equals(addressChecksum, calculatedChecksum);
    }

    @Override
    public int hashCode() {
        return this.encoded.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Address))
            return false;

        Address rhs = (Address)obj;
        return this.encoded.equals(rhs.encoded);
    }
}
