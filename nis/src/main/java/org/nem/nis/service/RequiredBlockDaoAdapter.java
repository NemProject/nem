package org.nem.nis.service;

import org.nem.core.model.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.MissingResourceException;

/**
 * Adapter around BlockDao that throws a MissingResourceException if a requested
 * Block is not found.
 */
@Service
public class RequiredBlockDaoAdapter implements RequiredBlockDao {

	private final BlockDao blockDao;

	@Autowired(required = true)
	public RequiredBlockDaoAdapter(final BlockDao blockDao) {
		this.blockDao = blockDao;
	}

	/**
	 * Retrieves Block from db given its id in the database.
	 *
	 * @param id id of a block.
	 * @return Block with the specified id.
	 * @throws MissingResourceException If a matching block cannot be found.
	 */
	public org.nem.nis.dbmodel.Block findById(long id) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findById(id);
		if (null == dbBlock)
			throw createMissingResourceException(Long.toBinaryString(id));

		return dbBlock;
	}

	/**
	 * Retrieves Block from db given its hash.
	 *
	 * @param blockHash hash of a block to retrieve.
	 * @return Block having given hash.
	 * @throws MissingResourceException If a matching block cannot be found.
	 */
	public org.nem.nis.dbmodel.Block findByHash(final Hash blockHash) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(blockHash);
		if (null == dbBlock)
			throw createMissingResourceException(blockHash.toString());

		return dbBlock;
	}

	/**
	 * Retrieves Block from db at given height.
	 *
	 * @param height Height of the block to retrieve.
	 * @return Block at given height.
	 * @throws MissingResourceException If a matching block cannot be found.
	 */
	public org.nem.nis.dbmodel.Block findByHeight(final BlockHeight height) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		if (null == dbBlock)
			throw createMissingResourceException(height.toString());

		return dbBlock;
	}

	/**
	 * Retrieves list of hashes for blocks starting at given height.
	 * This should be used, not to pull whole block from the db.
	 *
	 * @param height height of starting block.
	 * @return Chain of block hashes.
	 */
	public HashChain getHashesFrom(final BlockHeight height, int limit) {
		// TODO: throw exception?
		return this.blockDao.getHashesFrom(height, limit);
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
