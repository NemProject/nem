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
}
