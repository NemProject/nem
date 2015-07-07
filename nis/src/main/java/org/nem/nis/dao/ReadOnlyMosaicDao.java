package org.nem.nis.dao;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.DbMosaic;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbMosaic objects.
 */
public interface ReadOnlyMosaicDao {
	/**
	 * Gets all mosaics for the specified account, optionally confined to a specified namespace.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param account The account.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The collection of db mosaics.
	 */
	Collection<DbMosaic> getMosaicsForAccount(
			final Account account,
			final NamespaceId namespaceId,
			final Long maxId,
			final int limit);

	/**
	 * Gets all mosaics for the specified namespace id.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The collection of db mosaics.
	 */
	Collection<DbMosaic> getMosaicsForNamespace(
			final NamespaceId namespaceId,
			final Long maxId,
			final int limit);

	/**
	 * Gets all mosaics.
	 * The search is limited by a given max id and returns at most limit mosaics.
	 *
	 * @param maxId The id of "top-most" mosaic.
	 * @param limit The limit.
	 * @return The collection of db mosaics.
	 */
	Collection<DbMosaic> getMosaics(
			final Long maxId,
			final int limit);
}
