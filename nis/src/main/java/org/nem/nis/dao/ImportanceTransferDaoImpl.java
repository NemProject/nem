package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.utils.ByteUtils;
import org.nem.nis.dbmodel.ImportanceTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class ImportanceTransferDaoImpl implements ImportanceTransferDao {

	private final SessionFactory sessionFactory;

	@Autowired(required = true)
	public ImportanceTransferDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public Long count() {
		return (Long)this.getCurrentSession().createQuery("select count (*) from ImportanceTransfer").uniqueResult();
	}

	@Override
	@Transactional
	public void save(final ImportanceTransfer entity) {
		this.getCurrentSession().saveOrUpdate(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public ImportanceTransfer findByHash(final byte[] txHash) {
		final long txId = ByteUtils.bytesToLong(txHash);
		final Query query = this.getCurrentSession()
				.createQuery("from ImportanceTransfer a where a.shortId = :id")
				.setParameter("id", txId);
		return getByHashQuery(txHash, query);
	}

	@Override
	@Transactional(readOnly = true)
	public ImportanceTransfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		final long txId = ByteUtils.bytesToLong(txHash);
		final Query query = this.getCurrentSession()
				.createQuery("from ImportanceTransfer t where t.shortId = :id and t.block.height <= :height")
				.setParameter("id", txId)
				.setParameter("height", maxBlockHeight);
		return getByHashQuery(txHash, query);
	}

	private ImportanceTransfer getByHashQuery(final byte[] txHash, final Query query) {
		final List<?> userList = query.list();
		for (final Object transferObject : userList) {
			final ImportanceTransfer transfer = (ImportanceTransfer)transferObject;
			if (Arrays.equals(txHash, transfer.getTransferHash().getRaw())) {
				return transfer;
			}
		}
		return null;
	}
}
