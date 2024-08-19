package org.nem.nis.dao.retrievers;

import org.hibernate.*;
import org.nem.core.model.mosaic.MosaicId;
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
	 * Gets the mosaic definition for the specified mosaic id.
	 *
	 * @param session The session.
	 * @param mosaicId The mosaic id.
	 * @return The db mosaic definition.
	 */
	public DbMosaicDefinition getMosaicDefinition(final Session session, final MosaicId mosaicId) {
		MustBe.notNull(mosaicId, "mosaic id");
		final String queryString = "SELECT m.* FROM mosaicDefinitions m " + "WHERE namespaceId = :namespaceId AND NAME = :name ORDER BY id DESC LIMIT 1";
		final Query query = session.createSQLQuery(queryString) // preserve-newline
				.addEntity(DbMosaicDefinition.class)
				.setParameter("namespaceId", mosaicId.getNamespaceId().toString())
				.setParameter("name", mosaicId.getName());

		return (DbMosaicDefinition) query.uniqueResult();
	}

	/**
	 * Gets all mosaic definitions for the specified account, optionally confined to a specified namespace. The search is limited by a given
	 * max id and returns at most limit mosaic definitions.
	 *
	 * @param session The session.
	 * @param accountId The account identifier.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForAccount(final Session session, final Long accountId,
			final NamespaceId namespaceId, final long maxId, final int limit) {
		MustBe.notNull(accountId, "account");
		return this.getMosaicDefinitions(session, accountId, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaic definitions for the specified namespace. The search is limited by a given max id and returns at most limit mosaic
	 * definitions.
	 *
	 * @param session The session.
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespace(final Session session, final NamespaceId namespaceId,
			final long maxId, final int limit) {
		MustBe.notNull(namespaceId, "namespace id");
		return this.getMosaicDefinitions(session, null, namespaceId, maxId, limit);
	}

	/**
	 * Gets all mosaic definitions. The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param session The session.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The mosaic definitions.
	 */
	public Collection<DbMosaicDefinition> getMosaicDefinitions(final Session session, final long maxId, final int limit) {
		return this.getMosaicDefinitions(session, null, null, maxId, limit);
	}

	// note that this will not scale well and might need to be optimized later depending on usage
	private Collection<DbMosaicDefinition> getMosaicDefinitions(final Session session, final Long accountId, final NamespaceId namespaceId,
			final long maxId, final int limit) {
		String queryString = "SELECT m.* FROM mosaicdefinitions m "
				+ "WHERE concat(namespaceid, ' * ', name) NOT IN (SELECT concat(namespaceid, ' * ', name) FROM mosaicdefinitions WHERE id > m.id) "
				+ "AND id < :maxId ";
		if (null != accountId) {
			queryString += String.format("AND creatorId = %d ", accountId);
		}

		if (null != namespaceId) {
			queryString += "AND namespaceId = :namespaceId ";
		}

		queryString += "ORDER BY id DESC LIMIT :limit";
		Query query = session.createSQLQuery(queryString) // preserve-newline
				.addEntity(DbMosaicDefinition.class) // preserve-newline
				.setParameter("maxId", maxId) // preserve-newline
				.setParameter("limit", limit);

		if (null != namespaceId) {
			query = query.setParameter("namespaceId", namespaceId.toString());
		}

		return HibernateUtils.listAndCast(query);
	}
}
