package org.nem.core.model;

import java.util.*;

/**
 * A NEM block.
 */
public class Block {
//    this.prevBlockHash = prevBlockHash;
//    this.blockHash = blockHash;
//    this.timestamp = timestamp;
//    this.forger = forger;
//    this.forgerProof = forgerProof;
//    this.blockSignature = blockSignature;
//    this.height = height;
//    this.totalAmount = totalAmount;
//    this.totalFee = totalFee;

    private List<Transaction> transactions;

    /**
     * Creates a new block.
     */
    public Block() {
        this.transactions = new ArrayList<>();
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
}
