package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.ObjectSerializer;

public class TransferTransaction extends Transaction {

    private long amount;
    private byte[] message;

    public TransferTransaction(final Account sender, final long amount, final byte[] message) {
        super(1, TransactionTypes.TRANSFER, sender);
        this.amount = amount;
        this.message = message;
    }

    protected void serializeImpl(final ObjectSerializer serializer) throws Exception {
        serializer.writeLong(this.amount);
        if (null != this.message)
            serializer.writeBytes(this.message);
    }
}