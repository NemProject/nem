package org.nem.core.utils;

import java.math.BigInteger;

/**
 * Static class that contains a handful of array helper functions.
 */
public class ArrayUtils {
	/**
	 * Creates duplicate of given array
	 *
	 * @param src - array to duplicate
	 *
	 * @return copy of an array
	 */
	public static byte[] duplicate(final byte[] src) {
		final byte[] result = new byte[src.length];
		System.arraycopy(src, 0, result, 0, src.length);
		return result;
	}

	/**
	 * Concatenates two arrays and returns the result.
	 *
	 * @param lhs The first array.
	 * @param rhs The second array.
	 *
	 * @return An array that contains all the elements in lhs concatenated with all the elements in rhs.
	 */
	public static byte[] concat(final byte[] lhs, final byte[] rhs) {
		final byte[] result = new byte[lhs.length + rhs.length];
		System.arraycopy(lhs, 0, result, 0, lhs.length);
		System.arraycopy(rhs, 0, result, lhs.length, rhs.length);
		return result;
	}

	/**
	 * Splits a single array into two arrays.
	 *
	 * @param bytes      The input array.
	 * @param splitIndex The index at which the array should be split.
	 *
	 * @return Two arrays split at the splitIndex.
	 * The first array will contain the first splitIndex elements.
	 * The second array will contain all trailing elements.
	 */
	public static byte[][] split(final byte[] bytes, final int splitIndex) {
		if (splitIndex < 0 || bytes.length < splitIndex)
			throw new IllegalArgumentException("split index is out of range");

		final byte[] lhs = new byte[splitIndex];
		final byte[] rhs = new byte[bytes.length - splitIndex];

		System.arraycopy(bytes, 0, lhs, 0, lhs.length);
		System.arraycopy(bytes, splitIndex, rhs, 0, rhs.length);
		return new byte[][] { lhs, rhs };
	}

	/**
	 * Converts a BigInteger to a little endian byte array.
	 *
	 * @param value The value to convert.
	 *
	 * @return The resulting little endian byte array.
	 */
	public static byte[] toByteArray(final BigInteger value, final int numBytes) {
		final byte[] outputBytes = new byte[numBytes];
		final byte[] bigIntegerBytes = value.toByteArray();

		int copyStartIndex = (0x00 == bigIntegerBytes[0]) ? 1 : 0;
		int numBytesToCopy = bigIntegerBytes.length - copyStartIndex;
		if (numBytesToCopy > numBytes) {
			copyStartIndex += numBytesToCopy - numBytes;
			numBytesToCopy = numBytes;
		}

		for (int i = 0; i < numBytesToCopy; ++i)
			outputBytes[i] = bigIntegerBytes[copyStartIndex + numBytesToCopy - i - 1];

		return outputBytes;
	}

	/**
	 * Converts a little endian byte array to a BigInteger.
	 *
	 * @param bytes The bytes to convert.
	 *
	 * @return The resulting BigInteger.
	 */
	public static BigInteger toBigInteger(final byte[] bytes) {
		final byte[] bigEndianBytes = new byte[bytes.length + 1];
		for (int i = 0; i < bytes.length; ++i)
			bigEndianBytes[i + 1] = bytes[bytes.length - i - 1];

		return new BigInteger(bigEndianBytes);
	}
	
	/**
	 * Utility method to find the maximum value in an array.
	 * 
	 * @param vector
	 * @return double in <code>vector</code> that has the largest value.
	 */
	public static double max(double[] vector) {
		if (vector == null || vector.length < 1) {
			throw new IllegalArgumentException("input vector is empty");
		}
		double max = Double.MIN_VALUE;
		
		for (double val : vector) {
			if (max < val) {
				max = val;
			}
		}
		
		return max;
	}
}
