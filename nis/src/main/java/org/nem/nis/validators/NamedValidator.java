package org.nem.nis.validators;

/**
 * Interface for validators that have a name.
 */
public interface NamedValidator {

	/**
	 * Gets the name of the validator.
	 *
	 * @return The name of the validator.
	 */
	default String getName() {
		return this.getClass().getSimpleName();
	}
}
