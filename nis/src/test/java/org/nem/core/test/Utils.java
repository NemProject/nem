package org.nem.core.test;

import java.security.SecureRandom;

/**
 * Static class containing test utilities.
 */
public class Utils {

    /**
     * Generates a byte array containing random data.
     */
    public static byte[] generateRandomBytes() {
        return generateRandomBytes(214);
    }

    /**
     * Generates a byte array containing random data.
     *
     * @param numBytes The number of bytes to generate.
     */
    public static byte[] generateRandomBytes(int numBytes) {
        SecureRandom rand = new SecureRandom();
        byte[] input = new byte[numBytes];
        rand.nextBytes(input);
        return input;
    }
}
