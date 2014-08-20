package org.nem.core.serialization;

/**
 * Exception that is thrown to indicate a serialization failure caused by a missing required property.
 */
public class MissingRequiredPropertyException extends InvalidPropertyException {

	/**
	 * Creates a new exception.
	 *
	 * @param propertyName The missing property name.
	 */
	public MissingRequiredPropertyException(final String propertyName) {
		super("expected value for property %s, but none was found", propertyName);
	}
}
