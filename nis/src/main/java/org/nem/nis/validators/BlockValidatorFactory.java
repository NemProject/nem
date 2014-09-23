package org.nem.nis.validators;

import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {

	/**
	 * Creates a block validator.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public BlockValidator create(final PoiFacade poiFacade) {
		return new EligibleSignerBlockValidator(poiFacade);
	}
}
