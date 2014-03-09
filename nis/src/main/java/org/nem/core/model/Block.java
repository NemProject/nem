package org.nem.core.model;

import org.nem.core.crypto.Hashes;
import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;

import java.util.*;

/**
 * A NEM block.
 */
public class Block extends VerifiableEntity {
	private byte[] prevBlockHash;
	private int timestamp; // in seconds, max 68 years...
	// forger == signer
	// forgerProof = signature
	private byte[] generationSignature; // hash(prevBlockHash || signer pubKey)
	private int height; // unsure yet, but probably will be easier to talk on forums having that

	// I think it can be worth to keep fee here, discrepancies
	// might be one more 'input' for trust
	private long totalFee;

    private final List<Transaction> transactions;

    /**
     * Creates a new block.
     *
     * @param forger The forger.
     */
    public Block(final Account forger, final byte[] prevBlockHash, int timestamp, int height) {
        super(1, 1, forger);
        this.transactions = new ArrayList<>();
		this.prevBlockHash = prevBlockHash;
		this.timestamp = timestamp;
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

		this.prevBlockHash= deserializer.readBytes("prevBlockHash");
		this.timestamp    = deserializer.readInt("timestamp");
		this.height       = deserializer.readInt("height");
		this.totalFee     = deserializer.readLong("totalFee");

		this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.VERIFIABLE);
    }

    /**
     * Gets the transactions associated with this block.
     *
     * @return The transactions associated with this block.
     */
    public List<Transaction> getTransactions() {
        return this.transactions;
    }

	/**
	 * Gets the timestamp of this block since NEM epoch.
	 *
	 * @return timestamp of this block since NEM epoch.
	 */
	public int getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the height of this block in the blockchain.
	 *
	 * @return height of this block in the blockchain.
	 */
	public int getHeight() {
		return height;
	}


	/**
	 * Gets total amount of fees of all transactions stored in this block.
	 *
     * @return total amount of fees of all transactions stored in this block.
     */
    public long getTotalFee() {
//        long fee = 0;
//        for (Transaction transaction : this.transactions)
//            fee += transaction.getFee();
//
//        return fee;
		return this.totalFee;
    }

    /**
     * Adds a new transaction to this block.
     *
     * @param transaction The transaction to add.
     */
    public void addTransaction(final Transaction transaction) {
        this.transactions.add(transaction);

		this.totalFee += transaction.getFee();
    }

    @Override
    protected void serializeImpl(Serializer serializer) {
		serializer.writeBytes("prevBlockHash", this.prevBlockHash);

		serializer.writeInt("timestamp", this.timestamp);
		serializer.writeInt("height", this.height);
		serializer.writeLong("totalFee", this.totalFee);

		serializer.writeObjectArray("transactions", this.transactions);
    }
}
