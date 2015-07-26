package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a quantity that can be used by any class which needs to handle non-negative quantities.
 */
public class Quantity extends AbstractQuantity<Quantity> {
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
	}

	@Override
	protected Quantity createFromRaw(final long quantity) {
		return new Quantity(quantity);
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
