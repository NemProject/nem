package org.nem.nis.dao.retrievers;

import org.hibernate.*;
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

	// TODO 20150724 BR -> all: ugh, this will not scale well :/
	// > We should have a big additional fee for mosaic definition creation which goes to the namespace rental fee sink account.
	// TODO 20150831 J-B -> can't we improve perf by adding a lookup table that points to the latest mosaic entry?
	// TODO 20150810 BR -> J: I would leave it like it is for now and decide later.
	private Collection<DbMosaicDefinition> getMosaicDefinitions(
			final Session session,
			final Long accountId,
			final NamespaceId namespaceId,
			final long maxId,
			final int limit) {
		String queryString = "SELECT m.* FROM mosaicdefinitions m " +
				"WHERE concat(namespaceid, ' * ', name) NOT IN (SELECT concat(namespaceid, ' * ', name) FROM mosaicdefinitions WHERE id > m.id) " +
				"AND id < :maxId ";
		if (null != accountId) {
			queryString += String.format("AND creatorId = %d ", accountId);
		}

		if (null != namespaceId) {
			queryString += String.format("AND namespaceId = '%s' ", namespaceId.toString());
		}

		queryString += "ORDER BY id DESC LIMIT :limit";
		final Query query = session
				.createSQLQuery(queryString)
				.addEntity(DbMosaicDefinition.class)
				.setParameter("maxId", maxId)
				.setParameter("limit", limit);

		return HibernateUtils.listAndCast(query);
	}
}
