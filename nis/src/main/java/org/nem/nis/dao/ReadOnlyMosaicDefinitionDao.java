package org.nem.nis.dao;

import java.util.Collection;
import org.nem.core.model.Address;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.*;

/**
 * Read-only DAO for accessing DbMosaicDefinition objects.
 */
public interface ReadOnlyMosaicDefinitionDao {
	/**
	 * Gets the mosaic definition for the specified mosaic id.
	 *
	 * @param mosaicId The mosaic id.
	 * @return The db mosaic definition.
	 */
	DbMosaicDefinition getMosaicDefinition(final MosaicId mosaicId);

	/**
	 * Gets a mosaic definition and its corresponding supply at a specified height.
	 *
	 * @param mosaicId The mosaic id.
	 * @param height The search height.
	 * @return The db mosaic definition and supply tuple, if found. \c null otherwise.
	 */
	DbMosaicDefinitionSupplyTuple getMosaicDefinitionWithSupply(final MosaicId mosaicId, final Long height);

	/**
	 * Gets all mosaic definitions for the specified account, optionally confined to a specified namespace. The search is limited by a given
	 * max id and returns at most limit mosaic definitions.
	 *
	 * @param address The account address.
	 * @param namespaceId The (optional) namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitionsForAccount(final Address address, final NamespaceId namespaceId, final Long maxId,
			final int limit);

	/**
	 * Gets all mosaic definitions for the specified namespace id. The search is limited by a given max id and returns at most limit mosaic
	 * definitions.
	 *
	 * @param namespaceId The namespace id.
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitionsForNamespace(final NamespaceId namespaceId, final Long maxId, final int limit);

	/**
	 * Gets all mosaic definitions. The search is limited by a given max id and returns at most limit mosaic definitions.
	 *
	 * @param maxId The id of "top-most" mosaic definition.
	 * @param limit The limit.
	 * @return The collection of db mosaic definitions.
	 */
	Collection<DbMosaicDefinition> getMosaicDefinitions(final Long maxId, final int limit);
}
