package org.nem.core.test;

import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;

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

    /**
     * Generates a random address.
     *
     * @return A random address.
     */
    public static Address generateRandomAddress() {
        return new Account(new KeyPair()).getAddress();
    }

    /**
     * Increments a single character in the specified string.
     *
     * @param s The string
     * @param index The index of the character to increment
     * @return The resulting string
     */
    public static String incrementAtIndex(final String s, final int index) {
        char[] chars = s.toCharArray();
        chars[index] = (char)(chars[index] + 1);
        return new String(chars);
    }

    /**
     * Increments a single byte in the specified byte array.
     *
     * @param bytes The byte array
     * @param index The index of the byte to increment
     * @return The resulting byte array
     */
    public static byte[] incrementAtIndex(final byte[] bytes, final int index) {
        byte[] copy = new byte[bytes.length];
        System.arraycopy(bytes, 0, copy, 0, bytes.length);
        ++copy[index];
        return copy;
    }

    /**
     * Creates a string initialized with a single character.
     *
     * @param ch The character used in the string.
     * @param numChars The number of characters in hte string.
     * @return A string of length numChars initialized to ch.
     */
    public static String createString(final char ch, final int numChars) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numChars; ++i)
            builder.append(ch);

        return builder.toString();
    }
}
