package org.nem.nis.sync;

import org.nem.core.connect.FatalPeerException;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.deploy.NisConfiguration;
import org.nem.nis.*;
import org.nem.nis.controller.requests.ChainRequest;
import org.nem.nis.dao.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.poi.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.peer.NodeInteractionResult;
import org.nem.peer.connect.*;

import java.util.*;
import java.util.logging.Logger;

// TODO 20140920 J-* this class needs tests!!!

/**
 * Facade for updating a block chain.
 */
public class BlockChainUpdater implements BlockChainScoreManager {
	private static final Logger LOGGER = Logger.getLogger(BlockChainUpdater.class.getName());

	private final AccountDao accountDao;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final BlockDao blockDao;
	private final AccountAnalyzer accountAnalyzer;
	private final BlockChainContextFactory blockChainContextFactory;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final NisConfiguration configuration;
	private BlockChainScore score;

	public BlockChainUpdater(
			final AccountAnalyzer accountAnalyzer,
			final AccountDao accountDao,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final BlockDao blockDao,
			final BlockChainContextFactory blockChainContextFactory,
			final UnconfirmedTransactions unconfirmedTransactions,
			final NisConfiguration configuration) {
		this.accountAnalyzer = accountAnalyzer;
		this.accountDao = accountDao;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.blockChainContextFactory = blockChainContextFactory;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.configuration = configuration;
		this.score = BlockChainScore.ZERO;
	}

	//region BlockChainScoreManager

	@Override
	public BlockChainScore getScore() {
		return this.score;
	}

	@Override
	public void updateScore(final Block parentBlock, final Block block) {
		final BlockScorer scorer = new BlockScorer(this.accountAnalyzer.getPoiFacade());
		this.score = this.score.add(new BlockChainScore(scorer.calculateBlockScore(parentBlock, block)));
	}

	//endregion

	//region updateChain

	/**
	 * Synchronizes the chain with another node.
	 *
	 * @param connectorPool The connector pool.
	 * @param node The node.
	 * @return The result of the interaction.
	 */
	public NodeInteractionResult updateChain(final SyncConnectorPool connectorPool, final Node node) {
		final org.nem.nis.dbmodel.Block expectedLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final BlockChainSyncContext context = this.createSyncContext();
		// IMPORTANT: autoCached here
		final SyncConnector connector = connectorPool.getSyncConnector(context.accountAnalyzer().getAccountCache().asAutoCache());
		LOGGER.severe("compare chains");
		final ComparisonResult result = this.compareChains(connector, context.createLocalBlockLookup(), node);

		switch (result.getCode()) {
			case REMOTE_IS_SYNCED:
			case REMOTE_REPORTED_EQUAL_CHAIN_SCORE:
				final Collection<Transaction> unconfirmedTransactions = connector.getUnconfirmedTransactions(node);
				this.unconfirmedTransactions.addNewBatch(unconfirmedTransactions);
				return NodeInteractionResult.fromComparisonResultCode(result.getCode());

			case REMOTE_IS_NOT_SYNCED:
				break;

			default:
				return NodeInteractionResult.fromComparisonResultCode(result.getCode());
		}

		final BlockHeight commonBlockHeight = new BlockHeight(result.getCommonBlockHeight());
		final org.nem.nis.dbmodel.Block dbParent = this.blockDao.findByHeight(commonBlockHeight);

		//region revert TXes inside contemporaryAccountAnalyzer
		BlockChainScore ourScore = BlockChainScore.ZERO;
		if (!result.areChainsConsistent()) {
			LOGGER.info(String.format(
					"synchronizeNodeInternal -> chain inconsistent: calling undoTxesAndGetScore() (%d blocks).",
					this.blockChainLastBlockLayer.getLastBlockHeight() - dbParent.getHeight()));
			ourScore = context.undoTxesAndGetScore(commonBlockHeight);
		}
		//endregion

		//region verify peer's chain
		final int minBlocks = (int)(this.blockChainLastBlockLayer.getLastBlockHeight() - commonBlockHeight.getRaw());
		LOGGER.severe("get peer chain part");
		final Collection<Block> peerChain = connector.getChainAfter(node, new ChainRequest(commonBlockHeight, minBlocks, configuration.getMaxTransactions()));
		final ValidationResult validationResult = this.updateOurChain(context, dbParent, peerChain, ourScore, !result.areChainsConsistent(), true, expectedLastBlock);
		return NodeInteractionResult.fromValidationResult(validationResult);
		//endregion
	}

	private ComparisonResult compareChains(final SyncConnector connector, final BlockLookup localLookup, final Node node) {
		final ComparisonContext context = new DefaultComparisonContext(localLookup.getLastBlock().getHeight());
		final BlockChainComparer comparer = new BlockChainComparer(context);

		final BlockLookup remoteLookup = new RemoteBlockLookupAdapter(connector, node);

		final ComparisonResult result = comparer.compare(localLookup, remoteLookup);

		if (result.getCode().isEvil()) {
			throw new FatalPeerException(String.format("remote node is evil: %s", result.getCode()));
		}

		return result;
	}

	//endregion

	//region updateBlock

