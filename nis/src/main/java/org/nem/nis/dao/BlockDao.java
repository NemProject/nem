package org.nem.nis.dao;

import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbBlock;

import java.util.List;

/**
 * DAO for accessing DbBlock objects.
 */
public interface BlockDao extends ReadOnlyBlockDao {

	/**
	 * Saves full block in the database, along with associated transactions, signers, etc.
	 *
	 * @param block DbBlock to save.
	 */
	public void save(DbBlock block);

	/**
	 * Saves all blocks in the database, along with associated transactions, signers, etc.
	 * TODO 20141206 J-G: we should use this
	 * TODO 20150209 BR -> J: we used it in the multisig integration test but for me it was slower than saving the blocks one by one.
	 *
	 * @param blocks Blocks to save.
	 */
	public void save(final List<DbBlock> blocks);

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
	public List<TimeInstant> getTimeStampsFrom(final BlockHeight height, int limit);

	/**
	 * Deletes blocks after given block.
	 *
	 * @param height The height of the reference block.
	 */
	public void deleteBlocksAfterHeight(final BlockHeight height);
}
