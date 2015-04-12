package org.nem.peer.trust;

import org.nem.core.math.*;

/**
 * A convergence policy for the Eigen Trust algorithm based on the Taylor Series.
 * Although the original papers use PowerIteration for convergence,
 * PowerIteration converges slowly because the alpha value is large.
 * Taylor Series seems to work better and faster than PowerIteration.
 */
public class EigenTrustConvergencePolicy {

	private final ColumnVector preTrustVector;
	private final Matrix trustMatrix;
	private final int maxIterations;
	private final double epsilon;

	private boolean hasConverged;
	private ColumnVector result;

	/**
	 * Creates a new eigen trust power iterator.
	 *
	 * @param preTrustVector The pre-trust vector.
	 * @param trustMatrix The trust matrix.
	 * @param maxIterations The maximum number of iterations.
	 * @param epsilon The convergence epsilon value.
	 */
	public EigenTrustConvergencePolicy(
			final ColumnVector preTrustVector,
			final Matrix trustMatrix,
			final int maxIterations,
			final double epsilon) {
		this.preTrustVector = preTrustVector;
		this.trustMatrix = trustMatrix;
		this.maxIterations = maxIterations;
		this.epsilon = epsilon;

		this.hasConverged = false;
	}

	public boolean hasConverged() {
		return this.hasConverged;
	}

	public ColumnVector getResult() {
		return this.result;
	}

	/**
	 * Runs the convergence algorithm until convergence is reached
	 * or the maximum number of iterations have occurred.
	 */
	public void converge() {
		final int numDimensions = this.preTrustVector.size();
		int numIterations = 0;
		double scale = 1.0;
		ColumnVector sumVector = new ColumnVector(numDimensions);
		ColumnVector lastTermVector = this.trustMatrix.multiply(this.preTrustVector);
		do {
			sumVector = sumVector.addElementWise(lastTermVector);

			lastTermVector = this.trustMatrix.multiply(lastTermVector);
			scale += 1.0;
			lastTermVector.scale(scale);

			++numIterations;
		} while (this.maxIterations > numIterations && !this.hasConverged(lastTermVector));

		this.hasConverged = this.hasConverged(lastTermVector);
		this.result = sumVector;
		this.result.normalize();
	}

	private boolean hasConverged(final ColumnVector vector) {
		return vector.getMagnitude() <= this.epsilon;
	}
}
