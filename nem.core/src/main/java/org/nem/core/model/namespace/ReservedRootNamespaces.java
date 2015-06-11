package org.nem.core.model.namespace;

import java.util.*;

/**
 * Class holding all root namespaces that users are not allowed to claim.
 */
public class ReservedRootNamespaces {
	final static String[] reservedRoots = { "nem", "user", "account", "org", "com", "biz", "net", "edu", "mil", "gov", "info" };

	private static final Set<NamespaceId> NAMESPACE_IDS = new HashSet<NamespaceId>() {
		{
			Arrays.stream(reservedRoots).forEach(r -> this.add(new NamespaceId(r)));
		}
	};

	/**
	 * Gets a value indicating whether or not the given namespace id is claimable.
	 *
	 * @param namespaceId The namespace id.
	 * @return true if the namespace id is reserved (not publicly available), false otherwise.
	 */
	public static boolean contains(final NamespaceId namespaceId) {
		return NAMESPACE_IDS.contains(namespaceId);
	}

	/**
	 * Gets the collection of all reserved namespace ids.
	 *
	 * @return The collection of namespace ids.
	 */
	public static Set<NamespaceId> getAll() {
		return Collections.unmodifiableSet(NAMESPACE_IDS);
	}
}
