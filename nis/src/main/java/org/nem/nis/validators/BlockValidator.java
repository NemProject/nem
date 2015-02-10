package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * Interface for validating a block.
 */
public interface BlockValidator {

	/**
	 * Gets the name of the validator.
	 *
	 * @return The name of the validator.
	 */
	public default String getName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Checks the validity of the specified block.
	 *
	 * @param block The block.
	 * @return The validation result.
	 */
	public ValidationResult validate(final Block block);
}
