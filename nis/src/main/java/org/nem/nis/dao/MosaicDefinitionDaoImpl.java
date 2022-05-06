package org.nem.nis.dao;

import org.hibernate.*;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dao.retrievers.MosaicDefinitionRetriever;
import org.nem.nis.dbmodel.DbMosaicDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class MosaicDefinitionDaoImpl implements ReadOnlyMosaicDefinitionDao {
	private final SessionFactory sessionFactory;
	private final MosaicDefinitionRetriever retriever;

	/**
	 * Creates a mosaic definition dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 */
	@Autowired(required = true)
	public MosaicDefinitionDaoImpl(final SessionFactory sessionFactory) {
		this(sessionFactory, new MosaicDefinitionRetriever());
	}

	/**
	 * Creates a mosaic definition dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 * @param retriever The mosaic retriever.
	 */
	public MosaicDefinitionDaoImpl(final SessionFactory sessionFactory, final MosaicDefinitionRetriever retriever) {
		this.sessionFactory = sessionFactory;
		this.retriever = retriever;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	@Override
	@Transactional(readOnly = true)
	public DbMosaicDefinition getMosaicDefinition(final MosaicId mosaicId) {
		return this.retriever.getMosaicDefinition(this.getCurrentSession(), mosaicId);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForAccount(final Address address, final NamespaceId namespaceId,
			final Long maxId, final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		final Long accountId = DaoUtils.getAccountId(this.getCurrentSession(), address);
		if (null == accountId) {
			return Collections.emptyList();
		}

		return this.retriever.getMosaicDefinitionsForAccount(this.getCurrentSession(), accountId, namespaceId, id, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespace(final NamespaceId namespaceId, final Long maxId,
			final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		return this.retriever.getMosaicDefinitionsForNamespace(this.getCurrentSession(), namespaceId, id, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<DbMosaicDefinition> getMosaicDefinitions(final Long maxId, final int limit) {
		final long id = null == maxId ? Long.MAX_VALUE : maxId;
		return this.retriever.getMosaicDefinitions(this.getCurrentSession(), id, limit);
	}
}
