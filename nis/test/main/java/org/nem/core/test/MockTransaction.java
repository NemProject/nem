package org.nem.core.test;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

public class MockTransaction extends Transaction {

    private int customField;
    private long minimumFee;

    public MockTransaction(final Account sender) throws Exception {
        this(sender, 0);
    }

    public MockTransaction(final Account sender, final int customField) throws Exception {
        super(123, 759, sender);
        this.customField = customField;
    }

    public MockTransaction(final ObjectDeserializer deserializer) throws Exception {
        super(deserializer.readInt("type"), deserializer);
        this.customField = deserializer.readInt("customField");
    }

    public int getCustomField() { return this.customField; }
    public void setCustomField(final int customField) { this.customField = customField; }

    public void setMinimumFee(final long minimumFee) { this.minimumFee = minimumFee; }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    protected long getMinimumFee() {
        return this.minimumFee;
    }

    @Override
    protected void serializeImpl(ObjectSerializer serializer) throws Exception {
        serializer.writeInt("customField", this.customField);
    }
}