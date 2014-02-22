package org.nem.core.crypto;

import java.security.*;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Hashes {
    private static final Logger logger = Logger.getLogger(Hashes.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] sha3(final byte[]... inputs) {
        return hash("SHA3-256", inputs);
    }

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
            logger.warning(e.toString());
            e.printStackTrace();
            return null;

        } catch (NoSuchProviderException e) {
            logger.warning(e.toString());
            e.printStackTrace();
            return null;
        }
    }
}