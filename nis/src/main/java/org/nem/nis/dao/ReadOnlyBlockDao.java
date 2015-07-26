package org.nem.nis.dao;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbBlock;

import java.util.*;

/**
 * Read-only DAO for accessing DbBlock objects.
 */
public interface ReadOnlyBlockDao {

	/**
	 * Returns number of blocks in the database.
	 *
	 * @return number of blocks in the database.
	 */
	Long count();

	/**
	 * Retrieves DbBlock from db at given height.
	 *
	 * @param height height of a block to retrieve.
	 * @return DbBlock at given height or null.
	 */
	DbBlock findByHeight(final BlockHeight height);

	/**
	 * Retrieves list of at most limit hashes for blocks starting at given height.
	 * This should be used, not to pull whole block from the db.
	 *
	 * @param height height of a first block.
	 * @param limit maximum number of hashes to return.
	 * @return The hash chain.
	 */
	HashChain getHashesFrom(final BlockHeight height, int limit);

	/**
	 * Retrieves all Blocks from the database that were harvested by the specified account.
	 *
	 * @param account The account.
	 * @param id The id of "top-most" block.
	 * @param limit The maximum number of blocks to return.
	 * @return The blocks.
	 */
	Collection<DbBlock> getBlocksForAccount(final Account account, final Long id, int limit);

	// TODO 20150726 J-B: might be cleaner to have a separate getBlocksAfterAndUpdateCache(...)
	/**
	 * Gets at most blocksCount blocks after blockHeight.
	 *
	 * @param height The height of the block before the first desired block.
	 * @param limit The maximum number of blocks to return.
	 * @param updateCache value indicating whether the mosaic id cache should be updated or not.
	 * @return The blocks.
	 */
	Collection<DbBlock> getBlocksAfter(final BlockHeight height, int limit, final boolean updateCache);

	/**
	 * Retrieves list of at most limit difficulties for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's difficulties.
	 */
	List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, int limit);

	/**
	 * Retrieves list of at most limit timestamps for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's timestamps.
	 */
	List<TimeInstant> getTimeStampsFrom(final BlockHeight height, int limit);
}