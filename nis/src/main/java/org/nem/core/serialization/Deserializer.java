package org.nem.core.serialization;

import java.math.BigInteger;
import java.util.List;

/**
 * An interface for forward-only deserialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public interface Deserializer {

	/**
	 * Reads a 32-bit integer value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public Integer readInt(final String label);

	/**
	 * Reads a 64-bit long value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public Long readLong(final String label);

	/**
	 * Reads a BigInteger value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public BigInteger readBigInteger(final String label);

	/**
	 * Reads a byte array value
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public byte[] readBytes(final String label);

	/**
	 * Reads a String value.
	 *
	 * @param label The optional name of the value.
	 *
	 * @return The read value.
	 */
	public String readString(final String label);

	/**
	 * Reads an object value.
	 *
	 * @param label     The optional name of the value.
	 * @param activator The activator that should be used to create the SerializableEntity value.
	 * @param <T>       The type of SerializableEntity object.
	 *
	 * @return The read value.
	 */
	public <T> T readObject(final String label, final ObjectDeserializer<T> activator);

	/**
	 * Reads an array of object values.
	 *
	 * @param label     The optional name of the value.
	 * @param activator The activator that should be used to create the SerializableEntity values.
	 *
	 * @return The read array.
	 */
	public <T> List<T> readObjectArray(final String label, final ObjectDeserializer<T> activator);

	/**
	 * Gets the current deserialization context.
	 *
	 * @return The current deserialization context.
	 */
	public DeserializationContext getContext();
}