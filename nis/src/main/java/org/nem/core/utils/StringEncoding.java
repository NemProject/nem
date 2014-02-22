package org.nem.core.utils;

import java.io.UnsupportedEncodingException;

/**
 * Static class that contains utility functions for converting strings to and from bytes.
 */
public class StringEncoding {
    public static byte[] getBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getString(byte[] encoded) {
        return new String(encoded);
    }
}
