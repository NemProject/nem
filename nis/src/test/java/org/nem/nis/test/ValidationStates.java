package org.nem.nis.test;

import org.nem.nis.validators.ValidationState;

/**
 * Validation states used for testing.
 */
public class ValidationStates {

	/**
	 * A validation state that throws when called.
	 */
	public static final ValidationState Throw = new ValidationState(DebitPredicates.XemThrow, DebitPredicates.MosaicThrow, null);
}
