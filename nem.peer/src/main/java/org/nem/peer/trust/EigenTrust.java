package org.nem.peer.trust;

import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.peer.trust.score.*;

/**
 * EigenTrust algorithm implementation.
 */
public class EigenTrust implements TrustProvider {

	private final TrustScores trustScores;
	private final org.nem.peer.trust.ScoreProvider scoreProvider;

	private int numComputations;
	private int numConvergences;

	/**
	 * Creates a new eigen trust object using the default score provider.
	 */
	public EigenTrust() {
		this(new ScoreProvider());
	}

	/**
	 * Creates a new eigen trust object using a custom score provider.
	 *
	 * @param scoreProvider The score provider to use.
	 */
	public EigenTrust(final org.nem.peer.trust.ScoreProvider scoreProvider) {
		this.trustScores = new TrustScores();
		this.scoreProvider = scoreProvider;
	}

	/**
	 * Gets the score provider.
	 *
	 * @return the score provider.
	 */
	protected org.nem.peer.trust.ScoreProvider getScoreProvider() {
		return this.scoreProvider;
	}

	/**
	 * Returns the trust matrix for the specified nodes.
	 *
	 * @param nodes The nodes.
	 * @return The trust matrix
	 */
	public Matrix getTrustMatrix(final Node[] nodes) {
		final Matrix matrix = this.trustScores.getScoreMatrix(nodes);
		matrix.normalizeColumns();
		return matrix;
	}

	/**
	 * Gets the trust scores.
	 *
	 * @return The trust scores.
	 */
	public TrustScores getTrustScores() {
		return this.trustScores;
	}

	/**
	 * Gets the number of computations.
	 *
	 * @return The number of computations.
	 */
	public int getNumComputations() {
		return this.numComputations;
	}

	/**
	 * Gets the number of computations that converged.
	 *
	 * @return The number of computations that converged.
	 */
	public int getNumConvergences() {
		return this.numConvergences;
	}

	/**
	 * Updates the local trust values for the specified node using the specified context.
	 *
	 * @param node The node.
	 * @param context The trust context.
	 */
	public void updateTrust(final Node node, final TrustContext context) {
		int index = 0;
		final Node[] nodes = context.getNodes();
		final ColumnVector scoreVector = new ColumnVector(nodes.length);
		for (final Node otherNode : nodes) {
			final NodeExperience experience = context.getNodeExperiences().getNodeExperience(node, otherNode);
			final long successfulCalls = experience.successfulCalls().get();
			final long failedCalls = experience.failedCalls().get();
			final double totalCalls = successfulCalls + failedCalls;

			final double score;
			if (totalCalls > 0) {
				score = this.scoreProvider.calculateTrustScore(experience) / totalCalls;
			} else {
				score = context.getPreTrustedNodes().isPreTrusted(otherNode) || node.equals(otherNode) ? 1.0 : 0.0;
			}

			scoreVector.setAt(index++, score);
		}

		final double scoreWeight = scoreVector.sum();
		scoreVector.normalize();
		this.trustScores.setScoreVector(node, nodes, scoreVector);
		this.trustScores.getScoreWeight(node).set(scoreWeight);
	}

	protected final void updateTrust(final TrustContext context) {
		for (final Node node : context.getNodes()) {
			this.updateTrust(node, context);
		}
	}

	/**
	 * Calculates a trust score given a trust context.
	 *
	 * @param context The trust context.
	 * @return The global trust vector.
	 */
	@Override
	public TrustResult computeTrust(final TrustContext context) {
		// (1) Compute the local trust values
		this.updateTrust(context);

		// (2) Compute the global trust
		return new TrustResult(context, this.computeGlobalTrust(context));
	}

	protected ColumnVector computeGlobalTrust(final TrustContext context) {
		final EigenTrustConvergencePolicy policy = new EigenTrustConvergencePolicy(
				context.getPreTrustedNodes().getPreTrustVector(context.getNodes()), this.getTrustMatrix(context.getNodes()),
				context.getParams().getAsInteger("MAX_ITERATIONS"), context.getParams().getAsDouble("EPSILON"),
				context.getParams().getAsDouble("ALPHA"));
		policy.converge();

		++this.numComputations;
		if (policy.hasConverged()) {
			++this.numConvergences;
		}

		return policy.getResult();
	}

	/**
	 * An EigenTrust score provider implementation.
	 */
	public static class ScoreProvider implements org.nem.peer.trust.ScoreProvider {

		@Override
		public double calculateTrustScore(final NodeExperience experience) {
			return Math.max(experience.successfulCalls().get() - experience.failedCalls().get(), 0.0);
		}

		@Override
		public double calculateCredibilityScore(final Node node1, final Node node2, final Node node3) {
			return 0;
		}
	}
}
