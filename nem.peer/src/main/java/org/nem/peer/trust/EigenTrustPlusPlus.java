package org.nem.peer.trust;

import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.peer.trust.score.*;

/**
 * EigenTrust++ algorithm implementation.
 */
public class EigenTrustPlusPlus extends EigenTrust {

	private final CredibilityScores credibilityScores = new CredibilityScores();

	/**
	 * Creates a new eigen trust plus plus object using the default score provider.
	 */
	public EigenTrustPlusPlus() {
		this(new ScoreProvider());
	}

	/**
	 * Creates a new eigen trust plus plus object using a custom score provider.
	 *
	 * @param scoreProvider The score provider to use.
	 */
	public EigenTrustPlusPlus(final org.nem.peer.trust.ScoreProvider scoreProvider) {
		super(scoreProvider);
	}

	/**
	 * Creates a new eigen trust plus plus object using a custom score provider.
	 *
	 * @param scoreProvider The score provider to use.
	 */
	private EigenTrustPlusPlus(final ScoreProvider scoreProvider) {
		super(scoreProvider);
		scoreProvider.setTrustScores(this.getTrustScores());
	}

	/**
	 * Gets the credibility scores.
	 *
	 * @return The credibility scores.
	 */
	public CredibilityScores getCredibilityScores() {
		return this.credibilityScores;
	}

	@Override
	public Matrix getTrustMatrix(final Node[] nodes) {
		final Matrix trustMatrix = this.getTrustScores().getScoreMatrix(nodes);
		final Matrix credibilityMatrix = this.credibilityScores.getScoreMatrix(nodes);
		final Matrix matrix = trustMatrix.multiplyElementWise(credibilityMatrix);
		matrix.normalizeColumns();
		return matrix;
	}

	/**
	 * Updates the feedback credibility values for the specified node given the specified nodes and experiences.
	 *
	 * @param node The node.
	 * @param nodes The nodes.
	 * @param nodeExperiences The node experiences.
	 */
	public void updateFeedback(final Node node, final Node[] nodes, final NodeExperiences nodeExperiences) {
		final Matrix sharedExperiencesMatrix = nodeExperiences.getSharedExperienceMatrix(node, nodes);

		final ColumnVector vector = new ColumnVector(nodes.length);
		for (int i = 0; i < nodes.length; ++i) {
			if (node.equals(nodes[i])) {
				// the node should completely trust itself
				vector.setAt(i, 1);
				continue;
			}

			double sum = 0.0;
			int numCommonPartners = 0;
			for (int j = 0; j < nodes.length; ++j) {
				if (0 == sharedExperiencesMatrix.getAt(i, j)) {
					continue;
				}

				final double score = this.getScoreProvider().calculateCredibilityScore(node, nodes[i], nodes[j]);
				sum += score * score;
				++numCommonPartners;
			}

			if (0 == numCommonPartners) {
				continue;
			}

			// Original paper suggests sim = 1 - Math.sqrt(sum).
			// This leads to values of around 0.5 for evil nodes and almost 1 for honest nodes.
			// We get better results by taking a power of that value since (0.5)^n quickly converges to 0 for increasing n.
			// The value n=4 is just an example which works well.
			sum /= numCommonPartners;
			vector.setAt(i, Math.pow(1 - Math.sqrt(sum), 4));
		}

		this.credibilityScores.setScoreVector(node, nodes, vector);
	}

	private void updateFeedback(final TrustContext context) {
		for (final Node node : context.getNodes()) {
			this.updateFeedback(node, context.getNodes(), context.getNodeExperiences());
		}
	}

	@Override
	public TrustResult computeTrust(final TrustContext context) {
		// (1) Compute the local trust values
		this.updateTrust(context);

		// (2) Compute the local feedback credibility values
		this.updateFeedback(context);

		// (3) Compute the global trust
		return new TrustResult(context, this.computeGlobalTrust(context));
	}

	/**
	 * An EigenTrust score provider implementation.
	 */
	public static class ScoreProvider implements org.nem.peer.trust.ScoreProvider {

		private TrustScores scores;

		/**
		 * Sets the trust scores associated with this provider.
		 *
		 * @param scores The trust scores associated with this provider.
		 */
		public void setTrustScores(final TrustScores scores) {
			this.scores = scores;
		}

		@Override
		public double calculateTrustScore(final NodeExperience experience) {
			return experience.successfulCalls().get();
		}

		@Override
		public double calculateCredibilityScore(final Node node1, final Node node2, final Node node3) {
			final TrustScores scores = this.scores;
			final double localSum1 = scores.getScoreWeight(node1).get();
			final double localScore1 = scores.getScore(node1, node3).score().get();
			final double localSum2 = scores.getScoreWeight(node2).get();
			final double localScore2 = scores.getScore(node2, node3).score().get();
			return localScore1 * localSum1 - localScore2 * localSum2;
		}
	}
}
