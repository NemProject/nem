package org.nem.nis;

import org.nem.core.connect.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.nis.mappers.*;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dao.BlockDao;
import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.mappers.TransferMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.*;
import org.nem.nis.visitors.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.node.Node;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	private final AccountDao accountDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final AccountAnalyzer accountAnalyzer;
	private final Foraging foraging;
	private BlockChainScore score;

	@Autowired(required = true)
	public BlockChain(
			final AccountAnalyzer accountAnalyzer,
			final AccountDao accountDao,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final Foraging foraging) {
		this.accountAnalyzer = accountAnalyzer;
		this.accountDao = accountDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.foraging = foraging;
		this.score = BlockChainScore.ZERO;
	}
	
	/**
	 * Returns the overall score for the chain.
	 * 
	 * @return the score.
	 */
	public BlockChainScore getScore() {
		return this.score;
	}

	/**
	 * Creates a copy of the current account analyzer.
	 *
	 * @return A copy of the account analyzer.
	 */
	public AccountAnalyzer copyAccountAnalyzer() {
		return this.accountAnalyzer.copy();
	}

	/**
	 * Updates the score of this chain by adding the score derived from the specified block and parent block.
	 *
	 * @param parentBlock The parent block.
	 * @param block The block.
	 */
	public void updateScore(final Block parentBlock, final Block block) {
		final BlockScorer scorer = new BlockScorer(this.accountAnalyzer);
		this.score = this.score.add(new BlockChainScore(scorer.calculateBlockScore(parentBlock, block)));
	}

	/**
	 * Checks if given block follows last block in the chain.
	 *
	 * @param block The block to check.
	 * @return true if block can be next in chain
	 */
	private boolean isLastBlockParent(final Block block) {
		boolean result;
		synchronized (blockChainLastBlockLayer) {
			result = this.blockChainLastBlockLayer.getLastDbBlock().getBlockHash().equals(block.getPreviousBlockHash());
			LOGGER.info("isLastBlockParent result: " + result);
			LOGGER.info("last block height: " + this.blockChainLastBlockLayer.getLastDbBlock().getHeight());
		}
		return result;
	}

	/**
	 * Checks if given block has the same parent as last block in the chain.
	 *
	 * @param block The block to check.
	 * @return true if block is a sibling of the last block in the chain.
	 */
	private boolean isLastBlockSibling(final Block block) {
		boolean result;
		synchronized (blockChainLastBlockLayer) {
			// it's better to base it on hash of previous block instead of height
			result = this.blockChainLastBlockLayer.getLastDbBlock().getPrevBlockHash().equals(block.getPreviousBlockHash());
		}
		return result;
	}

	/**
	 * Checks a block that was received by a peer.
	 *
	 * @param block The block.
	 * @return An appropriate interaction result.
	 */
	public ValidationResult checkPushedBlock(final Block block) {
		if (!this.isLastBlockParent(block)) {
			// if peer tried to send us block that we also generated, there is no sense to punish him
			return this.isLastBlockSibling(block) ? ValidationResult.NEUTRAL : ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		// the peer returned a block that can be added to our chain
		return block.verify() ? ValidationResult.SUCCESS : ValidationResult.FAILURE_SIGNATURE_NOT_VERIFIABLE;
	}

	public Block forageBlock() {
		final BlockScorer scorer = new BlockScorer(this.accountAnalyzer);
		final Block block = this.foraging.forageBlock(scorer);

		// make a full-blown analysis
		// TODO: we can call it thanks to the "hack" inside processBlock
		if (block != null && this.processBlock(block) == ValidationResult.SUCCESS) {
			return block;
		}

		return null;
	}

	/**
	 * Synch algorithm:
	 *  1. Get peer's last block compare with ours, assuming it's ok
	 *  2. Take hashes of last blocks - at most REWRITE_LIMIT hashes, compare with proper hashes
	 *     of peer, to find last common and first different block.
	 *     If all peer's hashes has been checked we have nothing to do
	 *  3. if we have some blocks left AFTER common blocks, we'll need to revert those transactions,
	 *     but before that we'll do some simple check, to see if peer's chain is actually better
	 *  4. Now we can get peer's chain and verify it
	 *  5. Once we've verified it, we can apply it
	 *     (all-or-nothing policy, if verification failed, we won't try to apply part of it)
	 *
	 * @param connectorPool The sync connector pool.
	 * @param node The other node.
	 */
	@Override
	public synchronized NodeInteractionResult synchronizeNode(final SyncConnectorPool connectorPool, final Node node) {
		try {
			return this.synchronizeNodeInternal(connectorPool, node);
		} catch (InactivePeerException | FatalPeerException ex) {
			return NodeInteractionResult.FAILURE;
		}
	}

	private ComparisonResult compareChains(final SyncConnector connector, final BlockLookup localLookup, final Node node) {
		final ComparisonContext context = new ComparisonContext(BlockChainConstants.BLOCKS_LIMIT, BlockChainConstants.REWRITE_LIMIT);
		final BlockChainComparer comparer = new BlockChainComparer(context);

		final BlockLookup remoteLookup = new RemoteBlockLookupAdapter(connector, node);

		final ComparisonResult result = comparer.compare(localLookup, remoteLookup);

		if (0 != (ComparisonResult.Code.REMOTE_IS_EVIL & result.getCode())) {
			throw new FatalPeerException("remote node is evil");
		}

		return result;
	}

	private NodeInteractionResult synchronizeNodeInternal(final SyncConnectorPool connectorPool, final Node node) {
		final BlockChainSyncContext context = this.createSyncContext();
		// IMPORTANT: autoCached here
		final SyncConnector connector = connectorPool.getSyncConnector(context.accountAnalyzer.asAutoCache());
		final ComparisonResult result = compareChains(connector, context.createLocalBlockLookup(), node);

		if (ComparisonResult.Code.REMOTE_IS_SYNCED == result.getCode()) {
			// TODO: remove try-catch when we are sure everyone has nis version with unconfirmed transactions polling
			try {
				Collection<Transaction> unconfirmedTransactions = connector.getUnconfirmedTransactions(node);
				this.foraging.processTransactions(unconfirmedTransactions);
			} catch (Exception e) {
				// Don't care at the moment
				LOGGER.info("Exception calling getUnconfirmedTransactions(): " + e.toString());
			}
		}
		if (ComparisonResult.Code.REMOTE_IS_NOT_SYNCED != result.getCode()) {
			return mapComparisonResultCodeToNodeInteractionResult(result.getCode());
		}

		final BlockHeight commonBlockHeight = new BlockHeight(result.getCommonBlockHeight());
		final org.nem.nis.dbmodel.Block dbParent = this.blockDao.findByHeight(commonBlockHeight);

		//region revert TXes inside contemporaryAccountAnalyzer
		BlockChainScore ourScore = BlockChainScore.ZERO;
		if (!result.areChainsConsistent()) {
			LOGGER.info("Chain inconsistent: calling undoTxesAndGetScore().");
			ourScore = context.undoTxesAndGetScore(commonBlockHeight);
		}
		//endregion

		//region verify peer's chain
		final Collection<Block> peerChain = connector.getChainAfter(node, commonBlockHeight);
		final ValidationResult validationResult = updateOurChain(context, dbParent, peerChain, ourScore, !result.areChainsConsistent(), true);
		return NodeInteractionResult.fromValidationResult(validationResult);
	}

	private NodeInteractionResult mapComparisonResultCodeToNodeInteractionResult(final int comparisonResultCode) {
		switch (comparisonResultCode) {
			case ComparisonResult.Code.REMOTE_IS_SYNCED:
            case ComparisonResult.Code.REMOTE_IS_TOO_FAR_BEHIND:
            case ComparisonResult.Code.REMOTE_REPORTED_LOWER_OR_EQUAL_CHAIN_SCORE:
				return NodeInteractionResult.NEUTRAL;
		}

		return NodeInteractionResult.FAILURE;
	}

	private void fixGenerationHash(final Block block, final org.nem.nis.dbmodel.Block parent) {
		block.setPreviousGenerationHash(parent.getGenerationHash());
	}

	/**
	 * Checks if passed receivedBlock is correct, and if eligible adds it to db
	 *
	 * @param receivedBlock - receivedBlock that's going to be processed
	 *
	 * @return Node experience code which indicates the status of the operation
	 */
	public synchronized ValidationResult processBlock(Block receivedBlock) {
		final Hash blockHash = HashUtils.calculateHash(receivedBlock);
		final Hash parentHash = receivedBlock.getPreviousBlockHash();

		final org.nem.nis.dbmodel.Block dbParent;

		// this method processes only blocks that have been sent directly (pushed)
		// to us, so we can add quite strict rule here
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		if (receivedBlock.getTimeStamp().compareTo(currentTime.addMinutes(3)) > 0) {
			// This really should not happen
			return ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE;
		}

		// receivedBlock already seen
		synchronized (blockChainLastBlockLayer) {
			if (blockDao.findByHash(blockHash) != null) {
				// This will happen frequently and is ok
				return ValidationResult.NEUTRAL;
			}

			// check if we know previous receivedBlock
			dbParent = blockDao.findByHash(parentHash);
		}

		// if we don't have parent, we can't do anything with this receivedBlock
		if (dbParent == null) {
			// We might be on a fork, don't punish remote node
			return ValidationResult.NEUTRAL;
		}

		// TODO: we should have some time limit set
		// BR: Why that? Could just be bad luck.
//		if (receivedBlock.getTimeStamp() > parent.getTimestamp() + 20*30) {
//			return false;
//		}

		final BlockChainSyncContext context = this.createSyncContext();

		fixGenerationHash(receivedBlock, dbParent);

		// EVIL hack, see issue#70
		// this evil hack also has side effect, that calling toModel, calculates proper totalFee inside the block
		org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(receivedBlock, new AccountDaoLookupAdapter(this.accountDao));
		receivedBlock = BlockMapper.toModel(dbBlock, context.accountAnalyzer.asAutoCache());
		// EVIL hack end

		BlockChainScore ourScore = BlockChainScore.ZERO;
		boolean hasOwnChain = false;
		// we have parent, check if it has child
		if (dbParent.getNextBlockId() != null) {
			ourScore = context.undoTxesAndGetScore(new BlockHeight(dbParent.getHeight()));
			hasOwnChain = true;
		}

		final ArrayList<Block> peerChain = new ArrayList<>(1);
		peerChain.add(receivedBlock);

		return updateOurChain(context, dbParent, peerChain, ourScore, hasOwnChain, false);
	}

	private BlockChainSyncContext createSyncContext() {
		return new BlockChainSyncContext(
				this.accountAnalyzer.copy(),
				this.accountAnalyzer,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.score);
	}

	private ValidationResult updateOurChain(
			final BlockChainSyncContext context,
			final org.nem.nis.dbmodel.Block dbParentBlock,
			final Collection<Block> peerChain,
			final BlockChainScore ourScore,
			final boolean hasOwnChain,
			final boolean shouldPunishLowerPeerScore) {
		final UpdateChainResult updateResult = context.updateOurChain(
				this.foraging,
				dbParentBlock,
				peerChain,
				ourScore,
				hasOwnChain);

		if (shouldPunishLowerPeerScore && updateResult.peerScore.compareTo(updateResult.ourScore) <= 0) {
			// if we got here, the peer lied about his score, so penalize him
			return ValidationResult.FAILURE_CHAIN_SCORE_INFERIOR;
		}

		if (ValidationResult.SUCCESS == updateResult.validationResult) {
			this.score = this.score.subtract(updateResult.ourScore).add(updateResult.peerScore);
		}

		return updateResult.validationResult;
	}

	//region UpdateChainResult

	private static class UpdateChainResult {
		public ValidationResult validationResult;
		public BlockChainScore ourScore;
		public BlockChainScore peerScore;
	}

	//endregion

	//region BlockChainSyncContext

	private static class BlockChainSyncContext {
		private final AccountAnalyzer accountAnalyzer;
		private final AccountAnalyzer originalAnalyzer;
		private final BlockScorer blockScorer;
		private final BlockChainLastBlockLayer blockChainLastBlockLayer;
		private final BlockDao blockDao;
		private final BlockChainScore ourScore;

		private BlockChainSyncContext(
				final AccountAnalyzer accountAnalyzer,
				final AccountAnalyzer originalAnalyzer,
				final BlockChainLastBlockLayer blockChainLastBlockLayer,
				final BlockDao blockDao,
				final BlockChainScore ourScore) {
			this.accountAnalyzer = accountAnalyzer;
			this.originalAnalyzer = originalAnalyzer;
			this.blockScorer = new BlockScorer(this.accountAnalyzer);
			this.blockChainLastBlockLayer = blockChainLastBlockLayer;
			this.blockDao = blockDao;
			this.ourScore = ourScore;
		}

		/**
		 * Reverses transactions between commonBlockHeight and current lastBlock.
		 * Additionally calculates score.
		 *
		 * @param commonBlockHeight height up to which TXes should be reversed.
		 *
		 * @return score for iterated blocks.
		 */
		public BlockChainScore undoTxesAndGetScore(final BlockHeight commonBlockHeight) {
			final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(this.blockScorer);

			// this is delicate and the order matters, first visitor during unapply changes amount of foraged blocks
			// second visitor needs that information
			final List<BlockVisitor> visitors = new ArrayList<>();
			visitors.add(new UndoBlockVisitor(new AccountsHeightObserver(this.accountAnalyzer)));
			visitors.add(scoreVisitor);
			final BlockVisitor visitor = new AggregateBlockVisitor(visitors);
			BlockIterator.unwindUntil(
					this.createLocalBlockLookup(),
					commonBlockHeight,
					visitor);

			return scoreVisitor.getScore();
		}

		public UpdateChainResult updateOurChain(
				final Foraging foraging,
				final org.nem.nis.dbmodel.Block dbParentBlock,
				final Collection<Block> peerChain,
				final BlockChainScore ourScore,
				final boolean hasOwnChain) {

				final BlockChainUpdateContext updateContext = new BlockChainUpdateContext(
					this.accountAnalyzer,
					this.originalAnalyzer,
					this.blockScorer,
					this.blockChainLastBlockLayer,
					this.blockDao,
					foraging,
					dbParentBlock,
					peerChain,
					ourScore,
					hasOwnChain);

			final UpdateChainResult result = new UpdateChainResult();
			result.validationResult = updateContext.update();
			result.ourScore = updateContext.ourScore;
			result.peerScore = updateContext.peerScore;
			return result;
		}

		private BlockLookup createLocalBlockLookup() {
			return new LocalBlockLookupAdapter(
					this.blockDao,
					this.accountAnalyzer,
					this.blockChainLastBlockLayer.getLastDbBlock(),
					this.ourScore,
					BlockChainConstants.BLOCKS_LIMIT);
		}
	}

	//endregion

	//region BlockChainUpdateContext

	private static class BlockChainUpdateContext {

		private final AccountAnalyzer accountAnalyzer;
		private final AccountAnalyzer originalAnalyzer;
		private final BlockScorer blockScorer;
		private final BlockChainLastBlockLayer blockChainLastBlockLayer;
		private final BlockDao blockDao;
		private final Foraging foraging;
		private final Block parentBlock;
		private final Collection<Block> peerChain;
		private final BlockChainScore ourScore;
		private BlockChainScore peerScore;
		private final boolean hasOwnChain;

		public BlockChainUpdateContext(
				final AccountAnalyzer accountAnalyzer,
				final AccountAnalyzer originalAnalyzer,
				final BlockScorer blockScorer,
				final BlockChainLastBlockLayer blockChainLastBlockLayer,
				final BlockDao blockDao,
				final Foraging foraging,
				final org.nem.nis.dbmodel.Block dbParentBlock,
				final Collection<Block> peerChain,
				final BlockChainScore ourScore,
				final boolean hasOwnChain) {

			this.accountAnalyzer = accountAnalyzer;
			this.originalAnalyzer = originalAnalyzer;
			this.blockScorer = blockScorer;
			this.blockChainLastBlockLayer = blockChainLastBlockLayer;
			this.blockDao = blockDao;
			this.foraging = foraging;

			// do not trust peer, take first block from our db and convert it
			this.parentBlock = BlockMapper.toModel(dbParentBlock, this.accountAnalyzer);

			this.peerChain = peerChain;
			this.ourScore = ourScore;
			this.peerScore = BlockChainScore.ZERO;
			this.hasOwnChain = hasOwnChain;
		}

		public ValidationResult update() {
			if (!this.validatePeerChain()) {
				return ValidationResult.FAILURE_CHAIN_INVALID;
			}

			this.peerScore = this.getPeerChainScore();

			logScore(this.ourScore, this.peerScore);
			if (BlockChainScore.ZERO.equals(this.peerScore)) {
				return ValidationResult.FAILURE_CHAIN_INVALID;
			}

			if (this.peerScore.compareTo(this.ourScore) < 0) {
				return ValidationResult.NEUTRAL;
			}

			this.updateOurChain();
			return ValidationResult.SUCCESS;
		}

		private static void logScore(final BlockChainScore ourScore, final BlockChainScore peerScore) {
			if (BlockChainScore.ZERO.equals(ourScore)) {
				LOGGER.info(String.format("new block's score: %s", peerScore));
			} else {
				LOGGER.info(String.format("our score: %s, peer's score: %s", ourScore, peerScore));
			}
		}

		/**
		 * Validates blocks in peerChain.
		 *
		 * @return score or -1 if chain is invalid
		 */
		private boolean validatePeerChain() {
			final BlockChainValidator validator = new BlockChainValidator(
					this.accountAnalyzer,
					this.blockScorer,
					BlockChainConstants.BLOCKS_LIMIT);
			this.calculatePeerChainDifficulties();
			return validator.isValid(this.parentBlock, this.peerChain);
		}

		private BlockChainScore getPeerChainScore() {
			final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(this.blockScorer);
			BlockIterator.all(this.parentBlock, this.peerChain, scoreVisitor);
			return scoreVisitor.getScore();
		}

		private void calculatePeerChainDifficulties() {
			final long blockDifference = this.parentBlock.getHeight().getRaw() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION + 1;
			final BlockHeight blockHeight = new BlockHeight(Math.max(1L, blockDifference));

			int limit = (int)Math.min(this.parentBlock.getHeight().getRaw(), BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION);
			final List<TimeInstant> timestamps = this.blockDao.getTimestampsFrom(blockHeight, limit);
			final List<BlockDifficulty> difficulties = this.blockDao.getDifficultiesFrom(blockHeight, limit);

			for (final Block block : this.peerChain) {
				final BlockDifficulty difficulty = this.blockScorer.calculateDifficulty(difficulties, timestamps);
				block.setDifficulty(difficulty);

				// apache collections4 only have CircularFifoQueue which as a queue doesn't have .get()
				difficulties.add(difficulty);
				timestamps.add(block.getTimeStamp());
				if (difficulties.size() > BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION) {
					difficulties.remove(0);
					timestamps.remove(0);
				}
			}
		}

		/*
		 * 1. replace current accountAnalyzer with contemporaryAccountAnalyzer
		 * 2. add unconfirmed transactions from "our" chain
		 *    (except those transactions, that are included in peer's chain)
		 *
		 * 3. drop "our" blocks from the db
		 *
		 * 4. update db with "peer's" chain
		 */
		private void updateOurChain() {
			synchronized (this.blockChainLastBlockLayer) {
				logAccounts("original", this.originalAnalyzer);
				logAccounts("new", this.accountAnalyzer);
				this.accountAnalyzer.shallowCopyTo(this.originalAnalyzer);

				if (this.hasOwnChain) {
					// mind that we're using "new" (replaced) accountAnalyzer
					final Set<Hash> transactionHashes = this.peerChain.stream()
							.flatMap(bl -> bl.getTransactions().stream())
							.map(bl -> HashUtils.calculateHash(bl))
							.collect(Collectors.toSet());
					this.addRevertedTransactionsAsUnconfirmed(
							transactionHashes,
							this.parentBlock.getHeight().getRaw(),
							this.originalAnalyzer);
				}

				this.blockChainLastBlockLayer.dropDbBlocksAfter(this.parentBlock.getHeight());

				this.peerChain.stream()
						.filter(tr -> this.blockChainLastBlockLayer.addBlockToDb(tr))
						.forEach(tr -> this.foraging.removeFromUnconfirmedTransactions(tr));
			}
		}

		private static void logAccounts(final String heading, final Iterable<Account> accounts) {
			LOGGER.info(String.format("[%s]", heading));
			for (final Account account : accounts) {
				LOGGER.info(String.format("%s : %s", account.getAddress().getEncoded(), account.getImportanceInfo()));
			}
		}

		private void addRevertedTransactionsAsUnconfirmed(
				final Set<Hash> transactionHashes,
				final long wantedHeight,
				final AccountAnalyzer accountAnalyzer) {
			long currentHeight = this.blockChainLastBlockLayer.getLastBlockHeight();

			while (currentHeight != wantedHeight) {
				org.nem.nis.dbmodel.Block block = this.blockDao.findByHeight(new BlockHeight(currentHeight));

				// if the transaction is in DB it means at some point
				// isValid and verify had to be called on it, so we can safely add it
				// as unconfirmed
				block.getBlockTransfers().stream()
						.filter(tr -> !transactionHashes.contains(tr.getTransferHash()))
						.map(tr -> TransferMapper.toModel(tr, accountAnalyzer))
						.forEach(tr -> this.foraging.addUnconfirmedTransactionWithoutDbCheck(tr));
				currentHeight--;
			}
		}
	}

	//endregion
}