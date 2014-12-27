package org.nem.core.model.primitive;

/**
 * An exception that is thrown when an Amount is attempted to be created around a negative amount.
 */
public class NegativeBalanceException extends IllegalArgumentException {

	/**
	 * Creates a new exception.
	 *
	 * @param amount The negative amount.
	 */
	public NegativeBalanceException(final long amount) {
		super(String.format("amount (%d) must be non-negative", amount));
	}
}
