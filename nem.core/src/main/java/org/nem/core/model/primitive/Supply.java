package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a supply that can be used by any class which needs to handle non-negative supplies.
 * TODO 201507262015 J-J: refactor with Quantity
 */
public class Supply extends AbstractPrimitive<Supply, Long> {
	/**
	 * Supply representing 0.
	 */
	public static final Supply ZERO = new Supply(0);

	/**
	 * Creates a new supply given a value.
	 *
	 * @param value The value.
	 * @return The new supply.
	 */
	public static Supply fromValue(final long value) {
		return new Supply(value);
	}

	/**
	 * Creates a supply.
	 *
	 * @param supply The supply.
	 */
	public Supply(final long supply) {
		super(supply, Supply.class);

		if (supply < 0) {
			throw new NegativeQuantityException(supply);
		}
	}

	/**
	 * Creates a new Supply by adding the specified supply to this supply.
	 *
	 * @param supply The specified supply.
	 * @return The new supply.
	 */
	public Supply add(final Supply supply) {
		return new Supply(this.getRaw() + supply.getRaw());
	}

	/**
	 * Creates a new Supply by subtracting the specified supply from this supply.
	 *
	 * @param supply The specified supply.
	 * @return The new supply.
	 */
	public Supply subtract(final Supply supply) {
		return new Supply(this.getRaw() - supply.getRaw());
	}

	/**
	 * Returns the supply.
	 *
	 * @return The supply.
	 */
	public long getRaw() {
		return this.getValue();
	}

	//region inline serialization

	/**
	 * Writes a Supply object.
	 *
	 * @param serializer The serializer to use.
	 * @param label The optional label.
	 * @param supply The object.
	 */
	public static void writeTo(final Serializer serializer, final String label, final Supply supply) {
		serializer.writeLong(label, supply.getRaw());
	}

	/**
	 * Reads a Supply object.
	 *
	 * @param deserializer The deserializer to use.
	 * @param label The optional label.
	 * @return The read object.
	 */
	public static Supply readFrom(final Deserializer deserializer, final String label) {
		return new Supply(deserializer.readLong(label));
	}

	//endregion
}
