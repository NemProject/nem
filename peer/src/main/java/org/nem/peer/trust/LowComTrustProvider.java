package org.nem.peer.trust;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.Node;
import org.nem.peer.trust.score.NodeExperience;

/**
 * TrustProvider decorator that boosts the trust values of nodes that have low communication.
 */
public class LowComTrustProvider implements TrustProvider {

	private static final int MIN_COMMUNICATION = 10;

	private final int weight;
	private final TrustProvider trustProvider;

	/**
	 * Creates a new low communication trust provider.
	 *
	 * @param trustProvider The trust provider.
	 * @param weight The desired percentage boost for choosing a low communication node.
	 */
	public LowComTrustProvider(final TrustProvider trustProvider, final int weight) {
		this.trustProvider = trustProvider;
		this.weight = weight;
	}

	@Override
	public TrustResult computeTrust(final TrustContext context) {
		final TrustResult result = this.trustProvider.computeTrust(context);

		final ColumnVector lowComVector = computeLowComVector(result.getTrustContext());
		final double lowComVectorSum = lowComVector.sum();

		ColumnVector trustVector = result.getTrustValues();
		if (0 != lowComVectorSum && 0 != this.weight) {
			trustVector.normalize();
			lowComVector.scale(lowComVectorSum / this.weight * 100.0);
			trustVector = trustVector.addElementWise(lowComVector);
		}

		trustVector.normalize();
		return new TrustResult(result.getTrustContext(), trustVector);
	}

	private static ColumnVector computeLowComVector(final TrustContext context) {
		final Node localNode = context.getLocalNode();
		final Node[] nodes = context.getNodes();

		final ColumnVector lowComVector = new ColumnVector(nodes.length);
		for (int i = 0; i < nodes.length; ++i) {
			final NodeExperience experience = context.getNodeExperiences().getNodeExperience(localNode, nodes[i]);
			if (experience.totalCalls() >= MIN_COMMUNICATION) {
				continue;
			}

			lowComVector.setAt(i, 1);
		}

		return lowComVector;
	}
}
