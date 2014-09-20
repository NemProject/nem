package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;
import org.nem.nis.validators.TransactionValidator;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.logging.Logger;

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {
	private static final Logger LOGGER = Logger.getLogger(BlockChainValidator.class.getName());
	private static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 60;

	private final Consumer<Block> executor;
	private final BlockScorer scorer;
	private final int maxChainSize;
	private final Predicate<Hash> transactionExists;
	private final TransactionValidator validator;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param executor The block executor to use.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 * @param transactionExists Predicate that returns true if a transaction with the same hash already exists in the database.
	 * @param validator The validator to use for validating transactions.
	 */
	public BlockChainValidator(
			final Consumer<Block> executor,
			final BlockScorer scorer,
			final int maxChainSize,
			final Predicate<Hash> transactionExists,
			final TransactionValidator validator) {
		this.executor = executor;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.transactionExists = transactionExists;
		this.validator = validator;
	}

	/**
	 * Determines if blocks is a valid block chain given blocks and parentBlock.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 * @return true if the blocks are valid.
	 */
	public boolean isValid(Block parentBlock, final Collection<Block> blocks) {
		if (blocks.size() > this.maxChainSize) {
			return false;
		}

		final Set<Hash> chainHashes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight()) || !block.verify()) {
				return false;
			}

			// TODO-CR: not sure if i like having a hard dependency on NisMain here instead of injecting the TimeProvider
			final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
			if (block.getTimeStamp().compareTo(currentTime.addSeconds(MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0) {
				return false;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.fine(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return false;
			}

			for (final Transaction transaction : block.getTransactions()) {
				if (ValidationResult.SUCCESS != this.validator.validate(transaction) ||
						!transaction.verify() ||
						transaction.getTimeStamp().compareTo(currentTime.addSeconds(MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0 ||
						transaction.getSigner().equals(block.getSigner())) {
					return false;
				}

				if (block.getHeight().getRaw() >= BlockMarkerConstants.FATAL_TX_BUG_HEIGHT) {
					final Hash hash = HashUtils.calculateHash(transaction);
					if (this.transactionExists.test(hash) || chainHashes.contains(hash)) {
						LOGGER.info("received block with duplicate TX");
						return false;
					}
					chainHashes.add(hash);
				}
			}

			parentBlock = block;
			expectedHeight = expectedHeight.next();

			this.executor.accept(block);
		}

		return true;
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
