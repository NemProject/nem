package org.nem.core.test;

import java.security.SecureRandom;

public class Utils {

    public static byte[] generateRandomBytes() {
        return generateRandomBytes(214);
    }

    public static byte[] generateRandomBytes(int numBytes) {
        SecureRandom rand = new SecureRandom();
        byte[] input = new byte[numBytes];
        rand.nextBytes(input);
        return input;
    }
}
