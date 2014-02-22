package org.nem.core.crypto;

import java.security.*;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Static class that exposes hash functions.
 */
public class Hashes {
    private static final Logger logger = Logger.getLogger(Hashes.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Performs a SHA3-256 hash of the concatenated inputs.
     *
     * @param inputs The byte arrays to concatenate and hash.
     * @return The hash of the concatenated inputs.
     * @throws CryptoException if the hash operation failed.
     */
    public static byte[] sha3(final byte[]... inputs) {
        return hash("SHA3-256", inputs);
    }

    /**
     * Performs a RIPEMD160 hash of the concatenated inputs.
     *
     * @param inputs The byte arrays to concatenate and hash.
     * @return The hash of the concatenated inputs.
     * @throws CryptoException if the hash operation failed.
     */
    public static byte[] ripemd160(final byte[]... inputs) {
        return hash("RIPEMD160", inputs);
    }

    private static byte[] hash(final String algorithm, final byte[]... inputs) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm, "BC");

            for (byte[] input : inputs)
                digest.update(input);

            return digest.digest();

        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (NoSuchProviderException e) {
            throw new CryptoException(e);
        }
    }
}