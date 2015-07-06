package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.hibernate.criterion.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.utils.MustBe;
import org.nem.nis.dao.HibernateUtils;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * Class for for retrieving mosaics.
 * TODO 20150705 J-B: needs tests (after child mosaic resolution)
 */
public class MosaicRetriever {

	/**
	 * Gets all mosaics for the specified account, optionally confined to a specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param session The session.
	 * @param accountId The account identifier.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaics.
	 */
	public Collection<DbMosaic> getMosaicsForAccount(
			final Session session,
			final Long accountId,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		MustBe.notNull(accountId, "account");
		return this.getMosaics(session, accountId, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaics for the specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param session The session.
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaics.
	 */
	public Collection<DbMosaic> getMosaicsForNamespace(
			final Session session,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		MustBe.notNull(namespaceId, "namespace id");
		return this.getMosaics(session, null, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaics.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param session The session.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaics.
	 */
	public Collection<DbMosaic> getMosaics(
			final Session session,
			final long maxId,
			final int limit) {
		return this.getMosaics(session, null, null, maxId, limit);
	}

	private Collection<DbMosaic> getMosaics(
			final Session session,
			final Long accountId,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		final Criteria criteria = session.createCriteria(DbMosaic.class)
				.add(Restrictions.le("id", maxId))
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
