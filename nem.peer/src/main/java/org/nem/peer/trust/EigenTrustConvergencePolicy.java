package org.nem.peer.trust;

import org.nem.core.math.*;

import java.util.logging.Logger;

/**
 * A convergence policy for the Eigentrust algorithm based on the PowerIteration.
 */
public class EigenTrustConvergencePolicy {
	private static final Logger LOGGER = Logger.getLogger(EigenTrustConvergencePolicy.class.getName());

	private final ColumnVector preTrustVector;
	private final Matrix trustMatrix;
	private final int maxIterations;
	private final double epsilon;
	private final double alpha;

	private boolean hasConverged;
	private ColumnVector result;

	/**
	 * Creates a new eigen trust power iterator.
	 *
	 * @param preTrustVector The pre-trust vector.
	 * @param trustMatrix The trust matrix.
	 * @param maxIterations The maximum number of iterations.
	 * @param epsilon The convergence epsilon value.
	 * @param alpha The weight for the pretrusted nodes.
	 */
	public EigenTrustConvergencePolicy(
			final ColumnVector preTrustVector,
			final Matrix trustMatrix,
			final int maxIterations,
			final double epsilon,
			final double alpha) {
		this.preTrustVector = preTrustVector;
		this.trustMatrix = trustMatrix;
		this.maxIterations = maxIterations;
		this.epsilon = epsilon;
		this.alpha = alpha;

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
		ColumnVector t = new ColumnVector(numDimensions).addElementWise(this.preTrustVector);
		ColumnVector s = new ColumnVector(numDimensions);
		while (this.maxIterations > numIterations && !this.hasConverged(s, t)) {
			s = t;
			t = this.trustMatrix.multiply(s).multiply(1 - this.alpha);
			t = t.addElementWise(this.preTrustVector.multiply(this.alpha));
			++numIterations;
		}

		//final ColumnVector difference = t.addElementWise(s.multiply(-1));
		this.hasConverged = this.hasConverged(s, t);
		if (!this.hasConverged) {
			LOGGER.severe(String.format("trust algorithm did not converge within %d iterations", this.maxIterations));
		} else {
			LOGGER.info(String.format("converge needed %d iterations", numIterations));
		}
		this.result = t;
		this.result.normalize();
	}

	private boolean hasConverged(final ColumnVector vector) {
		return vector.getMagnitude() <= this.epsilon;
	}

	private boolean hasConverged(final ColumnVector vector1, final ColumnVector vector2) {
		final double threshold = 1.0 / (vector1.size() * 10);
		for (int i = 0; i < vector1.size(); i++) {
			final double vec1 = vector1.getAt(i);
			final double vec2 = vector2.getAt(i);

			// only fail if
			// 1) one of the values is relevant
			// 2) the difference is more than 10% of the fair share of a node
			// 3) it changed by more than 5%
			if ((vec1 > threshold || vec2 > threshold) &&
					Math.abs(vec1 - vec2) * vector1.size() > 0.1 &&
					Math.abs(vec1 - vec2) / Math.max(vec1, vec2) > 0.05) {
				return false;
			}
		}

		return true;
	}
}
