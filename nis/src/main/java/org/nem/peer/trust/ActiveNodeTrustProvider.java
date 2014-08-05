package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;

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
	public ColumnVector computeTrust(final TrustContext context) {
		final ColumnVector result = this.trustProvider.computeTrust(context);
		final boolean[] activeArray = getActiveArray(context.getNodes(), context.getLocalNode());
		for (int i = 0; i < activeArray.length; ++i) {
			if (!activeArray[i])
				result.setAt(i, 0);
		}

		if (result.isZeroVector()) {
			// none of the active nodes have any trust, distribute the trust among untrusted active nodes
			for (int i = 0; i < activeArray.length; ++i) {
				if (activeArray[i])
					result.setAt(i, 1);
			}
		}

		result.normalize();
		return result;
	}

	final boolean[] getActiveArray(final Node[] nodes, final Node localNode) {
		final boolean[] result = new boolean[nodes.length];
		for (int i = 0; i < nodes.length; ++i) {
			final NodeStatus status = this.nodeCollection.getNodeStatus(nodes[i]);
			if (NodeStatus.ACTIVE == status && !nodes[i].equals(localNode))
				result[i] = true;
		}

		return result;
	}
}
