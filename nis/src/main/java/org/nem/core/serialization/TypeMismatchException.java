package org.nem.core.serialization;

/**
 * Exception that is thrown to indicate a serialization failure caused by a property value having an incompatible type.
 */
public class TypeMismatchException extends SerializationException {
	private final String propertyName;

	/**
	 * Creates a new exception.
	 *
	 * @param propertyName The name of the property with the incompatible value.
	 */
	public TypeMismatchException(final String propertyName) {
		super(String.format("property %s has an incompatible value", propertyName));

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
