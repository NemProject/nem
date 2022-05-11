package org.nem.peer.trust.score;

/**
 * Simple class that wraps a double and constrains it to a real value.
 */
public class RealDouble {

	private double value;

	/**
	 * Creates a new real double.
	 *
	 * @param value The double value.
	 */
	public RealDouble(final double value) {
		this.set(value);
	}

	/**
	 * Gets the double value.
	 *
	 * @return The double value
	 */
	public double get() {
		return this.value;
	}

	/**
	 * Sets the double value.
	 *
	 * @param value The double value
	 */
	public void set(final double value) {
		this.value = (Double.isNaN(value) || Double.isInfinite(value)) ? 0.0 : value;
	}
}
