package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

public class TransferTransaction extends Transaction {
    private static final int MAX_MESSAGE_SIZE = 1000;

    private long amount;
    private byte[] message;

    public TransferTransaction(final Account sender, final long amount, final byte[] message) {
        super(TransactionTypes.TRANSFER, 1, sender);
        this.amount = amount;
        this.message = null == message ? new byte[] { } : message;
    }

    public TransferTransaction(final ObjectDeserializer deserializer) throws Exception {
        super(TransactionTypes.TRANSFER, deserializer);
        this.amount = deserializer.readLong("amount");
        this.message = deserializer.readBytes("message");
    }

    public long getAmount() { return this.amount; }
    public byte[] getMessage() { return this.message; }

    @Override
    public boolean isValid() {
        return message.length <= MAX_MESSAGE_SIZE;
    }

    @Override
    protected long getMinimumFee() {
        long amountFee = (long)Math.ceil(this.amount * 0.001);
        long messageFee = (long)Math.ceil(this.message.length * 0.005);
        return amountFee + messageFee;
    }

    @Override
    protected void serializeImpl(final ObjectSerializer serializer) throws Exception {
        serializer.writeLong("amount", this.amount);
        serializer.writeBytes("message", this.message);
    }
}
