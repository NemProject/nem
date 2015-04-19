package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents an amount of NEM.
 */
public class Amount extends AbstractPrimitive<Amount, Long> {

	public static final int MICRONEMS_IN_NEM = 1000000;

	/**
	 * Amount representing 0 NEM.
	 */
	public static final Amount ZERO = new Amount(0);

	/**
	 * Creates a new amount given a quantity of NEM.
	 *
	 * @param amount The amount of NEM.
	 * @return The new amount.
	 */
	public static Amount fromNem(final long amount) {
		return new Amount(amount * MICRONEMS_IN_NEM);
	}

	/**
	 * Creates a new amount given a quantity of micro NEM.
	 *
	 * @param amount The amount of micro NEM.
	 * @return The new amount.
	 */
	public static Amount fromMicroNem(final long amount) {
		return new Amount(amount);
	}

	/**
	 * Creates a NEM amount.
	 *
	 * @param amount The number of micro NEM.
	 */
	public Amount(final long amount) {
		super(amount, Amount.class);

		if (amount < 0) {
			throw new NegativeBalanceException(amount);
		}
	}

	/**
	 * Creates a new Amount by adding the specified amount to this amount.
	 *
	 * @param amount The specified amount.
	 * @return The new amount.
	 */
	public Amount add(final Amount amount) {
		return new Amount(this.getNumMicroNem() + amount.getNumMicroNem());
	}

	/**
	 * Creates a new Amount by subtracting the specified amount from this amount.
	 *
	 * @param amount The specified amount.
	 * @return The new amount.
	 */
	public Amount subtract(final Amount amount) {
		return new Amount(this.getNumMicroNem() - amount.getNumMicroNem());
	}

	/**
	 * Creates a new Amount by multiplying this amount by the specified scalar
	 *
	 * @param scalar The specified scalar.
	 * @return The new amount.
	 */
	public Amount multiply(final int scalar) {
		return new Amount(this.getNumMicroNem() * scalar);
	}

	/**
	 * Returns the number of micro NEM.
	 *
	 * @return The number of micro NEM.
	 */
	public long getNumMicroNem() {
		return this.getValue();
	}

	/**
	 * Returns the number of NEM (rounded down to the nearest NEM).
	 *
	 * @return The number of NEM.
	 */
	public long getNumNem() {
		return this.getValue() / MICRONEMS_IN_NEM;
	}

	//region inline serialization

	/**
	 * Writes an amount object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param amount The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Amount amount) {
		serializer.writeLong(label, amount.getNumMicroNem());
	}

	/**
	 * Reads an amount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Amount readFrom(final Deserializer deserializer, final String label) {
		return new Amount(deserializer.readLong(label));
	}

	/**
	 * Reads an optional amount object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Amount readFromOptional(final Deserializer deserializer, final String label) {
		final Long value = deserializer.readOptionalLong(label);
		return null == value ? null : new Amount(value);
	}

	//endregion
}
