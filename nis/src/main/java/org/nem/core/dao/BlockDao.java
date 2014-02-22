package org.nem.core.dao;

import org.nem.core.dbmodel.Block;

public interface BlockDao {
	public void save(Block block);
	
	public Long count();

	public Block findByShortId(long shortId);
}
