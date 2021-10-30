package org.nem.nis.dao;

import org.nem.core.model.Address;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.DbNamespace;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbNamespace objects.
 */
public interface ReadOnlyNamespaceDao {

	/**
	 * Gets all namespaces that are owned by an account. Optionally a parent can be supplied, in that case only children of the parent are
	 * returned.
	 *
	 * @param address The account address.
	 * @param parent The parent namespace id (optional).
	 * @param limit The maximum number of db namespaces to be returned.
	 * @return The collection of db namespaces.
	 */
	Collection<DbNamespace> getNamespacesForAccount(final Address address, final NamespaceId parent, final int limit);

	/**
	 * Gets the specified namespace.
	 *
	 * @param id The namespace id.
	 * @return The specified namespace or null.
	 */
	DbNamespace getNamespace(final NamespaceId id);

	/**
	 * Retrieves all root namespaces.
	 *
	 * @param maxId The (optional) id of "top-most" namespace.
	 * @param limit The limit.
	 * @return The root namespaces.
	 */
	Collection<DbNamespace> getRootNamespaces(final Long maxId, final int limit);
}
