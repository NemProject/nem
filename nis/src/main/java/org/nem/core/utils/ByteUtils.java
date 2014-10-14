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
		// TODO 20141010 J-B: can you explain what you're doing here?
		// > are you treating the ints as bytes? if so, wouldn't this be faster:
		// (b & 0xFF) == (c & 0xFF)
		// TODO 20141011 BR -> J the method was "stolen" from the original github project. It's all about constant time behavior.
		// TODO 20141011         All the implementation I have seen use a "complex" strategy. Bernstein uses for int comparisons:
		// TODO 20141011         https://www.cipherdyne.org/lcov-results/openssh-6.6p1/openssh-6.6p1/verify.c.gcov.html
		// TODO 20141011         And yes, it's abused to compare only bytes as the input will have 8 relevant bits in our case (see Ed25519GroupElement.select()).
		// TODO 20141011         You may change it if you can guarantee constant time behavior.
		// TODO 20141010 J-B: i withdraw my comments / just rename with a suffix (isNegative too)
		// TODO 20141014 BR -> J: done.

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
		// TODO 20141010 J-B: b & 0x80 (probably doesn't matter bc java should optimize)
		// TODO 20141014 BR -> J: renaming done.
		return (b >> 8) & 1;
	}
}
