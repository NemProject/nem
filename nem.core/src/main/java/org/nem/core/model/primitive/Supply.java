package org.nem.core.model.primitive;

import org.nem.core.serialization.*;

/**
 * Represents a supply that can be used by any class which needs to handle non-negative supplies.
 */
public class Supply extends AbstractQuantity<Supply> {
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
	}

	@Override
	protected Supply createFromRaw(final long quantity) {
		return new Supply(quantity);
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
