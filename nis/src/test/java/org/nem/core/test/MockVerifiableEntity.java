package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * A mock VerifiableEntity implementation.
 */
public class MockVerifiableEntity extends VerifiableEntity {

	private int customField;

    /**
     * Creates a mock verifiable entity.
     *
     * @param signer The owner's account.
     */
    public MockVerifiableEntity(final Account signer) {
        this(signer, 0);
    }

    /**
     * Creates a mock verifiable entity.
     *
     * @param signer The owner's account.
     * @param customField The initial custom field value.
     */
    public MockVerifiableEntity(final Account signer, final int customField) {
		super(11, 23, signer);
        this.customField = customField;
    }

    /**
     * Deserializes a mock verifiable entity.
     *
     * @param deserializer The deserializer to use.
     */
    public MockVerifiableEntity(final Deserializer deserializer) {
        super(deserializer.readInt("type"), deserializer);
        this.customField = deserializer.readInt("customField");
    }

    /**
     * Gets the custom field value.
     *
     * @return The custom field value.
     */
    public int getCustomField() { return this.customField; }

    /**
     * Sets the custom field value.
     *
     * @param customField The desired custom field value.
     */
    public void setCustomField(final int customField) { this.customField = customField; }

    @Override
    protected void serializeImpl(Serializer serializer) {
        serializer.writeInt("customField", this.customField);
    }
}