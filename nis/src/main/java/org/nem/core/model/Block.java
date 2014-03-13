package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;

import java.util.*;

/**
 * A NEM block.
 *
 * The forger is an alias for the signer.
 * The forger proof is the signature.
 */
public class Block extends VerifiableEntity {

    private final static int BLOCK_TYPE = 1;
    private final static int BLOCK_VERSION = 1;

	private final byte[] prevBlockHash;
	private long height; // unsure yet, but probably will be easier to talk on forums having that
	private long totalFee;

    private final List<Transaction> transactions;

    /**
     * Creates a new block.
     *
     * @param forger The forger.
     * @param prevBlockHash The hash of the previous block.
     * @param timestamp The block timestamp.
     * @param height The block height.
     */
    public Block(final Account forger, final byte[] prevBlockHash, int timestamp, final long height) {
        super(BLOCK_TYPE, BLOCK_VERSION, timestamp, forger);
        this.transactions = new ArrayList<>();
		this.prevBlockHash = prevBlockHash;
		this.height = height;
		this.totalFee = 0;
    }

    /**
     * Deserializes a new block.
     *
     * @param type The block type.
     * @param deserializer The deserializer to use.
     */
    public Block(final int type, final DeserializationOptions options, final Deserializer deserializer) {
        super(type, options, deserializer);

		this.prevBlockHash = deserializer.readBytes("prevBlockHash");
		this.height = deserializer.readLong("height");
		this.totalFee = deserializer.readLong("totalFee");

		this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.VERIFIABLE);
    }

    //region Getters

	/**
	 * Gets the height of this block in the block chain.
	 *
	 * @return The height of this block in the block chain.
	 */
	public long getHeight() {
		return height;
	}

	/**
	 * Gets total amount of fees of all transactions stored in this block.
	 *
     * @return The total amount of fees of all transactions stored in this block.
     */
    public long getTotalFee() { return this.totalFee; }

	/**
     * Gets the hash of the previous block.
     *
     * @return The hash of the previous block.
     */
    public byte[] getPreviousBlockHash() { return this.prevBlockHash; }


    /**
     * Gets the transactions associated with this block.
     *
     * @return The transactions associated with this block.
     */
    public List<Transaction> getTransactions() { return this.transactions; }

    //endregion

    /**
     * Adds a new transaction to this block.
     *
     * @param transaction The transaction to add.
     */
    public void addTransaction(final Transaction transaction) {
        this.transactions.add(transaction);
		this.totalFee += transaction.getFee();
    }

    /**
     * Adds new transactions to this block.
     *
     * @param transactions The transactions to add.
     */
	public void addTransactions(final List<Transaction> transactions) {
        for (final Transaction transaction : transactions)
            this.addTransaction(transaction);
	}

    /**
     * Determines if this block is valid.
     *
     * @return true if this block is valid.
     */
	public boolean isValid() {
		return this.getTimeStamp() >= 0;
	}

    @Override
    protected void serializeImpl(Serializer serializer) {
		serializer.writeBytes("prevBlockHash", this.prevBlockHash);
		serializer.writeLong("height", this.height);
		serializer.writeLong("totalFee", this.totalFee);

		serializer.writeObjectArray("transactions", this.transactions);
    }
}
