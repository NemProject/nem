package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {
	private static final Logger LOGGER = Logger.getLogger(BlockChainValidator.class.getName());

	private final Consumer<Block> executor;
	private final BlockScorer scorer;
	private final int maxChainSize;
	private final BlockValidator blockValidator;
	private final TransactionValidator transactionValidator;
	// TODO 20141030 J-G think about this a bit more
	private final BatchTransactionHashValidator batchTransactionHashValidator;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param executor The block executor to use.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 * @param blockValidator The validator to use for validating blocks.
	 * @param transactionValidator The validator to use for validating transactions.
	 */
	public BlockChainValidator(
			final Consumer<Block> executor,
			final BlockScorer scorer,
			final int maxChainSize,
			final BlockValidator blockValidator,
			final TransactionValidator transactionValidator,
			final BatchTransactionHashValidator batchTransactionHashValidator) {
		this.executor = executor;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.blockValidator = blockValidator;
		this.transactionValidator = transactionValidator;
		this.batchTransactionHashValidator = batchTransactionHashValidator;
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

		final BlockHeight confirmedBlockHeight = parentBlock.getHeight();
		if (duplicateHashExists(blocks, confirmedBlockHeight)) {
			return false;
		}

		final Set<Hash> chainHashes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight()) || !block.verify()) {
				return false;
			}

			if (ValidationResult.SUCCESS != this.blockValidator.validate(block)) {
				return false;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.fine(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return false;
			}

			final ValidationContext validationContext = new ValidationContext(block.getHeight(), confirmedBlockHeight);
			for (final Transaction transaction : block.getTransactions()) {
				if (ValidationResult.SUCCESS != this.transactionValidator.validate(transaction, validationContext) ||
						!transaction.verify() ||
						transaction.getSigner().equals(block.getSigner())) {
					return false;
				}

				final Hash hash = HashUtils.calculateHash(transaction);
				if (chainHashes.contains(hash)) {
					LOGGER.info("received block with duplicate TX");
					return false;
				}

				chainHashes.add(hash);
			}

			parentBlock = block;
			expectedHeight = expectedHeight.next();

			this.executor.accept(block);
		}

		return true;
	}

	private boolean duplicateHashExists(final Collection<Block> blocks, final BlockHeight confirmedBlockHeight) {
		Collection<Transaction> transactions = new ArrayList<>();
		for (final Block block : blocks) {
			for (final Transaction transaction : block.getTransactions()) {
				transactions.add(transaction);
			}
		}

		final ValidationContext validationContext = new ValidationContext(BlockHeight.MAX, confirmedBlockHeight);
		return ValidationResult.SUCCESS != this.batchTransactionHashValidator.validate(transactions, validationContext);
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
