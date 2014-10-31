package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class ImportanceTransferDaoImpl implements ImportanceTransferDao {
	private final SimpleTransferDaoImpl<ImportanceTransfer> impl;

	@Autowired(required = true)
	public ImportanceTransferDaoImpl(final SessionFactory sessionFactory) {
		this.impl = new SimpleTransferDaoImpl<>("ImportanceTransfer", sessionFactory, ImportanceTransfer::getTransferHash);
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return this.impl.count();
	}

	@Override
	@Transactional(readOnly = true)
	public ImportanceTransfer findByHash(final byte[] txHash) {
		return this.impl.findByHash(txHash);
	}

	@Override
	@Transactional(readOnly = true)
	public ImportanceTransfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		return this.impl.findByHash(txHash, maxBlockHeight);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean anyHashExists(final Collection<Hash> hashes, final BlockHeight maxBlockHeight) {
		return this.impl.anyHashExists(hashes, maxBlockHeight);
	}

	@Override
	@Transactional
	public void save(final ImportanceTransfer entity) {
		this.impl.save(entity);
	}

	@Override
	@Transactional
	public void saveMulti(final List<ImportanceTransfer> transfers) {
		this.impl.saveMulti(transfers);
	}
}