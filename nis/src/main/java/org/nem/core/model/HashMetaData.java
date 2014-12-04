package org.nem.core.model;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

/**
 * Class holding additional information about a hash.
 */
public class HashMetaData {
	private final BlockHeight height;
	private final TimeInstant timeStamp;

	/**
	 * Creates a hash meta data.
	 *
	 * @param height The height.
	 * @param timeStamp The time stamp.
	 */
	public HashMetaData(final BlockHeight height, final TimeInstant timeStamp) {
		this.height = height;
		this.timeStamp = timeStamp;
	}

	/**
	 * Gets the height at which the hash was first seen.
	 *
	 * @return The height.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets the time stamp at which the hash was first seen.
	 *
	 * @return The time stamp.
	 */
	public TimeInstant getTimeStamp() {
		return this.timeStamp;
	}

	@Override
	public int hashCode() {
		return this.height.hashCode() ^ this.timeStamp.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof HashMetaData)) {
			return false;
		}

		final HashMetaData rhs = (HashMetaData)obj;
		return this.height.equals(rhs.height)
				&& this.timeStamp.equals(rhs.timeStamp);
	}
}
