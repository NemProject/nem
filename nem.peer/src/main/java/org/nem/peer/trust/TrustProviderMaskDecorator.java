package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;

import java.util.function.Predicate;

/**
 * TrustProvider decorator that filters out the trust values of inactive and local nodes.
 */
public class TrustProviderMaskDecorator implements TrustProvider {
	private final TrustProvider trustProvider;
	private final NodeCollection nodeCollection;
	private final Predicate<PredicateContext> includePredicate;

	/**
	 * Creates a new trust provider mask decorator.
	 *
	 * @param trustProvider The trust provider.
	 * @param nodeCollection The node collection.
	 */
	public TrustProviderMaskDecorator(final TrustProvider trustProvider, final NodeCollection nodeCollection) {
		this(trustProvider, nodeCollection, pc -> NodeStatus.ACTIVE == pc.getNodeStatus() && !pc.isLocalNode());
	}

	/**
	 * Creates a new trust provider mask decorator with a custom include predicate.
	 *
	 * @param trustProvider The trust provider.
	 * @param nodeCollection The node collection.
	 * @param includePredicate A predicate that should return true to include a specified node.
	 */
	public TrustProviderMaskDecorator(final TrustProvider trustProvider, final NodeCollection nodeCollection,
			final Predicate<PredicateContext> includePredicate) {
		this.trustProvider = trustProvider;
		this.nodeCollection = nodeCollection;
		this.includePredicate = includePredicate;
	}

	@Override
	public TrustResult computeTrust(final TrustContext context) {
		final TrustResult result = this.trustProvider.computeTrust(context);
		final ColumnVector trustValues = result.getTrustValues();

		final boolean[] filteredArray = this.getFilteredArray(result.getTrustContext());
		for (int i = 0; i < filteredArray.length; ++i) {
			if (!filteredArray[i]) {
				trustValues.setAt(i, 0);
			}
		}

		if (trustValues.isZeroVector()) {
			// none of the filtered nodes have any trust, distribute the trust among untrusted filtered nodes
			for (int i = 0; i < filteredArray.length; ++i) {
				if (filteredArray[i]) {
					trustValues.setAt(i, 1);
				}
			}
		}

		trustValues.normalize();
		return result;
	}

	private boolean[] getFilteredArray(final TrustContext context) {
		return this.getFilteredArray(context.getNodes(), context.getLocalNode());
	}

	private boolean[] getFilteredArray(final Node[] nodes, final Node localNode) {
		final boolean[] result = new boolean[nodes.length];
		for (int i = 0; i < nodes.length; ++i) {
			final PredicateContext pc = new PredicateContext(nodes[i], this.nodeCollection.getNodeStatus(nodes[i]), localNode);

			if (this.includePredicate.test(pc)) {
				result[i] = true;
			}
		}

		return result;
	}

	// region PredicateContext

	/**
	 * The context passed to the include predicate.
	 */
	public final class PredicateContext {
		private final Node node;
		private final NodeStatus status;
		private final boolean isLocalNode;

		private PredicateContext(final Node node, final NodeStatus status, final Node localNode) {
			this.node = node;
			this.status = status;
			this.isLocalNode = this.node.equals(localNode);
		}

		/**
		 * Gets the node being evaluated.
		 *
		 * @return The node.
		 */
		public Node getNode() {
			return this.node;
		}

		/**
		 * Gets the status of the node being evaluated.
		 *
		 * @return The node status.
		 */
		public NodeStatus getNodeStatus() {
			return this.status;
		}

		/**
		 * Gets a value indicating whether or not the node being evaluated is the local node.
		 *
		 * @return The node status.
		 */
		public boolean isLocalNode() {
			return this.isLocalNode;
		}
	}

	// endregion
}
