package org.nem.nis.dao;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.Block;

import java.util.List;

/**
 * DAO for accessing db Block objects.
 */
public interface BlockDao {
	/**
	 * Saves full block in the database, along with associated transactions, forgers, signers, etc.
	 *
	 * @param block
	 */
	public void save(Block block);

	/**
	 * Updates lastBlockId of this block using id of given block.
	 *
	 * @param block Block whose id will be used.
	 */
	public void updateLastBlockId(Block block);

	/**
	 * Returns number of blocks in the database.
	 *
	 * @return number of blocks in the database.
	 */
	public Long count();

	/**
	 * Retrieves Block from db given it's id in the database.
	 *
	 * @param id id of a block.
	 *
	 * @return associated Block or null if there's no block with such id.
	 */
	public Block findById(long id);

    /**
	 * Retrieves Block from db given it's hash.
	 *
	 * @param blockHash hash of a block to retrieve.
	 *
	 * @return Block having given hash or null.
	 */
	public Block findByHash(final Hash blockHash);

	/**
	 * Retrieves Block from db at given height.
	 *
	 * @param height height of a block to retrieve.
	 *
	 * @return Block at given height or null.
	 */
	public Block findByHeight(final BlockHeight height);

    /**
     * Retrieves list of at most limit hashes for blocks starting at given height.
     * This should be used, not to pull whole block from the db.
     *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return HashChain.
     */
    public HashChain getHashesFrom(final BlockHeight height, int limit);

	/**
	 * Retrieves list of at most limit difficulties for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's difficulties.
	 */
	public List<Long> getDifficultiesFrom(final BlockHeight height, int limit);


	/**
	 * Retrieves list of at most limit timestamps for blocks starting at given height.
	 *
	 * @param height height of a first block.
	 * @param limit maximal number of elements to return.
	 * @return list of block's timestamps.
	 */
	public List<Integer> getTimestampsFrom(final BlockHeight height, int limit);

	/**
	 * Deletes blocks after given block
	 *
	 * @param height
	 */
	public void deleteBlocksAfterHeight(final BlockHeight height);
}
