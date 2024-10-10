package org.nem.peer.trust;

import java.util.List;
import org.nem.core.node.Node;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

	/**
	 * Selects a single node.
	 *
	 * @return The node.
	 */
	Node selectNode();

	/**
	 * Selects multiple nodes.
	 *
	 * @return The nodes.
	 */
	List<Node> selectNodes();
}
