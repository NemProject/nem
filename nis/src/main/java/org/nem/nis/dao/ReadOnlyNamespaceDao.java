package org.nem.nis.dao;

import org.nem.core.model.Account;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.nis.dbmodel.DbNamespace;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbNamespace objects.
 */
public interface ReadOnlyNamespaceDao {

	/**
	 * Gets all namespaces that are owned by an account.
	 * Optionally a parent can be supplied, in that case only children of the parent are returned.
	 *
	 * @param account The account.
	 * @param parent The parent namespace id (optional).
	 * @param limit The maximum number of db namespaces to be returned.
	 * @return The collection of db namespaces.
	 */
	Collection<DbNamespace> getNamespacesForAccount(
			final Account account,
			final NamespaceId parent,
			final int limit);

	/**
	 * Gets the specified namespace.
	 *
	 * @param fullName The fully qualified namespace name.
	 * @return The specified namespace or null.
	 */
	DbNamespace getNamespace(final String fullName);

	/**
	 * Retrieves all root namespaces.
	 *
	 * @param limit The limit.
	 * @return The root namespaces.
	 */
	Collection<DbNamespace> getRootNamespaces(final int limit);
}