	/**
	 * Synchronizes the chain with a received block.
	 *
	 * @param receivedBlock The receivedBlock.
	 * @return The result of the interaction.
	 */
	public ValidationResult updateBlock(Block receivedBlock) {
		final org.nem.nis.dbmodel.Block expectedLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final Hash blockHash = HashUtils.calculateHash(receivedBlock);
		final Hash parentHash = receivedBlock.getPreviousBlockHash();

		final org.nem.nis.dbmodel.Block dbParent;

		// receivedBlock already seen
		LOGGER.info("Find block hash");
		if (this.blockDao.findByHash(blockHash) != null) {
			// This will happen frequently and is ok
			return ValidationResult.NEUTRAL;
		}

		// check if we know previous receivedBlock
		LOGGER.info("Find parent hash");
		dbParent = this.blockDao.findByHash(parentHash);

		// if we don't have parent, we can't do anything with this receivedBlock
		if (dbParent == null) {
			// We might be on a fork, don't punish remote node
			return ValidationResult.NEUTRAL;
		}

		final BlockChainSyncContext context = this.createSyncContext();
		this.fixBlock(receivedBlock, dbParent);

		// EVIL hack, see issue#70
		// this evil hack also has side effect, that calling toModel, calculates proper totalFee inside the block
		LOGGER.info("remap block");
		receivedBlock = this.remapBlock(receivedBlock, context.accountAnalyzer());
		// EVIL hack end

		BlockChainScore ourScore = BlockChainScore.ZERO;
		boolean hasOwnChain = false;
		// we have parent, check if it has child
		if (this.blockDao.findByHeight(new BlockHeight(dbParent.getHeight() + 1)) != null) {
			LOGGER.info(String.format(
					"processBlock -> chain inconsistent: calling undoTxesAndGetScore() (%d blocks).",
					this.blockChainLastBlockLayer.getLastBlockHeight() - dbParent.getHeight()));
			ourScore = context.undoTxesAndGetScore(new BlockHeight(dbParent.getHeight()));
			hasOwnChain = true;

			// undo above might remove some accounts from account analyzer,
			// because receivedBlock has still references to those accounts, AND if competitive
			// block has transaction addressed to that account, it won't be seen later,
			// (see canSuccessfullyProcessBlockAndSiblingWithBetterScoreIsAcceptedAfterwards test for details)
			// we remap once more to fix accounts references (and possibly add them to AA)
			LOGGER.info("remap block");
			receivedBlock = this.remapBlock(receivedBlock, context.accountAnalyzer());
		}

		final ArrayList<Block> peerChain = new ArrayList<>(1);
		peerChain.add(receivedBlock);

		return this.updateOurChain(context, dbParent, peerChain, ourScore, hasOwnChain, false, expectedLastBlock);
	}

	private Block remapBlock(final Block block, final AccountAnalyzer accountAnalyzer) {
		final org.nem.nis.dbmodel.Block dbBlock = BlockMapper.toDbModel(block, new AccountDaoLookupAdapter(this.accountDao));
		return BlockMapper.toModel(dbBlock, accountAnalyzer.getAccountCache().asAutoCache());
	}

	private void fixBlock(final Block block, final org.nem.nis.dbmodel.Block parent) {
		// blocks that are received via /push/block do not have their generation hashes set
		// (generation hashes are not serialized), so we need to recalculate it for
		// each block that we receive
		fixGenerationHash(block, parent);

		final PoiFacade poiFacade = this.accountAnalyzer.getPoiFacade();
		final PoiAccountState state = poiFacade.findForwardedStateByAddress(block.getSigner().getAddress(), block.getHeight());
		final Account lessor = this.accountAnalyzer.getAccountCache().findByAddress(state.getAddress());
		block.setLessor(lessor);
	}

	private static void fixGenerationHash(final Block block, final org.nem.nis.dbmodel.Block parent) {
		block.setPreviousGenerationHash(parent.getGenerationHash());
	}

	//endregion

	private BlockChainSyncContext createSyncContext() {
		return this.blockChainContextFactory.createSyncContext(this.score);
	}

	private synchronized ValidationResult updateOurChain(
			final BlockChainSyncContext context,
			final org.nem.nis.dbmodel.Block dbParentBlock,
			final Collection<Block> peerChain,
			final BlockChainScore ourScore,
			final boolean hasOwnChain,
			final boolean shouldPunishLowerPeerScore,
			final org.nem.nis.dbmodel.Block expectedLastBlock) {
		if (!expectedLastBlock.getBlockHash().equals(this.blockChainLastBlockLayer.getLastDbBlock().getBlockHash())) {
			// last block has changed, don't do anything
			LOGGER.severe("last block changed!");
			return ValidationResult.NEUTRAL;
		}

		final BlockChainUpdateContext updateContext = this.blockChainContextFactory.createUpdateContext(
				context,
				dbParentBlock,
				peerChain,
				ourScore,
				hasOwnChain);
		final UpdateChainResult updateResult = updateContext.update();

		if (shouldPunishLowerPeerScore && updateResult.peerScore.compareTo(updateResult.ourScore) <= 0) {
			// if we got here, the peer lied about his score, so penalize him
			return ValidationResult.FAILURE_CHAIN_SCORE_INFERIOR;
		}

		if (ValidationResult.SUCCESS == updateResult.validationResult) {
			this.score = this.score.subtract(updateResult.ourScore).add(updateResult.peerScore);
		}

		return updateResult.validationResult;
	}
}
