package org.nem.nis.dao;

import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;

import java.util.List;

/**
 * DAO for accessing db Block objects.
 */
public interface BlockDao extends ReadOnlyBlockDao {

	/**
	 * Saves full block in the database, along with associated transactions, forgers, signers, etc.
	 *
	 * @param block Block to save.
	 */
	public void save(Block block);

	/**
	 * Retrieves list of at most limit difficulties for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's difficulties.
	 */
	public List<BlockDifficulty> getDifficultiesFrom(final BlockHeight height, Integer limit);

	/**
	 * Retrieves list of at most limit timestamps for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's timestamps.
	 */
	public List<TimeInstant> getTimeStampsFrom(final BlockHeight height, Integer limit);

	/**
	 * Deletes blocks after given block.
	 *
	 * @param height The height of the reference block.
	 */
	public void deleteBlocksAfterHeight(final BlockHeight height);
}
