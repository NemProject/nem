package org.nem.core.serialization;

/**
 * Exception that is thrown to indicate a serialization failure caused by a missing required property.
 */
public class MissingRequiredPropertyException extends SerializationException {
	private final String propertyName;

	/**
	 * Creates a new exception.
	 *
	 * @param propertyName The missing property name.
	 */
	public MissingRequiredPropertyException(final String propertyName) {
		super(String.format("expected value for property %s, but none was found", propertyName));

		this.propertyName = propertyName;
	}

	/**
	 * Gets the missing property name.
	 *
	 * @return The missing property name.
	 */
	public String getPropertyName() {
		return this.propertyName;
	}
}
