package org.nem.core.dao;

import java.util.List;

import org.nem.core.dbmodel.Transfer;

public interface TransferDao {
	public void save(Transfer block);
	
	public Long count();

	public void saveMulti(List<Transfer> transfers);
}
