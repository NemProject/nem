package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a quantity that can be used by any class which needs to handle non-negative quantities.
 */
public class Quantity extends AbstractPrimitive<Quantity, Long> {
	/**
	 * Quantity representing 0.
	 */
	public static final Quantity ZERO = new Quantity(0);

	/**
	 * Creates a new quantity given a value.
	 *
	 * @param value The value.
	 * @return The new quantity.
	 */
	public static Quantity fromValue(final long value) {
		return new Quantity(value);
	}

	/**
	 * Creates a quantity.
	 *
	 * @param quantity The quantity.
	 */
	public Quantity(final long quantity) {
		super(quantity, Quantity.class);

		if (quantity < 0) {
			throw new NegativeQuantityException(quantity);
		}
	}

	/**
	 * Creates a new Quantity by adding the specified quantity to this quantity.
	 *
	 * @param quantity The specified quantity.
	 * @return The new quantity.
	 */
	public Quantity add(final Quantity quantity) {
		return new Quantity(this.getRaw() + quantity.getRaw());
	}

	/**
	 * Creates a new Quantity by subtracting the specified quantity from this quantity.
	 *
	 * @param quantity The specified quantity.
	 * @return The new quantity.
	 */
	public Quantity subtract(final Quantity quantity) {
		return new Quantity(this.getRaw() - quantity.getRaw());
	}

	/**
	 * Creates a new Quantity by multiplying the specified quantity with this quantity.
	 *
	 * @param quantity The specified quantity.
	 * @return The new quantity.
	 */
	public Quantity multiply(final Quantity quantity) {
		return new Quantity(this.getRaw() * quantity.getRaw());
	}

	/**
	 * Returns the quantity.
	 *
	 * @return The quantity.
	 */
	public long getRaw() {
		return this.getValue();
	}

	//region inline serialization

	/**
	 * Writes a Quantity object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param quantity The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Quantity quantity) {
		serializer.writeLong(label, quantity.getRaw());
	}

	/**
	 * Reads a Quantity object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Quantity readFrom(final Deserializer deserializer, final String label) {
		return new Quantity(deserializer.readLong(label));
	}

	//endregion
}
