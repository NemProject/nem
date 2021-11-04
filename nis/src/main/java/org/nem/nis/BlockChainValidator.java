package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.chain.BlockProcessor;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	private final ValidationState validationState;
	private final ForkConfiguration forkConfiguration;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param processorFactory A factory for creating block processors.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 * @param blockValidator The validator to use for validating blocks.
	 * @param transactionValidator The validator to use for validating transactions.
	 * @param validationState The validation state.
	 * @param forkConfiguration The fork configuration.
	 */
	public BlockChainValidator(final Function<Block, BlockProcessor> processorFactory, final BlockScorer scorer, final int maxChainSize,
			final BlockValidator blockValidator, final SingleTransactionValidator transactionValidator,
			final ValidationState validationState, final ForkConfiguration forkConfiguration) {
		this.processorFactory = processorFactory;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.blockValidator = blockValidator;
		this.transactionValidator = transactionValidator;
		this.validationState = validationState;
		this.forkConfiguration = forkConfiguration;
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

		if (!verifyTransactions(blocks)) {
			LOGGER.info("received block with unverifiable transaction");
			return ValidationResult.FAILURE_TRANSACTION_UNVERIFIABLE;
		}

		final BlockHeight confirmedBlockHeight = parentBlock.getHeight();
		final Set<Hash> chainHashes = new HashSet<>();
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			final BlockProcessor processor = this.processorFactory.apply(block);
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight())) {
				LOGGER.info(String.format("received block at %s with unexpected height (expected %s)", block.getHeight(), expectedHeight));
				return ValidationResult.FAILURE_BLOCK_UNEXPECTED_HEIGHT;
			}

			if (!block.verify()) {
				LOGGER.info(String.format("received unverifiable block at %s", block.getHeight()));
				return ValidationResult.FAILURE_BLOCK_UNVERIFIABLE;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.info(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return ValidationResult.FAILURE_BLOCK_NOT_HIT;
			}

			final ValidationContext context = new ValidationContext(block.getHeight(), confirmedBlockHeight, this.validationState);
			for (final Transaction transaction : block.getTransactions()) {
				final List<Hash> hashes = getHashes(transaction);
				if (hashes.stream().anyMatch(chainHashes::contains)) {
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

			// move block validation last so that we can detect evil peers who send us multiple blocks
			// containing the same transaction (if this is before the chainHashes check, the block validator
			// will reject the block as NEUTRAL)
			final ValidationResult blockValidationResult = this.blockValidator.validate(block);
			if (!blockValidationResult.isSuccess()) {
				LOGGER.info(String.format("received block that failed validation: %s", blockValidationResult));
				return blockValidationResult;
			}

			processor.process();

			parentBlock = block;
			expectedHeight = expectedHeight.next();
		}

		return ValidationResult.SUCCESS;
	}

	private boolean verifyTransactions(final Collection<Block> blocks) {
		return blocks.parallelStream()
				// TreasuryReissuanceForkTransactionBlockValidator ensures fork block contains expected transactions
				.filter(b -> !b.getHeight().equals(this.forkConfiguration.getTreasuryReissuanceForkHeight()))
				.flatMap(b -> b.getTransactions().stream()).allMatch(VerifiableEntity::verify);
	}

	private static List<Hash> getHashes(final Transaction transaction) {
		return TransactionExtensions.streamDefault(transaction).parallel().map(HashUtils::calculateHash).collect(Collectors.toList());
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
