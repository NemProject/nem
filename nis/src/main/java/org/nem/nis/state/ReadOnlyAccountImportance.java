package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableEntity;

import java.util.Iterator;

public interface ReadOnlyAccountImportance extends SerializableEntity {
	/**
	 * Gets an iterator that returns all outlinks between (inclusive) given start height and end height. <br>
	 * Note that this is readonly because AccountLink is immutable.
	 *
	 * @param startHeight The start block height.
	 * @param endHeight The end block height.
	 * @return The matching links.
	 */
	Iterator<AccountLink> getOutlinksIterator(BlockHeight startHeight, BlockHeight endHeight);

	/**
	 * Gets the number of outlinks at or before the (inclusive) given height.
	 *
	 * @param blockHeight The block height.
	 * @return The number of matching links.
	 */
	int getOutlinksSize(BlockHeight blockHeight);

	/**
	 * Gets the last page rank.
	 *
	 * @return The last page rank.
	 */
	double getLastPageRank();

	/**
	 * Gets the importance at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @return The importance.
	 */
	double getImportance(BlockHeight blockHeight);

	/**
	 * Gets the height at which importance is set.
	 *
	 * @return The height of importance.
	 */
	BlockHeight getHeight();

	/**
	 * Gets a value indicating whether or not the importance is set.
	 *
	 * @return true if the importance is set.
	 */
	boolean isSet();
}
