package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.MissingResourceException;

@Service
public class DbBlockIoAdapter implements BlockIo {
	private final ReadOnlyBlockDao blockDao;
	private final AccountLookup accountLookup;

	@Autowired(required = true)
	public DbBlockIoAdapter(final ReadOnlyBlockDao blockDao, final AccountLookup accountLookup) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
	}

	@Override
	public Block getBlock(final Hash blockHash) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(blockHash);
		if (null == dbBlock) {
			throw createMissingResourceException(blockHash.toString());
		}

		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}

	@Override
	public Block getBlockAt(final BlockHeight blockHeight) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		if (null == dbBlock) {
			throw createMissingResourceException(blockHeight.toString());
		}

		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
