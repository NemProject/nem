package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.chain.BlockProcessor;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.*;

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {
	private static final Logger LOGGER = Logger.getLogger(BlockChainValidator.class.getName());

	private final Function<Block, BlockProcessor> processorFactory;
	private final BlockScorer scorer;
	private final int maxChainSize;
	private final BlockValidator blockValidator;
	private final SingleTransactionValidator transactionValidator;
	private final DebitPredicate debitPredicate;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param processorFactory A factory for creating block processors.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 * @param blockValidator The validator to use for validating blocks.
	 * @param transactionValidator The validator to use for validating transactions.
	 * @param debitPredicate The debit predicate to use for validating transactions.
	 */
	public BlockChainValidator(
			final Function<Block, BlockProcessor> processorFactory,
			final BlockScorer scorer,
			final int maxChainSize,
			final BlockValidator blockValidator,
			final SingleTransactionValidator transactionValidator,
			final DebitPredicate debitPredicate) {
		this.processorFactory = processorFactory;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.blockValidator = blockValidator;
		this.transactionValidator = transactionValidator;
		this.debitPredicate = debitPredicate;
	}

	/**
	 * Determines if blocks is a valid block chain given blocks and parentBlock.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 * @return The validation result.
	 */
	public ValidationResult isValid(Block parentBlock, final Collection<Block> blocks) {
		if (blocks.size() > this.maxChainSize) {
			LOGGER.info("received chain with too many blocks");
			return ValidationResult.FAILURE_MAX_CHAIN_SIZE_EXCEEDED;
		}

		final BlockHeight confirmedBlockHeight = parentBlock.getHeight();
		final Set<Hash> chainHashes = new HashSet<>();
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			final BlockProcessor processor = this.processorFactory.apply(block);
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight())) {
				LOGGER.info("received block with unexpected height");
				return ValidationResult.FAILURE_BLOCK_UNEXPECTED_HEIGHT;
			}

			if (!block.verify()) {
				LOGGER.info("received unverifiable block");
				return ValidationResult.FAILURE_BLOCK_UNVERIFIABLE;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.info(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return ValidationResult.FAILURE_BLOCK_NOT_HIT;
			}

			final ValidationResult blockValidationResult = this.blockValidator.validate(block);
			if (!blockValidationResult.isSuccess()) {
				LOGGER.info(String.format("received block that failed validation: %s", blockValidationResult));
				return blockValidationResult;
			}

			final ValidationContext context = new ValidationContext(block.getHeight(), confirmedBlockHeight, this.debitPredicate);
			for (final Transaction transaction : block.getTransactions()) {
				if (!transaction.verify()) {
					LOGGER.info("received block with unverifiable transaction");
					return ValidationResult.FAILURE_TRANSACTION_UNVERIFIABLE;
				}

				final List<Hash> hashes = getHashes(transaction);
				if (hashes.stream().anyMatch(chainHashes::contains) ){
					LOGGER.info("received block with duplicate transaction");
					return ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN;
				}

				final ValidationResult transactionValidationResult = this.transactionValidator.validate(transaction, context);
				if (!transactionValidationResult.isSuccess()) {
					LOGGER.info(String.format("received transaction that failed validation: %s", transactionValidationResult));
					return transactionValidationResult;
				}

				processor.process(transaction);
				chainHashes.addAll(hashes);
			}

			processor.process();

			parentBlock = block;
			expectedHeight = expectedHeight.next();
		}

		return ValidationResult.SUCCESS;
	}

	private static List<Hash> getHashes(final Transaction transaction) {
		return TransactionExtensions.streamDefault(transaction).map(HashUtils::calculateHash).collect(Collectors.toList());
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
