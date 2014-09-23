package org.nem.nis;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dao.*;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.secret.*;
import org.nem.nis.service.BlockExecutor;
import org.nem.nis.sync.BlockLookup;
import org.nem.nis.validators.*;
import org.nem.nis.visitors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

// TODO 20140920 J-* this class needs tests!!!

/**
 * Facade that hides the details of wiring up a number of BlockChain dependencies.
 *
 * This class is intended to hide BlockExecutor and BlockChainValidator from the BlockChain COMPLETELY.
 */
@Service
public class BlockChainServices {
	private final BlockDao blockDao;
	private final BlockTransactionObserverFactory observerFactory;
	private final BlockValidatorFactory blockValidatorFactory;
	private final TransactionValidatorFactory transactionValidatorFactory;

	@Autowired(required = true)
	public BlockChainServices(
			final BlockDao blockDao,
			final BlockTransactionObserverFactory observerFactory,
			final BlockValidatorFactory blockValidatorFactory,
			final TransactionValidatorFactory transactionValidatorFactory) {
		this.blockDao = blockDao;
		this.observerFactory = observerFactory;
		this.blockValidatorFactory = blockValidatorFactory;
		this.transactionValidatorFactory = transactionValidatorFactory;
	}

	/**
	 * Compares the peer chain to the local chain.
	 *
	 * @param accountAnalyzer The current account analyzer.
	 * @param parentBlock The parent block.
	 * @param peerChain The peer chain.
	 * @return true if the peer chain is valid; false otherwise.
	 */
	public boolean isPeerChainValid(
			final AccountAnalyzer accountAnalyzer,
			final Block parentBlock,
			final Collection<Block> peerChain)  {
		final PoiFacade poiFacade = accountAnalyzer.getPoiFacade();
		final BlockScorer scorer = new BlockScorer(poiFacade);
		this.calculatePeerChainDifficulties(parentBlock, peerChain, scorer);

		final BlockExecutor executor = new BlockExecutor(poiFacade, accountAnalyzer.getAccountCache());
		final BlockChainValidator validator = new BlockChainValidator(
				block -> executor.execute(block, this.observerFactory.createExecuteCommitObserver(accountAnalyzer)),
				scorer,
				BlockChainConstants.BLOCKS_LIMIT,
				this.blockValidatorFactory.create(poiFacade),
				this.transactionValidatorFactory.create(poiFacade));
		return validator.isValid(parentBlock, peerChain);
	}

	/**
	 * Undoes all transactions up until the common block height and returns the score.
	 *
	 * @param accountAnalyzer The current account analyzer.
	 * @param localBlockLookup The local block lookup adapter.
	 * @param commonBlockHeight The common block height (i.e. the height at which the undo should stop).
	 * @return The score of the undone transactions.
	 */
	public BlockChainScore undoAndGetScore(
			final AccountAnalyzer accountAnalyzer,
			final BlockLookup localBlockLookup,
			final BlockHeight commonBlockHeight) {
		final PoiFacade poiFacade = accountAnalyzer.getPoiFacade();
		final BlockScorer scorer = new BlockScorer(poiFacade);
		final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(scorer);

		// this is delicate and the order matters, first visitor during undo changes amount of foraged blocks
		// second visitor needs that information
		final List<BlockVisitor> visitors = new ArrayList<>();
		visitors.add(new UndoBlockVisitor(
				this.observerFactory.createUndoCommitObserver(accountAnalyzer),
				new BlockExecutor(poiFacade, accountAnalyzer.getAccountCache())));
		visitors.add(scoreVisitor);
		final BlockVisitor visitor = new AggregateBlockVisitor(visitors);
		BlockIterator.unwindUntil(
				localBlockLookup,
				commonBlockHeight,
				visitor);
		accountAnalyzer.getPoiFacade().undoVesting(commonBlockHeight);
		return scoreVisitor.getScore();
	}

	/**
	 * This function prepares the peer chain for validation by recalculating all block difficulties (we don't trust the peer).
	 *
	 * @param parentBlock The parent block.
	 * @param peerChain The peer chain.
	 * @param blockScorer The block scorer that should be used in the difficulty calculation.
	 */
	private void calculatePeerChainDifficulties(
			final Block parentBlock,
			final Collection<Block> peerChain,
			final BlockScorer blockScorer) {
		final long blockDifference = parentBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1;
		final BlockHeight blockHeight = new BlockHeight(Math.max(1L, blockDifference));

		final int limit = (int)Math.min(parentBlock.getHeight().getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
		final List<TimeInstant> timeStamps = this.blockDao.getTimeStampsFrom(blockHeight, limit);
		final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);

		for (final Block block : peerChain) {
			final BlockDifficulty difficulty = blockScorer.getDifficultyScorer().calculateDifficulty(
					difficulties,
					timeStamps,
					block.getHeight().getRaw());
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
