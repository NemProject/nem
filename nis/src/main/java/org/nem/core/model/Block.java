package org.nem.core.model;

import org.nem.core.serialization.ObjectDeserializer;
import org.nem.core.serialization.ObjectSerializer;

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

    private final List<Transaction> transactions = new ArrayList<>();

    /**
     * Creates a new block.
     *
     * @param forger The forger.
     */
    public Block(final Account forger) {
        super(1, 1, forger);
    }

    /**
     * Deserializes a new block.
     *
     * @param type The block type.
     * @param deserializer The deserializer to use.
     */
    public Block(final int type, final ObjectDeserializer deserializer) {
        super(type, deserializer);
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
    protected void serializeImpl(ObjectSerializer serializer) {
        // TODO: serialize block fields
    }
}