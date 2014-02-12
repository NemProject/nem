package org.nem.nis.dao;

import org.nem.nis.model.Block;

public interface BlockDao {
	public void save(Block block);
	
	public Long count();

	public Block findByShortId(long shortId);
}
