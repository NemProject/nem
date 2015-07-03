package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents an entry in the nem properties.
 */
public class NemProperty implements SerializableEntity {
	private final String name;
	private final String value;

	/**
	 * Creates a new nem properties entry.
	 *
	 * @param name The name.
	 * @param value The value.
	 */
	public NemProperty(final String name, final String value) {
		// TODO 20150720 J-B: should the names be case-insensitive?
		this.name = name;
		this.value = value;
	}

	/**
	 * Deserializes a nem property.
	 *
	 * @param deserializer The deserializer.
	 */
	public NemProperty(final Deserializer deserializer) {
		this.name = deserializer.readString("name");
		this.value = deserializer.readString("value");
	}

	/**
	 * Gets the name.
	 *
	 * @return The name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the value.
	 *
	 * @return The value.
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() ^ this.value.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NemProperty)) {
			return false;
		}

		final NemProperty rhs = (NemProperty)obj;
		return this.name.equals(rhs.name) && this.value.equals(rhs.value);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("name", this.name);
		serializer.writeString("value", this.value);
	}
}
