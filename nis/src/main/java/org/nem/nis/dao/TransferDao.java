package org.nem.nis.dao;

import java.util.List;

import org.nem.nis.model.Transfer;

public interface TransferDao {
	public void save(Transfer block);
	
	public Long count();

	public void saveMulti(List<Transfer> transfers);
}
