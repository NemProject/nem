package org.nem.core.test;

import org.nem.core.serialization.*;

/**
 * A mock SerializableEntity implementation.
 */
public class MockSerializableEntity implements SerializableEntity {

	private final int intValue;
	private final String stringValue;
	private final long longValue;

	/**
	 * Creates a new MockSerializableEntity object.
	 */
	public MockSerializableEntity() {
		this(1, "a", 2);
	}

	/**
	 * Creates a new MockSerializableEntity object.
	 *
	 * @param intValue The int value.
	 * @param stringValue The string value.
	 * @param longValue The long value.
	 */
	public MockSerializableEntity(final int intValue, final String stringValue, final long longValue) {
		this.intValue = intValue;
		this.stringValue = stringValue;
		this.longValue = longValue;
	}

	/**
	 * Deserializes a MockSerializableEntity object.
	 *
	 * @param deserializer The deserializer.
	 */
	public MockSerializableEntity(final Deserializer deserializer) {
		this.intValue = deserializer.readInt("int");
		this.stringValue = deserializer.readOptionalString("s");
		this.longValue = deserializer.readLong("long");
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeInt("int", this.intValue);
		serializer.writeString("s", this.stringValue);
		serializer.writeLong("long", this.longValue);
	}

	@Override
	public int hashCode() {
		return this.intValue;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MockSerializableEntity)) {
			return false;
		}

		final MockSerializableEntity rhs = (MockSerializableEntity)obj;
		return
				this.intValue == rhs.intValue
						&& this.stringValue.equals(rhs.stringValue)
						&& this.longValue == rhs.longValue;
	}

	@Override
	public String toString() {
		return String.format(
				"int: %d; string: %s; long: %d",
				this.intValue,
				this.stringValue,
				this.longValue);
	}

	/**
	 * ObjectDeserializer implementation that can activate MockSerializableEntity objects.
	 */
	public static class Activator implements ObjectDeserializer<MockSerializableEntity> {

		private DeserializationContext lastContext;

		/**
		 * Gets the last deserialization context passed to deserialize.
		 *
		 * @return The last deserialization context passed to deserialize
		 */
		public DeserializationContext getLastContext() {
			return this.lastContext;
		}

		@Override
		public MockSerializableEntity deserialize(final Deserializer deserializer) {
			this.lastContext = deserializer.getContext();
			return new MockSerializableEntity(deserializer);
		}
	}
}
