package org.nem.nis;

import org.nem.core.connect.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.nis.dao.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.*;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.sync.*;
import org.nem.nis.visitors.PartialWeightedScoreVisitor;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockChain implements BlockSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(BlockChain.class.getName());

	// TODO 20140920 J-G: started some of the refactoring by pulling stuff out into BlockChainServices (i wouldn't dump everything there), but it think it is a good facade that hides a lot of the helpers from this class

	private final AccountDao accountDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final AccountAnalyzer accountAnalyzer;
	private final BlockChainServices services;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private BlockChainScore score;

	@Autowired(required = true)
	public BlockChain(
			final AccountAnalyzer accountAnalyzer,
			final AccountDao accountDao,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainServices services,
			final UnconfirmedTransactions unconfirmedTransactions) {
		this.accountAnalyzer = accountAnalyzer;
		this.accountDao = accountDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.unconfirmedTransactions = unconfirmedTransactions;
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
		final BlockScorer scorer = new BlockScorer(this.accountAnalyzer.getPoiFacade());
		this.score = this.score.add(new BlockChainScore(scorer.calculateBlockScore(parentBlock, block)));
	}

	/**
	 * Checks if given block follows last block in the chain.
	 *
	 * @param block The block to check.
	 * @return true if block can be next in chain
	 */
	private boolean isLastBlockParent(final Block block) {
		final boolean result;
		synchronized (this.blockChainLastBlockLayer) {
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
		final boolean result;
		synchronized (this.blockChainLastBlockLayer) {
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

	/**
	 * Synch algorithm:
	 * 1. Get peer's last block compare with ours, assuming it's ok
	 * 2. Take hashes of last blocks - at most REWRITE_LIMIT hashes, compare with proper hashes
	 * of peer, to find last common and first different block.
	 * If all peer's hashes has been checked we have nothing to do
	 * 3. if we have some blocks left AFTER common blocks, we'll need to revert those transactions,
	 * but before that we'll do some simple check, to see if peer's chain is actually better
	 * 4. Now we can get peer's chain and verify it
	 * 5. Once we've verified it, we can apply it
	 * (all-or-nothing policy, if verification failed, we won't try to apply part of it)
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

		if (result.getCode().isEvil()) {
			throw new FatalPeerException("remote node is evil");
		}

		return result;
	}

	private NodeInteractionResult synchronizeNodeInternal(final SyncConnectorPool connectorPool, final Node node) {
		final BlockChainSyncContext context = this.createSyncContext();
		// IMPORTANT: autoCached here
		final SyncConnector connector = connectorPool.getSyncConnector(context.accountAnalyzer.getAccountCache().asAutoCache());
		final ComparisonResult result = this.compareChains(connector, context.createLocalBlockLookup(), node);

		if (ComparisonResult.Code.REMOTE_IS_SYNCED == result.getCode() ||
				ComparisonResult.Code.REMOTE_REPORTED_EQUAL_CHAIN_SCORE == result.getCode()) {
			final Collection<Transaction> unconfirmedTransactions = connector.getUnconfirmedTransactions(node);
			unconfirmedTransactions.forEach(this.unconfirmedTransactions::addNew);
		}
		if (ComparisonResult.Code.REMOTE_IS_NOT_SYNCED != result.getCode()) {
			return NodeInteractionResult.fromComparisonResultCode(result.getCode());
		}

		final BlockHeight commonBlockHeight = new BlockHeight(result.getCommonBlockHeight());
		final org.nem.nis.dbmodel.Block dbParent = this.blockDao.findByHeight(commonBlockHeight);

		//region revert TXes inside contemporaryAccountAnalyzer
		BlockChainScore ourScore = BlockChainScore.ZERO;
		if (!result.areChainsConsistent()) {
			LOGGER.info("synchronizeNodeInternal -> chain inconsistent: calling undoTxesAndGetScore() (" +
					(this.blockChainLastBlockLayer.getLastBlockHeight() - dbParent.getHeight()) + " blocks).");
			ourScore = context.undoTxesAndGetScore(commonBlockHeight);
		}
		//endregion

		//region verify peer's chain
		final Collection<Block> peerChain = connector.getChainAfter(node, commonBlockHeight);
		final ValidationResult validationResult = this.updateOurChain(context, dbParent, peerChain, ourScore, !result.areChainsConsistent(), true);
		return NodeInteractionResult.fromValidationResult(validationResult);
	}

	/**
	 * Checks if passed receivedBlock is correct, and if eligible adds it to db
	 *
	 * @param receivedBlock - receivedBlock that's going to be processed
	 * @return Node experience code which indicates the status of the operation
	 */
	public synchronized ValidationResult processBlock(Block receivedBlock) {
		final Hash blockHash = HashUtils.calculateHash(receivedBlock);
		final Hash parentHash = receivedBlock.getPreviousBlockHash();

		final org.nem.nis.dbmodel.Block dbParent;

		// receivedBlock already seen
		synchronized (this.blockChainLastBlockLayer) {
			if (this.blockDao.findByHash(blockHash) != null) {
				// This will happen frequently and is ok
				return ValidationResult.NEUTRAL;
			}

			// check if we know previous receivedBlock
			dbParent = this.blockDao.findByHash(parentHash);
		}

		// if we don't have parent, we can't do anything with this receivedBlock
		if (dbParent == null) {
			// We might be on a fork, don't punish remote node
			return ValidationResult.NEUTRAL;
		}

		final BlockChainSyncContext context = this.createSyncContext();
		this.fixBlock(receivedBlock, dbParent);

		// EVIL hack, see issue#70
		// this evil hack also has side effect, that calling toModel, calculates proper totalFee inside the block
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(receivedBlock, new AccountDaoLookupAdapter(this.accountDao));
		receivedBlock = BlockMapper.toModel(dbBlock, context.accountAnalyzer.getAccountCache().asAutoCache());
		// EVIL hack end

		BlockChainScore ourScore = BlockChainScore.ZERO;
		boolean hasOwnChain = false;
		// we have parent, check if it has child
		if (dbParent.getNextBlockId() != null) {
			LOGGER.info("processBlock -> chain inconsistent: calling undoTxesAndGetScore() (" +
					(this.blockChainLastBlockLayer.getLastBlockHeight() - dbParent.getHeight()) + " blocks).");
			ourScore = context.undoTxesAndGetScore(new BlockHeight(dbParent.getHeight()));
			hasOwnChain = true;
		}

		final ArrayList<Block> peerChain = new ArrayList<>(1);
		peerChain.add(receivedBlock);

		return this.updateOurChain(context, dbParent, peerChain, ourScore, hasOwnChain, false);
	}

	private void fixBlock(final Block block, final org.nem.nis.dbmodel.Block parent) {
		// TODO 20140927 J-G not sure if we still need this here since the previous block is also set by the block chain validator
		fixGenerationHash(block, parent);

		final PoiFacade poiFacade = this.accountAnalyzer.getPoiFacade();
		final PoiAccountState state = poiFacade.findForwardedStateByAddress(block.getSigner().getAddress(), block.getHeight());
		final Account lessor = this.accountAnalyzer.getAccountCache().findByAddress(state.getAddress());
		block.setLessor(lessor);
	}

	private static void fixGenerationHash(final Block block, final org.nem.nis.dbmodel.Block parent) {
		block.setPreviousGenerationHash(parent.getGenerationHash());
	}

	private BlockChainSyncContext createSyncContext() {
		return new BlockChainSyncContext(
				this.accountAnalyzer.copy(),
				this.accountAnalyzer,
				this.blockChainLastBlockLayer,
				this.blockDao,
				this.services,
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
				this.unconfirmedTransactions,
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
		private final BlockChainLastBlockLayer blockChainLastBlockLayer;
		private final BlockDao blockDao;
		private final BlockChainServices services;
		private final BlockChainScore ourScore;

		private BlockChainSyncContext(
				final AccountAnalyzer accountAnalyzer,
				final AccountAnalyzer originalAnalyzer,
				final BlockChainLastBlockLayer blockChainLastBlockLayer,
				final BlockDao blockDao,
				final BlockChainServices services,
				final BlockChainScore ourScore) {
			this.accountAnalyzer = accountAnalyzer;
			this.originalAnalyzer = originalAnalyzer;
			this.blockChainLastBlockLayer = blockChainLastBlockLayer;
			this.blockDao = blockDao;
			this.services = services;
			this.ourScore = ourScore;
		}

		/**
		 * Reverses transactions between commonBlockHeight and current lastBlock.
		 * Additionally calculates score.
		 *
		 * @param commonBlockHeight height up to which TXes should be reversed.
		 * @return score for iterated blocks.
		 */
		public BlockChainScore undoTxesAndGetScore(final BlockHeight commonBlockHeight) {
			return this.services.undoAndGetScore(this.accountAnalyzer, this.createLocalBlockLookup(), commonBlockHeight);
		}

		public UpdateChainResult updateOurChain(
				final UnconfirmedTransactions unconfirmedTransactions,
				final org.nem.nis.dbmodel.Block dbParentBlock,
				final Collection<Block> peerChain,
				final BlockChainScore ourScore,
				final boolean hasOwnChain) {

			final BlockChainUpdateContext updateContext = new BlockChainUpdateContext(
					this.accountAnalyzer,
					this.originalAnalyzer,
					this.blockChainLastBlockLayer,
					this.blockDao,
					this.services,
					unconfirmedTransactions,
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
					this.accountAnalyzer.getAccountCache(),
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
		private final BlockChainServices services;
		private final UnconfirmedTransactions unconfirmedTransactions;
		private final Block parentBlock;
		private final Collection<Block> peerChain;
		private final BlockChainScore ourScore;
		private BlockChainScore peerScore;
		private final boolean hasOwnChain;

		public BlockChainUpdateContext(
				final AccountAnalyzer accountAnalyzer,
				final AccountAnalyzer originalAnalyzer,
				final BlockChainLastBlockLayer blockChainLastBlockLayer,
				final BlockDao blockDao,
				final BlockChainServices services,
				final UnconfirmedTransactions unconfirmedTransactions,
				final org.nem.nis.dbmodel.Block dbParentBlock,
				final Collection<Block> peerChain,
				final BlockChainScore ourScore,
				final boolean hasOwnChain) {

			this.accountAnalyzer = accountAnalyzer;
			this.originalAnalyzer = originalAnalyzer;
			this.blockScorer = new BlockScorer(this.accountAnalyzer.getPoiFacade());
			this.blockChainLastBlockLayer = blockChainLastBlockLayer;
			this.blockDao = blockDao;
			this.services = services;
			this.unconfirmedTransactions = unconfirmedTransactions;

			// do not trust peer, take first block from our db and convert it
			this.parentBlock = BlockMapper.toModel(dbParentBlock, this.accountAnalyzer.getAccountCache());

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

			// BR: Do not accept a chain with the same score.
			//     In case we got here via pushBlock the following can happen:
			//     2 different blocks with the height and score are pushed in the network.
			//     This leads to switching between the 2 blocks indefinitely resulting in tons of pushes.
			if (this.peerScore.compareTo(this.ourScore) <= 0) {
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
			return this.services.isPeerChainValid(this.accountAnalyzer, this.parentBlock, this.peerChain);
		}

		private BlockChainScore getPeerChainScore() {
			final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(this.blockScorer);
			BlockIterator.all(this.parentBlock, this.peerChain, scoreVisitor);
			return scoreVisitor.getScore();
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
						.filter(block -> this.blockChainLastBlockLayer.addBlockToDb(block))
						.forEach(block -> this.unconfirmedTransactions.removeAll(block));
			}
		}

		private void addRevertedTransactionsAsUnconfirmed(
				final Set<Hash> transactionHashes,
				final long wantedHeight,
				final AccountAnalyzer accountAnalyzer) {
			long currentHeight = this.blockChainLastBlockLayer.getLastBlockHeight();

			while (currentHeight != wantedHeight) {
				final org.nem.nis.dbmodel.Block block = this.blockDao.findByHeight(new BlockHeight(currentHeight));

				// if the transaction is in db, we should add it to unconfirmed transactions without a db check
				// (otherwise, since it is not removed from the database, the database hash check would fail).
				// at this point, only "state" (in accountAnalyzer and so on) is reverted.
				// removing (our) transactions from the db, is one of the last steps, mainly because that I/O is expensive, so someone
				// could try to spam us with "fake" responses during synchronization (and therefore force us to drop our blocks).
				block.getBlockTransfers().stream()
						.filter(tr -> !transactionHashes.contains(tr.getTransferHash()))
						.map(tr -> TransferMapper.toModel(tr, accountAnalyzer.getAccountCache()))
						.forEach(this.unconfirmedTransactions::addExisting);
				currentHeight--;
			}
		}
	}

	//endregion
}