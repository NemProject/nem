package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.utils.MustBe;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.DbMosaicDefinition;

import java.util.Collection;

/**
 * Class for for retrieving mosaic definitions.
 */
public class MosaicDefinitionRetriever {

	/**
	 * Gets all mosaic definitions for the specified account, optionally confined to a specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param session The session.
	 * @param accountId The account identifier.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForAccount(
			final Session session,
			final Long accountId,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		MustBe.notNull(accountId, "account");
		return this.getMosaicDefinitions(session, accountId, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaic definitions for the specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param session The session.
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespace(
			final Session session,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		MustBe.notNull(namespaceId, "namespace id");
		return this.getMosaicDefinitions(session, null, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaic definitions.
	 * The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param session The session.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitions(
			final Session session,
			final long maxId,
			final int limit) {
		return this.getMosaicDefinitions(session, null, null, maxId, limit);
	}

	private Collection<DbMosaicDefinition> getMosaicDefinitions(
			final Session session,
			final Long accountId,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		final Criteria criteria = session.createCriteria(DbMosaicDefinition.class)
				.add(Restrictions.le("id", maxId))
				.addOrder(Order.desc("id"))
				.setMaxResults(limit);
		if (null != accountId) {
			criteria.add(Restrictions.eq("creator.id", accountId));
		}

		if (null != namespaceId) {
			criteria.add(Restrictions.eq("namespaceId", namespaceId.toString()));
		}

		return HibernateUtils.listAndCast(criteria);
	}
}
