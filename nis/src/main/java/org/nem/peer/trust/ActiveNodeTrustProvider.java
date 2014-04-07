package org.nem.peer.trust;

import org.nem.core.math.Vector;
import org.nem.peer.Node;
import org.nem.peer.NodeCollection;
import org.nem.peer.NodeStatus;

/**
 * TrustProvider decorator that filters out the trust values of inactive and local nodes.
 */
public class ActiveNodeTrustProvider implements TrustProvider {

	private final TrustProvider trustProvider;
	private final NodeCollection nodeCollection;

	/**
	 * Creates a new active node trust provider
	 *
	 * @param trustProvider  The trust provider.
	 * @param nodeCollection The node collection.
	 */
	public ActiveNodeTrustProvider(final TrustProvider trustProvider, final NodeCollection nodeCollection) {
		this.trustProvider = trustProvider;
		this.nodeCollection = nodeCollection;
	}

	@Override
	public Vector computeTrust(final TrustContext context) {
		final Vector vector = this.trustProvider.computeTrust(context);
		final Node[] nodes = context.getNodes();
		for (int i = 0; i < nodes.length; ++i) {
			final NodeStatus status = this.nodeCollection.getNodeStatus(nodes[i]);
			if (NodeStatus.ACTIVE != status || nodes[i].equals(context.getLocalNode()))
				vector.setAt(i, 0);
		}

		return vector;
	}
}
