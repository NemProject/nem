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
	 * @return copy of an array
	 */
	public static byte[] duplicate(final byte[] src) {
		final byte[] result = new byte[src.length];
		System.arraycopy(src, 0, result, 0, src.length);
		return result;
	}

	/**
	 * Concatenates byte arrays and returns the result.
	 *
	 * @param arrays The arrays.
	 * @return A single array containing all elements in all arrays.
	 */
	public static byte[] concat(final byte[]... arrays) {
		int totalSize = 0;
		for (final byte[] array : arrays) {
			totalSize += array.length;
		}

		int startIndex = 0;
		final byte[] result = new byte[totalSize];
		for (final byte[] array : arrays) {
			System.arraycopy(array, 0, result, startIndex, array.length);
			startIndex += array.length;
		}

		return result;
	}

	/**
	 * Splits a single array into two arrays.
	 *
	 * @param bytes The input array.
	 * @param splitIndex The index at which the array should be split.
	 * @return Two arrays split at the splitIndex.
	 * The first array will contain the first splitIndex elements.
	 * The second array will contain all trailing elements.
	 */
	public static byte[][] split(final byte[] bytes, final int splitIndex) {
		if (splitIndex < 0 || bytes.length < splitIndex) {
			throw new IllegalArgumentException("split index is out of range");
		}

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

		for (int i = 0; i < numBytesToCopy; ++i) {
			outputBytes[i] = bigIntegerBytes[copyStartIndex + numBytesToCopy - i - 1];
		}

		return outputBytes;
	}

	/**
	 * Converts a little endian byte array to a BigInteger.
	 *
	 * @param bytes The bytes to convert.
	 * @return The resulting BigInteger.
	 */
	public static BigInteger toBigInteger(final byte[] bytes) {
		final byte[] bigEndianBytes = new byte[bytes.length + 1];
		for (int i = 0; i < bytes.length; ++i) {
			bigEndianBytes[i + 1] = bytes[bytes.length - i - 1];
		}

		return new BigInteger(bigEndianBytes);
	}

	/**
	 * Utility method to find the maximum value in an array.
	 *
	 * @param vector - non-empty array of doubles
	 * @return double in <code>vector</code> that has the largest value.
	 */
	public static double max(final double[] vector) {
		if (vector == null || vector.length < 1) {
			throw new IllegalArgumentException("input vector is empty");
		}
		double max = Double.MIN_VALUE;

		for (final double val : vector) {
			if (max < val) {
				max = val;
			}
		}

		return max;
	}

	/**
	 * Utility method to find the maximum value in an array.
	 *
	 * @param vector - non-empty array of longs
	 * @return double in <code>vector</code> that has the largest value.
	 */
	public static long max(final long[] vector) {
		if (vector == null || vector.length < 1) {
			throw new IllegalArgumentException("input vector is empty");
		}
		long max = Long.MIN_VALUE;

		for (final long val : vector) {
			if (max < val) {
				max = val;
			}
		}

		return max;
	}

	/**
	 * Constant-time byte[] comparison.
	 * @return 1 if b and c are equal, 0 otherwise.
	 */
	public static int isEqual(final byte[] b, final byte[] c) {
		int result = 0;
		for (int i = 0; i < 32; i++) {
			result |= b[i] ^ c[i];
		}

		return ByteUtils.isEqual(result, 0);
	}

	/**
	 * Gets the i'th bit of a byte array.
	 *
	 * @param h The byte array.
	 * @param i The bit index.
	 * @return The value of the i'th bit in h
	 */
	public static int getBit(final byte[] h, final int i) {
		return (h[i >> 3] >> (i & 7)) & 1;
	}
}
