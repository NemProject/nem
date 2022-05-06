package org.nem.nis.pox.poi;

import org.nem.core.math.ColumnVector;

import java.util.logging.Logger;

/**
 * An abstract implementation of power iteration algorithm.
 */
public abstract class PowerIterator {

	private static final Logger LOGGER = Logger.getLogger(PowerIterator.class.getName());

	private final ColumnVector startVector;
	private final int maxIterations;
	private final double epsilon;

	private boolean hasConverged;
	private ColumnVector result;

	/**
	 * Creates a new poi power iterator.
	 *
	 * @param startVector The start vector.
	 * @param maxIterations The maximum number of iterations.
	 * @param epsilon The convergence epsilon value.
	 */
	public PowerIterator(final ColumnVector startVector, final int maxIterations, final double epsilon) {
		this.startVector = startVector;
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
	 * Runs the power iteration algorithm until convergence is reached or the maximum number of iterations have occurred.
	 */
	public void run() {
		int numIterations = 0;
		ColumnVector vector1;
		ColumnVector vector2 = this.step(this.startVector);
		do {
			vector1 = vector2;
			vector2 = this.step(vector1);
			++numIterations;
		} while (this.maxIterations > numIterations && !this.hasConverged(vector1, vector2));

		this.result = vector2;
		this.hasConverged = this.hasConverged(vector1, vector2);
		LOGGER.info(String.format("Iterations required: %d; converged?: %s", numIterations, this.hasConverged));
	}

	/**
	 * Performs a step of the algorithm.
	 *
	 * @param vector The vector that is the result of the last step.
	 * @return The result of this step.
	 */
	protected abstract ColumnVector stepImpl(final ColumnVector vector);

	private ColumnVector step(final ColumnVector vector) {
		final ColumnVector updatedVector = this.stepImpl(vector);
		updatedVector.normalize();
		return updatedVector;
	}

	private boolean hasConverged(final ColumnVector vector1, final ColumnVector vector2) {
		return vector1.l1Distance(vector2) <= this.epsilon;
	}
}
