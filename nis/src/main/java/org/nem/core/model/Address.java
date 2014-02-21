package org.nem.core.model;

import org.apache.commons.codec.binary.Base32;
import java.util.Arrays;
import org.nem.core.crypto.*;

public class Address {
    private static final int NUM_CHECKSUM_BYTES = 4;
    private static final int NUM_ENCODED_BYTES_LENGTH = 25;
    private static final byte VERSION = 0x68;

    public static String fromPublicKey(final byte[] publicKey) throws Exception {
        // step 1: sha3 hash of the public key
        byte[] sha3PublicKeyHash = Hashes.sha3(publicKey);

        // step 2: ripemd160 hash of (1)
        byte[] ripemd160StepOneHash = Hashes.ripemd160(sha3PublicKeyHash);

        // step 3: add version byte in front of (2)
        byte[] versionPrefixedRipemd160Hash = concat(new byte[] { VERSION }, ripemd160StepOneHash);

        // step 4: get the checksum of (3)
        byte[] stepThreeChecksum = generateChecksum(versionPrefixedRipemd160Hash);

        // step 5: concatenate (3) and (4)
        byte[] concatStepThreeAndStepSix = concat(versionPrefixedRipemd160Hash, stepThreeChecksum);

        // step 6: base32 encode (5)
        return toBase32(concatStepThreeAndStepSix);
    }

    public static Boolean isValid(final String address) throws Exception {
        byte[] encodedBytes = fromBase32(address);
        if (NUM_ENCODED_BYTES_LENGTH != encodedBytes.length)
            return false;

        if (VERSION != encodedBytes[0])
            return false;

        int checksumStartIndex = NUM_ENCODED_BYTES_LENGTH - NUM_CHECKSUM_BYTES;
        byte[] versionPrefixedHash = Arrays.copyOfRange(encodedBytes, 0, checksumStartIndex);
        byte[] addressChecksum = Arrays.copyOfRange(encodedBytes, checksumStartIndex, checksumStartIndex + NUM_CHECKSUM_BYTES);
        byte[] calculatedChecksum = generateChecksum(versionPrefixedHash);
        return Arrays.equals(addressChecksum, calculatedChecksum);
    }

    private static byte[] generateChecksum(final byte[] input) throws Exception {
        // step 1: sha3 hash of (input
        byte[] sha3StepThreeHash = Hashes.sha3(input);

        // step 2: get the first X bytes of (1)
        return Arrays.copyOfRange(sha3StepThreeHash, 0, NUM_CHECKSUM_BYTES);
    }

    private static byte[] concat(final byte[] lhs, final byte[] rhs) {
        byte[] result = new byte[lhs.length + rhs.length];
        System.arraycopy(lhs, 0, result, 0, lhs.length);
        System.arraycopy(rhs, 0, result, lhs.length, rhs.length);
        return result;
    }

    private static String toBase32(final byte[] input) {
        Base32 codec = new Base32();
        byte[] decodedBytes = codec.encode(input);
        return new String(decodedBytes);
    }

    private static byte[] fromBase32(final String encodedString) throws Exception {
        Base32 codec = new Base32();
        byte[] encodedBytes = encodedString.getBytes("UTF-8");
        return codec.decode(encodedBytes);
    }
}
