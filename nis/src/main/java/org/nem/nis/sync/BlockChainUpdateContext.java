package org.nem.nis.sync;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.nis.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.visitors.PartialWeightedScoreVisitor;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Somewhat messy class that actually updates the local chain as appropriate.
 */
public class BlockChainUpdateContext {
	private static final Logger LOGGER = Logger.getLogger(BlockChainUpdateContext.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final AccountAnalyzer originalAnalyzer;
	private final HashCache transactionHashCache;
	private final HashCache originalTransactionHashCache;
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
			final HashCache transactionHashCache,
			final HashCache originalTransactionHashCache,
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
		this.transactionHashCache = transactionHashCache;
		this.originalTransactionHashCache = originalTransactionHashCache;
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

	public UpdateChainResult update() {
		final UpdateChainResult result = new UpdateChainResult();
		result.validationResult = this.updateInternal();
		result.ourScore = this.ourScore;
		result.peerScore = this.peerScore;
		return result;
	}

	private ValidationResult updateInternal() {
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
		//     2 different blocks with the same height and score are pushed in the network.
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
		return this.services.isPeerChainValid(this.accountAnalyzer, this.transactionHashCache, this.parentBlock, this.peerChain);
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
		this.accountAnalyzer.shallowCopyTo(this.originalAnalyzer);
		this.transactionHashCache.shallowCopyTo(this.originalTransactionHashCache);

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
					.forEach(tr -> this.unconfirmedTransactions.addExisting(tr));
			currentHeight--;
		}
	}
}
