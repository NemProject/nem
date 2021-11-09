package org.nem.nis.dao;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.DbBlock;

import java.util.Collection;

/**
 * DAO for accessing DbBlock objects.
 */
public interface BlockDao extends ReadOnlyBlockDao {

	/**
	 * Saves full block in the database, along with associated transactions, signers, etc.
	 *
	 * @param block DbBlock to save.
	 */
	void save(DbBlock block);

	/**
	 * Saves all blocks in the database, along with associated transactions, signers, etc.
	 *
	 * @param blocks Blocks to save.
	 */
	void save(final Collection<DbBlock> blocks);

	/**
	 * Deletes blocks after given block.
	 *
	 * @param height The height of the reference block.
	 */
	void deleteBlocksAfterHeight(final BlockHeight height);
}
