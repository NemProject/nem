package main.java.org.nem.core.crypto;

import java.security.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Hashes {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] sha3(final byte[]... inputs) throws Exception {
        return hash("SHA3-256", inputs);
    }

    public static byte[] ripemd160(final byte[]... inputs) throws Exception {
        return hash("RIPEMD160", inputs);
    }

    private static byte[] hash(final String algorithm, final byte[]... inputs) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm, "BC");

        for (byte[] input : inputs)
            digest.update(input);

        return digest.digest();
    }
}