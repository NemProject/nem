package org.nem.nis.dao;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.Block;

import java.util.Collection;

/**
 * Read-only DAO for accessing db Block objects.
 */
public interface ReadOnlyBlockDao {

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
	 * @return The hash chain.
	 */
	public HashChain getHashesFrom(final BlockHeight height, int limit);

	/**
	 * Retrieves all Blocks from the database that were forged by the specified account.
	 *
	 * @param account The account.
	 * @param limit The maximum number of blocks to return.
	 * @return The blocks.
	 */
	public Collection<Block> getBlocksForAccount(final Account account, int limit);
}