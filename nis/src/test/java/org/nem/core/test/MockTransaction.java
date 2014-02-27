package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

/**
 * A mock Transaction implementation.
 */
public class MockTransaction extends Transaction {

	private int customField;
    private long minimumFee;

    /**
     * Creates a mock transaction.
     *
     * @param sender The transaction sender's account.
     */
    public MockTransaction(final Account sender) {
        this(sender, 0);
    }

    /**
     * Creates a mock transaction.
     *
     * @param sender The transaction sender's account.
     * @param customField The initial custom field value.
     */
    public MockTransaction(final Account sender, final int customField) {
		super(123, 759, sender);
        this.customField = customField;
    }

    /**
     * Deserializes a MockTransaction.
     *
     * @param deserializer The deserializer to use.
     */
    public MockTransaction(final Deserializer deserializer) {
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
     * Sets the minimum fee.
     * @param minimumFee The desired minimum fee.
     */
    public void setMinimumFee(final long minimumFee) { this.minimumFee = minimumFee; }

    @Override
    public boolean isValid() { return true; }

    @Override
    protected long getMinimumFee() {
        return this.minimumFee;
    }

    @Override
    protected void serializeImpl(Serializer serializer) {
        super.serializeImpl(serializer);
        serializer.writeInt("customField", this.customField);
    }

    @Override
    public void execute() { }
}