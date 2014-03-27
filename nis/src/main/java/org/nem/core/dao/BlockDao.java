package org.nem.core.dao;

import org.nem.core.dbmodel.Block;

public interface BlockDao {
	public void save(Block block);

	public void updateLastBlockId(Block block);
	
	public Long count();

	public Block findById(long id);

	public Block findByHash(byte[] blockHash);

	public Block findByHeight(Long blockHeight);
}
