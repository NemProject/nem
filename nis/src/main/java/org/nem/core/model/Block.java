package org.nem.core.model;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.nem.core.crypto.Hash;
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

	private final static int BLOCK_TYPE = 1;
	private final static int BLOCK_VERSION = 1;

	private final BlockHeight height;
	private Hash prevBlockHash;

	private final List<Transaction> transactions;
	private final List<BlockTransferObserver> blockTransferObservers = new ArrayList<>();

	// these are helper fields and shouldn't be serialized
	private BlockDifficulty difficulty;

	private Hash generationHash;

	/**
	 * Creates a new block.
	 *
	 * @param forger         The forger.
	 * @param prevBlockHash  The hash of the previous block.
	 * @param generationHash The generation hash.
	 * @param timestamp      The block timestamp.
	 * @param height         The block height.
	 */
	public Block(
			final Account forger,
			final Hash prevBlockHash,
			final Hash generationHash,
			final TimeInstant timestamp,
			final BlockHeight height) {
		super(BLOCK_TYPE, BLOCK_VERSION, timestamp, forger);
		this.transactions = new ArrayList<>();
		this.prevBlockHash = prevBlockHash;
		this.generationHash = generationHash;
		this.height = height;

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;

		this.blockTransferObservers.add(new WeightedBalancesObserver());
	}

	/**
	 * Creates a new block.
	 *
	 * @param forger    The forger.
	 * @param prevBlock The previous block.
	 * @param timestamp The block timestamp.
	 */
	public Block(final Account forger, final Block prevBlock, final TimeInstant timestamp) {
		this(forger, Hash.ZERO, Hash.ZERO, timestamp, prevBlock.getHeight().next());
		this.setPrevious(prevBlock);
	}

	/**
	 * Deserializes a new block.
	 *
	 * @param type         The block type.
	 * @param deserializer The deserializer to use.
	 */
	public Block(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);

		this.prevBlockHash = deserializer.readObject("prevBlockHash", Hash.DESERIALIZER);
		this.height = BlockHeight.readFrom(deserializer, "height");

		this.transactions = deserializer.readObjectArray("transactions", TransactionFactory.VERIFIABLE);

		this.difficulty = BlockDifficulty.INITIAL_DIFFICULTY;
		this.blockTransferObservers.add(new WeightedBalancesObserver());
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
	public Hash getGenerationHash() { return this.generationHash; }

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
		transactions.forEach(this::addTransaction);
	}

	/**
	 * Executes all transactions in the block.
	 */
	public void execute() {
		final TransferObserver observer = this.createTransferObserver(true);

		for (final Transaction transaction : this.transactions) {
			transaction.execute();
			transaction.execute(observer);
		}

		final Account signer = this.getSigner();
		signer.incrementForagedBlocks();
		signer.incrementBalance(this.getTotalFee());
		observer.notifyCredit(this.getSigner(), this.getTotalFee());
	}

	/**
	 * Undoes all transactions in the block.
	 */
	public void undo() {
		final TransferObserver observer = this.createTransferObserver(false);

		final Account signer = this.getSigner();
		observer.notifyDebit(this.getSigner(), this.getTotalFee());
		signer.decrementForagedBlocks();
		signer.decrementBalance(this.getTotalFee());

		for (final Transaction transaction : this.getReverseTransactions()) {
			transaction.undo(observer);
			transaction.undo();
		}
	}

	private Iterable<Transaction> getReverseTransactions() {
		return () -> new ReverseListIterator<>(transactions);
	}

	private TransferObserver createTransferObserver(final boolean isExecute) {
		final TransferObserver aggregateObserver = new AggregateBlockTransferObserverToTransferObserverAdapter(
				this.blockTransferObservers,
				this.height,
				isExecute);
		final TransferObserver outlinkObserver = new OutlinkObserver(this.height, isExecute);
		return new AggregateTransferObserver(Arrays.asList(aggregateObserver, outlinkObserver));
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

	/**
	 * Subscribes the observer to transfers initiated by this block.
	 *
	 * @param observer The observer.
	 */
	public void subscribe(final BlockTransferObserver observer) {
		this.blockTransferObservers.add(observer);
	}

	/**
	 * Unsubscribes the observer from transfers initiated by this block.
	 *
	 * @param observer The observer.
	 */
	public void unsubscribe(final BlockTransferObserver observer) {
		this.blockTransferObservers.remove(observer);
	}
}
