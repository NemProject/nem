package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.nis.dao.BlockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
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

	@Override
	public Long count() {
		return this.blockDao.count();
	}

	@Override
	public org.nem.nis.dbmodel.Block findById(long id) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findById(id);
		if (null == dbBlock)
			throw createMissingResourceException(Long.toBinaryString(id));

		return dbBlock;
	}

	@Override
	public org.nem.nis.dbmodel.Block findByHash(final Hash blockHash) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(blockHash);
		if (null == dbBlock)
			throw createMissingResourceException(blockHash.toString());

		return dbBlock;
	}

	@Override
	public org.nem.nis.dbmodel.Block findByHeight(final BlockHeight height) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(height);
		if (null == dbBlock)
			throw createMissingResourceException(height.toString());

		return dbBlock;
	}

	@Override
	public HashChain getHashesFrom(final BlockHeight height, int limit) {
		// TODO: throw exception?
		return this.blockDao.getHashesFrom(height, limit);
	}

	@Override
	public Collection<org.nem.nis.dbmodel.Block> getBlocksForAccount(final Account account, final Integer timestamp, int limit) {
		return this.blockDao.getBlocksForAccount(account, timestamp, limit);
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
