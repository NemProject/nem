package org.nem.core.test;

import org.nem.core.serialization.*;

/**
 * A mock SerializableEntity implementation.
 */
public class MockSerializableEntity implements SerializableEntity {

    private int intValue;
    private String stringValue;
    private long longValue;

    /**
     * Creates a new MockSerializableEntity object.
     *
     * @param intValue The int value.
     * @param stringValue The string value.
     * @param longValue The long value.
     */
    public MockSerializableEntity(final int intValue, final String stringValue, final int longValue) {
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
        this.stringValue = deserializer.readString("s");
        this.longValue = deserializer.readLong("long");
    }

    /**
     * Gets the int value.
     *
     * @return The int value.
     */
    public int getIntValue() { return this.intValue; }

    /**
     * Gets the String value.
     *
     * @return The String value.
     */
    public String getStringValue() { return this.stringValue; }

    /**
     * Gets the long value.
     *
     * @return The long value.
     */
    public long getLongValue() { return this.longValue; }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeInt("int", this.intValue);
        serializer.writeString("s", this.stringValue);
        serializer.writeLong("long", this.longValue);
    }

    /**
     * ObjectDeserializer implementation that can activate MockSerializableEntity objects.
     */
    public static class Activator implements ObjectDeserializer<MockSerializableEntity> {

        @Override
        public MockSerializableEntity deserialize(final Deserializer deserializer, final DeserializationContext context) {
            return new MockSerializableEntity(deserializer);
        }
    }
}
