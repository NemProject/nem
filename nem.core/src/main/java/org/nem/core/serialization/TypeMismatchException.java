package org.nem.core.serialization;

/**
 * Exception that is thrown to indicate a serialization failure caused by a property value having an incompatible type.
 */
public class TypeMismatchException extends InvalidPropertyException {

	/**
	 * Creates a new exception.
	 *
	 * @param propertyName The name of the property with the incompatible value.
	 */
	public TypeMismatchException(final String propertyName) {
		super("property %s has an incompatible value", propertyName);
	}
}
