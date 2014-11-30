package org.nem.core.model;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;

/**
 * Class holding additional information about a hash.
 */
public class HashMetaData {
	private final BlockHeight height;
	private final TimeInstant timeStamp;

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
}
