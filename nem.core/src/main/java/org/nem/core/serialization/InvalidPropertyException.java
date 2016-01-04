package org.nem.core.serialization;

/**
 * Base class for all exceptions that indicate an invalid serialization property.
 */
public class InvalidPropertyException extends SerializationException {
	private final String propertyName;

	/**
	 * Creates a new exception.
	 *
	 * @param message The message (this is expected to be a format string where the property name is the single argument).
	 * @param propertyName The property name.
	 */
	protected InvalidPropertyException(final String message, final String propertyName) {
		super(String.format(message, propertyName));

		this.propertyName = propertyName;
	}

	/**
	 * Gets the invalid property name.
	 *
	 * @return The invalid property name.
	 */
	public String getPropertyName() {
		return this.propertyName;
	}
}
