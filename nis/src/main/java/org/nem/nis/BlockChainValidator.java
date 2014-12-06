package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.*;
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
	private final SingleTransactionValidator transactionValidator;
	private final BatchTransactionValidator batchTransactionValidator;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param executor The block executor to use.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 * @param blockValidator The validator to use for validating blocks.
	 * @param transactionValidator The validator to use for validating transactions.
	 * @param batchTransactionValidator The validator to use for validating transactions in batches.
	 */
	public BlockChainValidator(
			final Consumer<Block> executor,
			final BlockScorer scorer,
			final int maxChainSize,
			final BlockValidator blockValidator,
			final SingleTransactionValidator transactionValidator,
			final BatchTransactionValidator batchTransactionValidator) {
		this.executor = executor;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.blockValidator = blockValidator;
		this.transactionValidator = transactionValidator;
		this.batchTransactionValidator = batchTransactionValidator;
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
			LOGGER.info("received chain with too many blocks");
			return false;
		}

		final BlockHeight confirmedBlockHeight = parentBlock.getHeight();
		final Set<Hash> chainHashes = new HashSet<>();
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight())) {
				LOGGER.info("received block with unexpected height");
				return false;
			}

			if (!block.verify()) {
				LOGGER.info("received unverifiable block");
				return false;
			}

			final ValidationResult blockValidationResult = this.blockValidator.validate(block);
			if (!blockValidationResult.isSuccess()) {
				LOGGER.info(String.format("received block that failed validation: %s", blockValidationResult));
				return false;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.info(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return false;
			}

			final ValidationContext context = new ValidationContext(block.getHeight(), confirmedBlockHeight);
			// TODO 20141205 J-B: why did you change how batch validation is happening?
			// > if you are doing it this way, we can simplify everything and just have this check as a block validator!
			// > (this is also causing the three remaining test failures)
			// TODO 20141206 BR -> J: executing a block adds the transaction hashes to the cache. Therefore we can't wait with validation till the end
			// > because it would fail. i agree that we should have a block validator for it.
			final ValidationResult batchTransactionValidationResult =
					this.batchTransactionValidator.validate(Arrays.asList(new TransactionsContextPair(block.getTransactions(), context)));
			if (!batchTransactionValidationResult.isSuccess()) {
				LOGGER.info(String.format("received transaction that failed validation: %s", batchTransactionValidationResult));
				return false;
			}

			for (final Transaction transaction : block.getTransactions()) {
				if (!transaction.verify()) {
					LOGGER.info("received block with unverifiable transaction");
					return false;
				}

				final Hash hash = HashUtils.calculateHash(transaction);
				if (chainHashes.contains(hash)) {
					LOGGER.info("received block with duplicate transaction");
					return false;
				}

				final ValidationResult transactionValidationResult = this.transactionValidator.validate(transaction, context);
				if (!transactionValidationResult.isSuccess()) {
					LOGGER.info(String.format("received transaction that failed validation: %s", transactionValidationResult));
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

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
