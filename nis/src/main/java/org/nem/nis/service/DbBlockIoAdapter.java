package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbBlockIoAdapter implements BlockIo {
	private final RequiredBlockDao blockDao;
	private final AccountLookup accountLookup;

	@Autowired(required = true)
	public DbBlockIoAdapter(final RequiredBlockDao blockDao, final AccountLookup accountLookup) {
		this.blockDao = blockDao;
		this.accountLookup = accountLookup;
	}

	@Override
	public Block getBlock(Hash blockHash) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHash(blockHash);
		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}

	@Override
	public Block getBlockAt(BlockHeight blockHeight) {
		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		return BlockMapper.toModel(dbBlock, this.accountLookup);
	}
}
