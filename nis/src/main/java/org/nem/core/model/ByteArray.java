package org.nem.core.model;

import org.nem.core.utils.ByteUtils;

import java.util.Arrays;

// or we could just use List<> instead...
public class ByteArray {
	private byte[] data;

	public ByteArray(byte[] data) {
		this.data = data;
	}

	/**
	 * Gets data
	 *
	 * @return data
	 */
	public byte[] get() {
		return data;
	}


	@Override
	public int hashCode() {
		return ByteUtils.bytesToInt(this.data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ByteArray))
			return false;

		ByteArray rhs = (ByteArray)obj;
		return Arrays.equals(this.data, rhs.data);
	}
}
