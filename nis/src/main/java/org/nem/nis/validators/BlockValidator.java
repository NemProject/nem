package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * Interface for validating a block.
 */
public interface BlockValidator extends NamedValidator {

	/**
	 * Checks the validity of the specified block.
	 *
	 * @param block The block.
	 * @return The validation result.
	 */
	ValidationResult validate(final Block block);
}
