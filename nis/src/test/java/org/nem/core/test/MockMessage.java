package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * A mock Message implementation.
 */
public class MockMessage extends Message {

    public static final int TYPE = 19;

    private int customField;

    /**
     * Creates a mock message.
     *
     * @param customField The initial custom field value.
     */
    public MockMessage(final int customField) {
        super(TYPE);
        this.customField = customField;
    }

    /**
     * Deserializes a mock verifiable entity.
     *
     * @param deserializer The deserializer to use.
     */
    public MockMessage(final Deserializer deserializer) {
        super(deserializer.readInt("type"));
        this.customField = deserializer.readInt("customField");
    }

    /**
     * Gets the custom field value.
     *
     * @return The custom field value.
     */
    public int getCustomField() { return this.customField; }

    @Override
    public boolean canDecode() { return true; }

    @Override
    public byte[] getEncodedPayload() { return null; }

    @Override
    public byte[] getDecodedPayload() { return null; }

    @Override
    public void serialize(final Serializer serializer) {
        super.serialize(serializer);
        serializer.writeInt("customField", this.customField);
    }
}