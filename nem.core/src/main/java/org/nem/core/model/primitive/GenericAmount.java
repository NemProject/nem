package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a generic amount that can be used by any class which needs to handle positive amounts.
 * TODO 20150702 J-B: we should rename this and add tests.
 */
public class GenericAmount extends AbstractPrimitive<GenericAmount, Long> {
	/**
	 * Amount representing 0.
	 */
	public static final GenericAmount ZERO = new GenericAmount(0);

	/**
	 * Creates a new generic amount given a value.
	 *
	 * @param value The value.
	 * @return The new amount.
	 */
	public static GenericAmount fromValue(final long value) {
		return new GenericAmount(value);
	}

	/**
	 * Creates a generic amount.
	 *
	 * @param amount The amount.
	 */
	public GenericAmount(final long amount) {
		super(amount, GenericAmount.class);

		if (amount < 0) {
			throw new NegativeBalanceException(amount);
		}
	}

	/**
	 * Creates a new GenericAmount by adding the specified amount to this amount.
	 *
	 * @param amount The specified amount.
	 * @return The new amount.
	 */
	public GenericAmount add(final GenericAmount amount) {
		return new GenericAmount(this.getAmount() + amount.getAmount());
	}

	/**
	 * Creates a new GenericAmount by subtracting the specified amount from this amount.
	 *
	 * @param amount The specified amount.
	 * @return The new amount.
	 */
	public GenericAmount subtract(final GenericAmount amount) {
		return new GenericAmount(this.getAmount() - amount.getAmount());
	}

	/**
	 * Creates a new GenericAmount by multiplying this amount by the specified scalar
	 *
	 * @param scalar The specified scalar.
	 * @return The new amount.
	 */
	public GenericAmount multiply(final int scalar) {
		return new GenericAmount(this.getAmount() * scalar);
	}

	/**
	 * Returns the amount.
	 *
	 * @return The amount.
	 */
	public long getAmount() {
		return this.getValue();
	}

	//region inline serialization

	/**
	 * Writes a GenericAmount object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param amount The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final GenericAmount amount) {
		serializer.writeLong(label, amount.getAmount());
	}

	/**
	 * Reads a GenericAmount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static GenericAmount readFrom(final Deserializer deserializer, final String label) {
		return new GenericAmount(deserializer.readLong(label));
	}

	/**
	 * Reads an optional GenericAmount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static GenericAmount readFromOptional(final Deserializer deserializer, final String label) {
		final Long value = deserializer.readOptionalLong(label);
		return null == value ? null : new GenericAmount(value);
	}

	//endregion
}
