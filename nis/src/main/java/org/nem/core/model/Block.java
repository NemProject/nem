package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A NEM block.
 * <p/>
 * The forger is an alias for the signer.
 * The forger proof is the signature.
 */
public class Block extends VerifiableEntity {

	private final static int BLOCK_VERSION = 1;

	private final BlockHeight height;
	private Hash prevBlockHash;

	private final List<Transaction> transactions;

	// these are helper fields and shouldn't be serialized
	private Account lessor;
	private BlockDifficulty difficulty;

	private Hash generationHash;

	/**
	 * Creates a new block.
	 *
	 * @param forger The forger.
	 * @param prevBlockHash The hash of the previous block.
	 * @param generationHash The generation hash.
	 * @param timeStamp The block timestamp.
	 * @param height The block height.
	 */
	public Block(
			final Account forger,
			final Hash prevBlockHash,
			final Hash generationHash,
			final TimeInstant timeStamp,
			final BlockHeight height) {
		super(BlockTypes.REGULAR, BLOCK_VERSION, timeStamp, forger);
		this.transactions = new ArrayList<>();
		this.prevBlockHash = prevBlockHash;
		this.generationHash = generationHash;
		this.height = height;

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;
	}

	/**
	 * Creates a new block.
	 *
	 * @param forger The forger.
	 * @param prevBlock The previous block.
	 * @param timeStamp The block timestamp.
	 */
	public Block(final Account forger, final Block prevBlock, final TimeInstant timeStamp) {
		this(forger, Hash.ZERO, Hash.ZERO, timeStamp, prevBlock.getHeight().next());
		this.setPrevious(prevBlock);
	}

	/**
	 * Deserializes a new block.
	 *
	 * @param type The block type.
	 * @param options The deserializer options.
	 * @param deserializer The deserializer to use.
	 */
	public Block(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);

		this.prevBlockHash = deserializer.readObject("prevBlockHash", Hash.DESERIALIZER);
		this.height = BlockHeight.readFrom(deserializer, "height");

		this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.VERIFIABLE);

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;
	}

	//region Getters

	/**
	 * Gets the height of this block in the block chain.
	 *
	 * @return The height of this block in the block chain.
	 */
	public BlockHeight getHeight() {
		return this.height;
	}

	/**
	 * Gets total amount of fees of all transactions stored in this block.
	 *
	 * @return The total amount of fees of all transactions stored in this block.
	 */
	public Amount getTotalFee() {
		final long rawTotalFee = this.transactions.stream()
				.map(tx -> tx.getFee().getNumMicroNem())
				.reduce(0L, Long::sum);
		return Amount.fromMicroNem(rawTotalFee);
	}

	/**
	 * Gets the hash of the previous block.
	 *
	 * @return The hash of the previous block.
	 */
	public Hash getPreviousBlockHash() {
		return this.prevBlockHash;
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
	 * Gets the difficulty associated with this block.
	 *
	 * @return Difficulty of this block.
	 */
	public BlockDifficulty getDifficulty() {
		return this.difficulty;
	}

	/**
	 * Gets the generation hash associated with this block.
	 *
	 * @return Generation hash of this block.
	 */
	public Hash getGenerationHash() {
		return this.generationHash;
	}

	/**
	 * Gets the lessor if block has been harvested by lessee.
	 *
	 * @return Lessor or null.
	 */
	public Account getLessor() {
		return this.lessor;
	}
	//endregion

	//region Setters

	/**
	 * Sets the previous block.
	 *
	 * @param prevBlock The previous block.
	 */
	public void setPrevious(final Block prevBlock) {
		this.setPreviousGenerationHash(prevBlock.getGenerationHash());
		this.prevBlockHash = HashUtils.calculateHash(prevBlock);
	}

	/**
	 * Sets the previous generation hash.
	 *
	 * @param previousGenerationHash The previous generation hash.
	 */
	public void setPreviousGenerationHash(final Hash previousGenerationHash) {
		this.generationHash = HashUtils.nextHash(
				previousGenerationHash,
				this.getSigner().getKeyPair().getPublicKey());
	}

	/**
	 * Sets the generation hash.
	 *
	 * @param generationHash The generation hash.
	 */
	protected void setGenerationHash(final Hash generationHash) {
		this.generationHash = generationHash;
	}

	/**
	 * Sets the difficulty.
	 *
	 * @param difficulty The difficulty.
	 */
	public void setDifficulty(final BlockDifficulty difficulty) {
		this.difficulty = difficulty;
	}


	public void setLessor(final Account lessor) {
		this.lessor = lessor;
	}
	// endregion

	/**
	 * Adds a new transaction to this block.
	 *
	 * @param transaction The transaction to add.
	 */
	public void addTransaction(final Transaction transaction) {
		this.transactions.add(transaction);
	}

	/**
	 * Adds new transactions to this block.
	 *
	 * @param transactions The transactions to add.
	 */
	public void addTransactions(final Collection<Transaction> transactions) {
		transactions.forEach(tx -> this.addTransaction(tx));
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		serializer.writeObject("prevBlockHash", this.prevBlockHash);
		BlockHeight.writeTo(serializer, "height", this.height);

		serializer.writeObjectArray("transactions", this.transactions);
	}

	@Override
	public String toString() {
		return String.format("height: %d, #tx: %d", this.height.getRaw(), this.transactions.size());
	}
}
