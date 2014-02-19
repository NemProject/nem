package org.nem.core.model;

import java.util.*;

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

    public Block() {
        this.transactions = new ArrayList<>();
    }

    public long getTotalFee() {
        long fee = 0;
        for (Transaction transaction : this.transactions)
            fee += transaction.getFee();

        return fee;
    }

    public void addTransaction(final Transaction transaction) {
        this.transactions.add(transaction);
    }
}
