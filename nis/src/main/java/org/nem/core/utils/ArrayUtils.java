package org.nem.core.utils;

import java.security.InvalidParameterException;

public class ArrayUtils {

    public static byte[] concat(final byte[] lhs, final byte[] rhs) {
        byte[] result = new byte[lhs.length + rhs.length];
        System.arraycopy(lhs, 0, result, 0, lhs.length);
        System.arraycopy(rhs, 0, result, lhs.length, rhs.length);
        return result;
    }

    public static byte[][] split(final byte[] bytes, int splitIndex) {
        if (splitIndex < 0 || bytes.length < splitIndex)
            throw new InvalidParameterException("split index is out of range");

        byte[] lhs = new byte[splitIndex];
        byte[] rhs = new byte[bytes.length - splitIndex];

        System.arraycopy(bytes, 0, lhs, 0, lhs.length);
        System.arraycopy(bytes, splitIndex, rhs, 0, rhs.length);
        return new byte[][] { lhs, rhs };
    }
}
