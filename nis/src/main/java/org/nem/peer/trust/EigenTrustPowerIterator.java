package org.nem.peer.trust;

/**
 * An implementation of power iteration for the Eigen Trust algorithm.
 */
public class EigenTrustPowerIterator {

	private final Vector preTrustVector;
	private final Matrix trustMatrix;
	private final int maxIterations;
	private final double alpha;
	private final double epsilon;

	private final Vector weightedPreTrustVector;
	private boolean hasConverged;
	private Vector result;

	/**
	 * Creates a new eigen trust power iterator.
	 *
	 * @param preTrustVector The pre-trust vector.
	 * @param trustMatrix    The trust matrix.
	 * @param maxIterations  The maximum number of iterations.
	 * @param alpha          The alpha value.
	 * @param epsilon        The convergence epsilon value.
	 */
	public EigenTrustPowerIterator(
			final Vector preTrustVector,
			final Matrix trustMatrix,
			int maxIterations,
			double alpha,
			double epsilon) {
		this.preTrustVector = preTrustVector;
		this.trustMatrix = trustMatrix;
		this.maxIterations = maxIterations;
		this.alpha = alpha;
		this.epsilon = epsilon;

		this.weightedPreTrustVector = this.preTrustVector.multiply(this.alpha);
		this.hasConverged = false;
	}

	public boolean hasConverged() {
		return this.hasConverged;
	}

	public Vector getResult() {
		return this.result;
	}

	/**
	 * Runs the power iteration algorithm until convergence is reached
	 * or the maximum number of iterations have occurred.
	 */
	public void run() {
		int numIterations = 0;
		Vector vector1;
		Vector vector2 = this.step(this.preTrustVector);
		do {
			vector1 = vector2;
			vector2 = this.step(vector1);
			++numIterations;
		} while (maxIterations > numIterations && !this.hasConverged(vector1, vector2));

		this.result = vector2;
		this.hasConverged = this.hasConverged(vector1, vector2);
	}

	private Vector step(final Vector vector) {
		Vector trustVector = vector.multiply(this.trustMatrix);
		trustVector = trustVector.multiply(1 - this.alpha);

		final Vector result = trustVector.add(this.weightedPreTrustVector);
		result.align();
		result.normalize();
		return result;
	}

	private boolean hasConverged(final Vector vector1, final Vector vector2) {
		return vector1.distance(vector2) <= this.epsilon;
	}
}
