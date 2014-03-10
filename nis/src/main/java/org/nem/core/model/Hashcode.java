package org.nem.core.model;

import org.nem.core.utils.ByteUtils;

import java.util.Arrays;

// or we could just use List<> instead...
public class Hashcode {
	private byte[] hash;

	public Hashcode(byte[] hash) {
		this.hash = hash;
	}

	/**
	 * Gets hash
	 *
	 * @return hash
	 */
	public byte[] getHash() {
		return hash;
	}


	@Override
	public int hashCode() {
		return ByteUtils.bytesToInt(this.hash);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Hashcode))
			return false;

		Hashcode rhs = (Hashcode)obj;
		return Arrays.equals(this.hash, rhs.hash);
	}
}
