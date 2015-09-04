package org.nem.core.serialization;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

/**
 * An abstract class for forward-only serialization of primitive data types.
 * Implementations may use or ignore label parameters but label-based lookup is not guaranteed.
 */
public abstract class Serializer {
	private static final Logger LOGGER = Logger.getLogger(Serializer.class.getName());

	private final SerializationContext context;

	/**
	 * Creates a serializer.
	 *
	 * @param context The serialization context.
	 */
	protected Serializer(final SerializationContext context) {
		this.context = null == context ? new SerializationContext() : context;
	}

	/**
	 * Writes a 32-bit integer value.
	 *
	 * @param label The optional name of the value.
	 * @param i The value.
	 */
	public abstract void writeInt(final String label, final int i);

	/**
	 * Writes a 64-bit long value.
	 *
	 * @param label The optional name of the value.
	 * @param l The value.
	 */
	public abstract void writeLong(final String label, final long l);

	/**
	 * Writes a 64-bit double value.
	 *
	 * @param label The optional name of the value.
	 * @param d The value.
	 */
	public abstract void writeDouble(final String label, final double d);

	/**
	 * Writes a BigInteger value.
	 *
	 * @param label The optional name of the value.
	 * @param i The value.
	 */
	public abstract void writeBigInteger(final String label, final BigInteger i);

	/**
	 * Writes a byte array value.
	 *
	 * @param label The optional name of the value.
	 * @param bytes The value.
	 */
	public final void writeBytes(final String label, final byte[] bytes) {
		this.writeBytes(label, bytes, this.context.getDefaultMaxBytesLimit());
	}

	/**
	 * Writes a byte array value.
	 *
	 * @param label The optional name of the value.
	 * @param bytes The value.
	 * @param limit The maximum number of bytes.
	 */
	public final void writeBytes(final String label, final byte[] bytes, final int limit) {
		byte[] value = bytes;
		if (null != value && value.length > limit) {
			final byte[] truncatedValue = Arrays.copyOf(value, limit);
			LOGGER.info(String.format("truncated '%s' bytes from '%s' to '%s'", label, value.length, truncatedValue.length));
			value = truncatedValue;
		}

		this.writeBytesImpl(label, value);
	}

	/**
	 * Writes a byte array value.
	 *
	 * @param label The optional name of the value.
	 * @param bytes The value.
	 */
	protected abstract void writeBytesImpl(final String label, final byte[] bytes);

	/**
	 * Writes a String value.
	 *
	 * @param label The optional name of the value.
	 * @param s The value.
	 */
	public final void writeString(final String label, final String s) {
		this.writeString(label, s, this.context.getDefaultMaxCharsLimit());
	}

	/**
	 * Writes a String value.
	 *
	 * @param label The optional name of the value.
	 * @param s The value.
	 * @param limit The maximum number of characters.
	 */
	public final void writeString(final String label, final String s, final int limit) {
		String value = s;
		if (null != value && value.length() > limit) {
			final String truncatedValue = s.substring(0, limit);
			LOGGER.info(String.format("truncated '%s' string from '%s' to '%s'", label, value, truncatedValue));
			value = truncatedValue;
		}

		this.writeStringImpl(label, value);
	}

	/**
	 * Writes a String value.
	 *
	 * @param label The optional name of the value.
	 * @param s The value.
	 */
	protected abstract void writeStringImpl(final String label, final String s);

	/**
	 * Writes an object value.
	 *
	 * @param label The optional name of the value.
	 * @param object The value.
	 */
	public abstract void writeObject(final String label, final SerializableEntity object);

	/**
	 * Writes an array of object values.
	 *
	 * @param label The optional name of the value.
	 * @param objects The array.
	 */
	public abstract void writeObjectArray(final String label, final Collection<? extends SerializableEntity> objects);

	/**
	 * Gets the current serialization context.
	 *
	 * @return The current serialization context.
	 */
	public final SerializationContext getContext() {
		return this.context;
	}
}