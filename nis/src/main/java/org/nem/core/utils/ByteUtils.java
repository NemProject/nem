package org.nem.core.utils;

import java.nio.ByteBuffer;

public class ByteUtils {
	public static long bytesToLong(final byte[] bytes) {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.put(bytes, 0, 8);
		buffer.flip();
		return buffer.getLong();
	}

	public static byte[] longToBytes(final long x) {
		final ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(x);
		return buffer.array();
	}

	public static int bytesToInt(final byte[] bytes) {
		final ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put(bytes, 0, 4);
		buffer.flip();
		return buffer.getInt();
	}

	public static byte[] intToBytes(final int x) {
		final ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(x);
		return buffer.array();
	}

	/**
	 * Constant-time byte comparison. The constant time behavior eliminates side channel attacks.
	 *
	 * @return 1 if b and c are equal, 0 otherwise.
	 */
	public static int isEqualConstantTime(final int b, final int c) {
		int result = 0;
		final int xor = b ^ c;
		for (int i = 0; i < 8; i++) {
			result |= xor >> i;
		}

		return (result ^ 0x01) & 0x01;
	}

	/**
	 * Constant-time check if byte is negative. The constant time behavior eliminates side channel attacks.
	 *
	 * @param b the byte to check.
	 * @return 1 if the byte is negative, 0 otherwise.
	 */
	public static int isNegativeConstantTime(final int b) {
		return (b >> 8) & 1;
	}
}
