package org.nem.nis.time.synchronization;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.nis.poi.*;
import org.nem.peer.trust.*;

import java.security.SecureRandom;
import java.util.*;

// TODO 20140909 i would actually have this derive from basic node selector by adding a virtual function to basicnodeselector like isCandidate(Node)

public class ImportanceAwareNodeSelector implements NodeSelector {
	private final int maxNodes;
	private final PoiFacade poiFacade;
	private final TrustContext context;
	private final ColumnVector trustVector;
	private final Random random;

	/**
	 * Creates a new importance aware node selector.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poiFacade The POI facade containing all importance information.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 */
	public ImportanceAwareNodeSelector(
			final int maxNodes,
			final PoiFacade poiFacade,
			final TrustProvider trustProvider,
			final TrustContext context) {
		this(maxNodes, poiFacade, trustProvider, context, new SecureRandom());
	}

	/**
	 * Creates a new new importance aware node selector using a custom random number generator.
	 *
	 * @param maxNodes The maximum number of nodes that should be returned from selectNodes.
	 * @param poiFacade The POI facade containing all importance information.
	 * @param trustProvider The trust provider.
	 * @param context The trust context.
	 * @param random The random number generator.
	 */
	public ImportanceAwareNodeSelector(
			final int maxNodes,
			final PoiFacade poiFacade,
			final TrustProvider trustProvider,
			final TrustContext context,
			final Random random) {
		this.maxNodes = maxNodes;
		this.poiFacade = poiFacade;
		this.context = context;
		this.trustVector = trustProvider.computeTrust(context);
		this.trustVector.normalize();
		this.random = random;
	}

	@Override
	public Node selectNode() {
		final List<Node> nodes = this.selectNodes(1);
		return !nodes.isEmpty() ? nodes.get(0) : null;
	}

	@Override
	public List<Node> selectNodes() {
		return this.selectNodes(this.maxNodes);
	}

	private List<Node> selectNodes(final int maxNodes) {
		final Node[] nodes = this.context.getNodes();
		final boolean[] usedNodes = new boolean[nodes.length];
		final List<Node> partnerNodes = new ArrayList<>();

		int numSelectedNodes;
		double remainingTrust = 1.0;
		do {
			numSelectedNodes = partnerNodes.size();

			double sum = 0;
			final double rand = this.random.nextDouble() * remainingTrust;
			for (int i = 0; i < nodes.length; ++i) {
				// skip nodes with zero trust, insufficient importance and those that have already been used
				final double trust = this.trustVector.getAt(i);
				final PoiAccountState accountState = this.poiFacade.findStateByAddress(nodes[i].getIdentity().getAddress());
				if (!this.poiFacade.getLastPoiRecalculationHeight().equals(accountState.getImportanceInfo().getHeight())) {
					continue;
				}
				final double importance = accountState.getImportanceInfo().getImportance(this.poiFacade.getLastPoiRecalculationHeight());
				if (0 == trust || TimeSynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE > importance || usedNodes[i]) {
					continue;
				}

				sum += trust;
				if (sum < rand) {
					continue;
				}

				usedNodes[i] = true;
				remainingTrust -= trust;
				partnerNodes.add(nodes[i]);
				break;
			}

			// stop the loop if either maxNodes have been selected or the last iteration didn't select a node
			// TODO BR: if we are unlucky, no new nodes were selected although there still are good nodes.
			// TODO     In case of the BasicNodeSelector this is ok, but probably not for this one.
		} while (partnerNodes.size() != maxNodes && partnerNodes.size() != numSelectedNodes);

		return partnerNodes;
	}
}
