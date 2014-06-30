package org.nem.core.serialization;

import java.math.BigInteger;
import java.util.List;

/**
 * An abstract class for forward-only deserialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public abstract class Deserializer {
	private final DeserializationContext context;

	/**
	 * Creates a deserializer.
	 *
	 * @param context The deserialization context.
	 */
	protected Deserializer(final DeserializationContext context) {
		this.context = context;
	}

	//region read[Optional]Int

	/**
	 * Reads a 32-bit integer value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public final Integer readInt(final String label) {
		return this.requireNonNull(label, this.readOptionalInt(label));
	}

	/**
	 * Reads a 32-bit integer value (allowing null values).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract Integer readOptionalInt(final String label);

	//endregion

	//region read[Optional]Long

	/**
	 * Reads a 64-bit long value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public final Long readLong(final String label) {
		return this.requireNonNull(label, this.readOptionalLong(label));
	}

	/**
	 * Reads a 64-bit long value (allowing null values).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract Long readOptionalLong(final String label);

	//endregion

	//region read[Optional]Double

	/**
	 * Reads a 64-bit double value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public final Double readDouble(final String label) {
		return this.requireNonNull(label, this.readOptionalDouble(label));
	}

	/**
	 * Reads a 64-bit double value (allowing null values).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract Double readOptionalDouble(final String label);

	//endregion

	//region read[Optional]BigInteger

	/**
	 * Reads a BigInteger value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public final BigInteger readBigInteger(final String label) {
		return this.requireNonNull(label, this.readOptionalBigInteger(label));
	}

	/**
	 * Reads a BigInteger value (allowing null values).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract BigInteger readOptionalBigInteger(final String label);

	//endregion

	//region readBytes

	/**
	 * Reads a byte array value.
	 * (Note that there is not an "or default" variant of this function because an empty array is always allowable).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract byte[] readBytes(final String label);

	//endregion

	//region read[Optional]String

	/**
	 * Reads a String value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public final String readString(final String label) {
		return this.requireNonNull(label, this.readOptionalString(label));
	}

	/**
	 * Reads a String value (allowing null values).
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public abstract String readOptionalString(final String label);

	//endregion

	//region read[Optional]Object

	/**
	 * Reads an object value.
	 *
	 * @param label     The optional name of the value.
	 * @param activator The activator that should be used to create the SerializableEntity value.
	 * @param <T>       The type of SerializableEntity object.
	 *
	 * @return The read value.
	 */
	public final <T> T readObject(final String label, final ObjectDeserializer<T> activator) {
		return this.requireNonNull(label, this.readOptionalObject(label, activator));
	}

	/**
	 * Reads an object value (allowing null values).
	 *
	 * @param label     The optional name of the value.
	 * @param activator The activator that should be used to create the SerializableEntity value.
	 * @param <T>       The type of SerializableEntity object.
	 *
	 * @return The read value.
	 */
	public abstract <T> T readOptionalObject(final String label, final ObjectDeserializer<T> activator);

	//endregion

	//region readObjectArray

	/**
	 * Reads an array of object values.
	 * (Note that there is not an "or default" variant of this function because an empty array is always allowable).
	 *
	 * @param label     The optional name of the value.
	 * @param activator The activator that should be used to create the SerializableEntity values.
	 *
	 * @return The read array.
	 */
	public abstract <T> List<T> readObjectArray(final String label, final ObjectDeserializer<T> activator);

	//endregion

	/**
	 * Gets the current deserialization context.
	 *
	 * @return The current deserialization context.
	 */
	public final DeserializationContext getContext() {
		return this.context;
	}

	private <T> T requireNonNull(final String label, final T value) {
		if (null == value)
			throw new SerializationException(String.format("expected non-null value for property %s", label));

		return value;
	}
}