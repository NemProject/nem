package org.nem.peer.node;

import org.nem.core.node.NodeMetaData;

/**
 * An interface for checking the compatibility of two nodes using their metadata.
 */
@FunctionalInterface
public interface NodeCompatibilityChecker {

	/**
	 * Checks the local and remote nodes for compatibility.
	 *
	 * @param local The local metadata
	 * @param remote The remote metadata.
	 * @return true if the nodes are compatible.
	 */
	boolean check(final NodeMetaData local, final NodeMetaData remote);
}
