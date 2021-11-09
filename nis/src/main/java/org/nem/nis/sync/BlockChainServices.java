package org.nem.nis.sync;

import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.chain.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.*;
import org.nem.nis.secret.*;
import org.nem.nis.validators.*;
import org.nem.nis.visitors.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;

/**
 * Facade that hides the details of wiring up a number of BlockChain dependencies. This class is intended to hide BlockExecutor and
 * BlockChainValidator from the BlockChain COMPLETELY.
 */
public class BlockChainServices {
	private final BlockDao blockDao;
	private final BlockTransactionObserverFactory observerFactory;
	private final BlockValidatorFactory blockValidatorFactory;
	private final TransactionValidatorFactory transactionValidatorFactory;
	private final NisMapperFactory mapperFactory;
	private final ForkConfiguration forkConfiguration;

	public BlockChainServices(final BlockDao blockDao, final BlockTransactionObserverFactory observerFactory,
			final BlockValidatorFactory blockValidatorFactory, final TransactionValidatorFactory transactionValidatorFactory,
			final NisMapperFactory mapperFactory, final ForkConfiguration forkConfiguration) {
		this.blockDao = blockDao;
		this.observerFactory = observerFactory;
		this.blockValidatorFactory = blockValidatorFactory;
		this.transactionValidatorFactory = transactionValidatorFactory;
		this.mapperFactory = mapperFactory;
		this.forkConfiguration = forkConfiguration;
	}

	/**
	 * Creates a NIS db model to model mapper that uses the specified account lookup.
	 *
	 * @param accountLookup The account lookup.
	 * @return The mapper.
	 */
	public NisDbModelToModelMapper createMapper(final AccountLookup accountLookup) {
		return this.mapperFactory.createDbModelToModelNisMapper(accountLookup);
	}

	/**
	 * Compares the peer chain to the local chain.
	 *
	 * @param nisCache The current NIS cache.
	 * @param parentBlock The parent block.
	 * @param peerChain The peer chain.
	 * @return true if the peer chain is valid; false otherwise.
	 */
	public ValidationResult isPeerChainValid(final NisCache nisCache, final Block parentBlock, final Collection<Block> peerChain) {
		final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final BlockScorer scorer = new BlockScorer(accountStateCache);
		this.calculatePeerChainDifficulties(parentBlock, peerChain, scorer);

		final ComparisonContext comparisonContext = new DefaultComparisonContext(parentBlock.getHeight());
		final BlockTransactionObserver observer = this.observerFactory.createExecuteCommitObserver(nisCache);

		final BlockChainValidator validator = new BlockChainValidator(block -> new BlockExecuteProcessor(nisCache, block, observer), scorer,
				comparisonContext.getMaxNumBlocksToAnalyze(), this.blockValidatorFactory.create(nisCache),
				this.transactionValidatorFactory.createSingle(nisCache), NisCacheUtils.createValidationState(nisCache),
				this.forkConfiguration);
		return validator.isValid(parentBlock, peerChain);
	}

	/**
	 * Undoes all transactions up until the common block height and returns the score.
	 *
	 * @param nisCache The current NIS cache.
	 * @param localBlockLookup The local block lookup adapter.
	 * @param commonBlockHeight The common block height (i.e. the height at which the undo should stop).
	 * @return The score of the undone transactions.
	 */
	public BlockChainScore undoAndGetScore(final NisCache nisCache, final BlockLookup localBlockLookup,
			final BlockHeight commonBlockHeight) {
		final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final BlockScorer scorer = new BlockScorer(accountStateCache);
		final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(scorer);

		final List<BlockVisitor> visitors = new ArrayList<>();
		visitors.add(new UndoBlockVisitor(this.observerFactory.createUndoCommitObserver(nisCache), new BlockExecutor(nisCache)));
		visitors.add(scoreVisitor);
		final BlockVisitor visitor = new AggregateBlockVisitor(visitors);
		BlockIterator.unwindUntil(localBlockLookup, commonBlockHeight, visitor);
		accountStateCache.undoVesting(commonBlockHeight);
		return scoreVisitor.getScore();
	}

	/**
	 * This function prepares the peer chain for validation by recalculating all block difficulties (we don't trust the peer).
	 *
	 * @param parentBlock The parent block.
	 * @param peerChain The peer chain.
	 * @param blockScorer The block scorer that should be used in the difficulty calculation.
	 */
	private void calculatePeerChainDifficulties(final Block parentBlock, final Collection<Block> peerChain, final BlockScorer blockScorer) {
		final long blockDifference = parentBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1;
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, blockDifference));

		final int limit = (int) Math.min(parentBlock.getHeight().getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
		final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);

		for (final Block block : peerChain) {
			final BlockDifficulty difficulty = blockScorer.getDifficultyScorer().calculateDifficulty(difficulties, timeStamps);
			block.setDifficulty(difficulty);

			// apache collections4 only have CircularFifoQueue which as a queue doesn't have .get()
			difficulties.add(difficulty);
			timeStamps.add(block.getTimeStamp());
			if (difficulties.size() > BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION) {
				difficulties.remove(0);
				timeStamps.remove(0);
			}
		}
	}
}
