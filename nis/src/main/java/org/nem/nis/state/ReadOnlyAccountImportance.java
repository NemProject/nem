package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableEntity;

import java.util.Iterator;

public interface ReadOnlyAccountImportance extends SerializableEntity {
	/**
	 * Gets an iterator that returns all outlinks at or before the (inclusive) given height.
	 * TODO 20150111 G-*: check usages, as this currently is not read-only
	 *
	 * @param blockHeight The block height.
	 * @return The matching links.
	 */
	Iterator<AccountLink> getOutlinksIterator(BlockHeight blockHeight);

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
	 * Gets the page rank at the specified block height.
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
