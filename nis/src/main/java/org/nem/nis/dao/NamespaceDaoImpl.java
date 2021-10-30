package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.retrievers.NamespaceRetriever;
import org.nem.nis.dbmodel.DbNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class NamespaceDaoImpl implements ReadOnlyNamespaceDao {
	private final SessionFactory sessionFactory;
	private final NamespaceRetriever retriever;

	/**
	 * Creates a namespace dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 */
	@Autowired(required = true)
	public NamespaceDaoImpl(final SessionFactory sessionFactory) {
		this(sessionFactory, new NamespaceRetriever());
	}

	/**
	 * Creates a namespace dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 * @param retriever The namespace retriever.
	 */
	public NamespaceDaoImpl(final SessionFactory sessionFactory, final NamespaceRetriever retriever) {
		this.sessionFactory = sessionFactory;
		this.retriever = retriever;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbNamespace> getNamespacesForAccount(final Address address, final NamespaceId parent, final int limit) {
		final Long accountId = DaoUtils.getAccountId(this.getCurrentSession(), address);
		if (null == accountId) {
			return Collections.emptyList();
		}

		return this.retriever.getNamespacesForAccount(this.getCurrentSession(), accountId, parent, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public DbNamespace getNamespace(final NamespaceId id) {
		return this.retriever.getNamespace(this.getCurrentSession(), id);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbNamespace> getRootNamespaces(final Long maxId, final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		return this.retriever.getRootNamespaces(this.getCurrentSession(), id, limit);
	}
}
