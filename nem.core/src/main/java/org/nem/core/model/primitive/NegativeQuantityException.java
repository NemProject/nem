package org.nem.core.model.primitive;

/**
 * An exception that is thrown when a Quantity is attempted to be created around a negative quantity.
 */
public class NegativeQuantityException extends IllegalArgumentException {

	/**
	 * Creates a new exception.
	 *
	 * @param quantity The negative quantity.
	 */
	public NegativeQuantityException(final long quantity) {
		super(String.format("quantity (%d) must be non-negative", quantity));
	}
}
