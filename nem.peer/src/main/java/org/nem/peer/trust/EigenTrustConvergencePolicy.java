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
	public EigenTrustConvergencePolicy(final ColumnVector preTrustVector, final Matrix trustMatrix, final int maxIterations,
			final double epsilon, final double alpha) {
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
	 * Runs the convergence algorithm until convergence is reached or the maximum number of iterations have occurred.
	 */
	public void converge() {
		int numIterations = 0;
		ColumnVector t = this.preTrustVector;
		for (; this.maxIterations > numIterations; ++numIterations) {
			final ColumnVector s = t;
			t = this.trustMatrix.multiply(s).multiply(1 - this.alpha);
			t = t.addElementWise(this.preTrustVector.multiply(this.alpha));

			if (this.hasConverged(s, t)) {
				break;
			}
		}

		this.hasConverged = numIterations < this.maxIterations;
		if (!this.hasConverged) {
			LOGGER.severe(String.format("trust algorithm did not converge within %d iterations", this.maxIterations));
		}

		this.result = t;
		this.result.normalize();
	}

	private boolean hasConverged(final ColumnVector vector1, final ColumnVector vector2) {
		final double distance = vector1.l1Distance(vector2);
		return distance <= this.epsilon;
	}
}
