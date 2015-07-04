package org.nem.core.model;

import org.nem.core.serialization.*;

/**
 * Represents a nem name-value pair.
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
		this.name = name.toLowerCase();
		this.value = value;
	}

	/**
	 * Deserializes a nem property.
	 *
	 * @param deserializer The deserializer.
	 */
	public NemProperty(final Deserializer deserializer) {
		this.name = deserializer.readString("name").toLowerCase();
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
	public void serialize(final Serializer serializer) {
		serializer.writeString("name", this.name);
		serializer.writeString("value", this.value);
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.name, this.value);
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
}
