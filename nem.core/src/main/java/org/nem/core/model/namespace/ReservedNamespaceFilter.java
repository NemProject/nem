package org.nem.core.model.namespace;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that filters namespaces that users are not allowed to claim.
 */
public class ReservedNamespaceFilter {
	private static final String[] RESERVED_ROOTS = { "nem", "user", "account", "org", "com", "biz", "net", "edu", "mil", "gov", "info" };
	private static final Set<NamespaceIdPart> NAMESPACE_ID_PARTS = Arrays.stream(RESERVED_ROOTS).map(NamespaceIdPart::new).collect(Collectors.toSet());

	/**
	 * Gets a value indicating whether or not the given namespace id is claimable.
	 *
	 * @param namespaceId The namespace id.
	 * @return true if the namespace id is claimable, false otherwise.
	 */
	public static boolean isClaimable(final NamespaceId namespaceId) {
		NamespaceId current = namespaceId;
		do {
			if (NAMESPACE_ID_PARTS.contains(current.getLastPart())) {
				return false;
			}

			current = current.getParent();
		} while (null != current);

		return true;
	}

	/**
	 * Gets the collection of all reserved namespace id parts.
	 *
	 * @return The collection of namespace id parts.
	 */
	public static Set<NamespaceIdPart> getAll() {
		return Collections.unmodifiableSet(NAMESPACE_ID_PARTS);
	}
}
