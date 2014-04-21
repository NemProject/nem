package org.nem.nis.dao;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;

import java.util.List;

/**
 * DAO for accessing db Block objects.
 */
public interface BlockDao extends ReadOnlyBlockDao {

	/**
	 * Returns number of blocks in the database.
	 *
	 * @return number of blocks in the database.
	 */
	public Long count();

	/**
	 * Saves full block in the database, along with associated transactions, forgers, signers, etc.
	 *
	 * @param block Block to save.
	 */
	public void save(Block block);

	/**
	 * Updates lastBlockId of this block using id of given block.
	 *
	 * @param block Block whose id will be used.
	 */
	public void updateLastBlockId(Block block);

	/**
	 * Retrieves list of at most limit difficulties for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's difficulties.
	 */
	public List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, int limit);


	/**
	 * Retrieves list of at most limit timestamps for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's timestamps.
	 */
	public List<TimeInstant> getTimestampsFrom(final BlockHeight height, int limit);

	/**
	 * Deletes blocks after given block.
	 *
	 * @param height The height of the reference block.
	 */
	public void deleteBlocksAfterHeight(final BlockHeight height);
}
