package main.java.org.nem.core.crypto;

import java.security.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Hashes {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] sha3(final byte[] input) throws Exception {
        return hash("SHA3-256", input);
    }

    public static byte[] ripemd160(final byte[] input) throws Exception {
        return hash("RIPEMD160", input);
    }

    private static byte[] hash(final String algorithm, final byte[] input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm, "BC");
        return digest.digest(input);
    }
}