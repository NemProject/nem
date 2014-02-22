package org.nem.core.utils;

import java.security.InvalidParameterException;

/**
 * Static class that contains a handful of array helper functions.
 */
public class ArrayUtils {

    /**
     * Concatenates two arrays and returns the result.
     *
     * @param lhs The first array.
     * @param rhs The second array.
     * @return An array that contains all the elements in lhs concatenated with all the elements in rhs.
     */
    public static byte[] concat(final byte[] lhs, final byte[] rhs) {
        byte[] result = new byte[lhs.length + rhs.length];
        System.arraycopy(lhs, 0, result, 0, lhs.length);
        System.arraycopy(rhs, 0, result, lhs.length, rhs.length);
        return result;
    }

    /**
     * Splits a single array into two arrays.
     *
     * @param bytes The input array.
     * @param splitIndex The index at which the array should be split.
     * @return Two arrays split at the splitIndex.
     *         The first array will contain the first splitIndex elements.
     *         The second array will contain all trailing elements.
     */
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
