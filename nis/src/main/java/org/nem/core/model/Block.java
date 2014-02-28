package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;

import java.util.*;

/**
 * A NEM block.
 */
public class Block extends VerifiableEntity {
//    this.prevBlockHash = prevBlockHash;
//    this.blockHash = blockHash;
//    this.timestamp = timestamp;
//    this.forger = forger;
//    this.forgerProof = forgerProof;
//    this.blockSignature = blockSignature;
//    this.height = height;
//    this.totalAmount = totalAmount;
//    this.totalFee = totalFee;

    private final List<Transaction> transactions;

    /**
     * Creates a new block.
     *
     * @param forger The forger.
     */
    public Block(final Account forger) {
        super(1, 1, forger);
        this.transactions = new ArrayList<>();
    }

    /**
     * Deserializes a new block.
     *
     * @param type The block type.
     * @param deserializer The deserializer to use.
     */
    public Block(final int type, final Deserializer deserializer) {
        super(type, deserializer);
        this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.DESERIALIZER);
    }

    /**
     * Gets the transactions associated with this block.
     *
     * @return The transactions associated with this block.
     */
    List<Transaction> getTransactions() {
        return this.transactions;
    }

    /**
     * Calculates the total fee of all transactions stored in this block.
     *
     * @return The total fee of all transactions stored in this block.
     */
    public long getTotalFee() {
        long fee = 0;
        for (Transaction transaction : this.transactions)
            fee += transaction.getFee();

        return fee;
    }

    /**
     * Adds a new transaction to this block.
     *
     * @param transaction The transaction to add.
     */
    public void addTransaction(final Transaction transaction) {
        this.transactions.add(transaction);
    }

    @Override
    protected void serializeImpl(Serializer serializer) {
        serializer.writeObjectArray("transactions", this.transactions);
    }
}
