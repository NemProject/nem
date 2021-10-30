package org.nem.nis.sync;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.visitors.PartialWeightedScoreVisitor;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Somewhat messy class that actually updates the local chain as appropriate.
 */
public class BlockChainUpdateContext {
	private static final Logger LOGGER = Logger.getLogger(BlockChainUpdateContext.class.getName());

	private final NisCache nisCache;
	private final ReadOnlyNisCache originalNisCache;
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

	public BlockChainUpdateContext(final NisCache nisCache, final ReadOnlyNisCache originalNisCache,
			final BlockChainLastBlockLayer blockChainLastBlockLayer, final BlockDao blockDao, final BlockChainServices services,
			final UnconfirmedTransactions unconfirmedTransactions, final DbBlock dbParentBlock, final Collection<Block> peerChain,
			final BlockChainScore ourScore, final boolean hasOwnChain) {

		this.nisCache = nisCache;
		this.originalNisCache = originalNisCache;
		this.blockScorer = new BlockScorer(this.nisCache.getAccountStateCache());
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.blockDao = blockDao;
		this.services = services;
		this.unconfirmedTransactions = unconfirmedTransactions;

		// do not trust peer, take first block from our db and convert it
		this.parentBlock = this.services.createMapper(this.nisCache.getAccountCache()).map(dbParentBlock);

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
		long start = System.currentTimeMillis();
		if (!this.validatePeerChain()) {
			return ValidationResult.FAILURE_CHAIN_INVALID;
		}
		long stop = System.currentTimeMillis();
		final int numTransactions = peerChain.stream().map(b -> b.getTransactions().size()).reduce(0, Integer::sum);
		LOGGER.info(String.format("validated %d blocks (%d transactions) in %d ms (%d μs/tx)", peerChain.size(), numTransactions,
				stop - start, 0 == numTransactions ? 0 : (stop - start) * 1000 / numTransactions));

		this.peerScore = this.getPeerChainScore();

		logScore(this.ourScore, this.peerScore);
		if (BlockChainScore.ZERO.equals(this.peerScore)) {
			return ValidationResult.FAILURE_CHAIN_INVALID;
		}

		// BR: Do not accept a chain with the same score.
		// In case we got here via pushBlock the following can happen:
		// 2 different blocks with the same height and score are pushed in the network.
		// This leads to switching between the 2 blocks indefinitely resulting in tons of pushes.
		if (this.peerScore.compareTo(this.ourScore) <= 0) {
			return ValidationResult.NEUTRAL;
		}

		// Since the blocks/transactions have been executed at this point it is time to fix the blocks lessors.
		this.fixChain(this.peerChain);

		start = System.currentTimeMillis();
		this.updateOurChain();
		stop = System.currentTimeMillis();
		LOGGER.info(String.format("chain update of %d blocks (%d transactions) needed %d ms (%d μs/tx)", peerChain.size(), numTransactions,
				stop - start, 0 == numTransactions ? 0 : (stop - start) * 1000 / numTransactions));
		return ValidationResult.SUCCESS;
	}

	private void fixChain(final Collection<Block> blocks) {
		blocks.stream().forEach(this::fixLessor);
	}

	private void fixLessor(final Block block) {
		final ReadOnlyAccountStateCache accountStateCache = this.nisCache.getAccountStateCache();
		final ReadOnlyAccountState state = accountStateCache.findForwardedStateByAddress(block.getSigner().getAddress(), block.getHeight());
		final Account lessor = this.nisCache.getAccountCache().findByAddress(state.getAddress());
		block.setLessor(lessor);
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
		return this.services.isPeerChainValid(this.nisCache, this.parentBlock, this.peerChain).isSuccess();
	}

	private BlockChainScore getPeerChainScore() {
		final PartialWeightedScoreVisitor scoreVisitor = new PartialWeightedScoreVisitor(this.blockScorer);
		BlockIterator.all(this.parentBlock, this.peerChain, scoreVisitor);
		return scoreVisitor.getScore();
	}

	/**
	 * 1. replace current accountAnalyzer with contemporaryAccountAnalyzer <br>
	 * 2. add unconfirmed transactions from "our" chain (except those transactions, that are included in peer's chain) <br>
	 * 3. drop "our" blocks from the db <br>
	 * 4. update db with "peer's" chain <br>
	 */
	private void updateOurChain() {
		// copy back changes into the "real" nis cache
		this.nisCache.commit();

		Collection<Transaction> revertedTransactions = Collections.emptyList();
		if (this.hasOwnChain) {
			// mind that we're using "new" (replaced) nisCache
			final Set<Hash> transactionHashes = this.peerChain.stream().flatMap(bl -> bl.getTransactions().stream())
					.map(HashUtils::calculateHash).collect(Collectors.toSet());
			revertedTransactions = this.getRevertedTransactions(transactionHashes, this.parentBlock.getHeight().getRaw(),
					this.originalNisCache.getAccountCache());
		}

		this.blockChainLastBlockLayer.dropDbBlocksAfter(this.parentBlock.getHeight());

		final List<Transaction> transactionsToRemove = new ArrayList<>();
		this.peerChain.stream().forEach(block -> {
			this.blockChainLastBlockLayer.addBlockToDb(block);
			transactionsToRemove.addAll(block.getTransactions());
		});

		// update the unconfirmed transactions
		// (as an optimization remove transactions first because removal will trigger a cache rebuild)
		this.unconfirmedTransactions.removeAll(transactionsToRemove);
		revertedTransactions.forEach(this.unconfirmedTransactions::addExisting);
	}

	private Collection<Transaction> getRevertedTransactions(final Set<Hash> transactionHashes, final long wantedHeight,
			final AccountLookup accountCache) {
		long currentHeight = this.blockChainLastBlockLayer.getLastBlockHeight().getRaw();

		final List<Transaction> revertedTransactions = new ArrayList<>();
		final NisDbModelToModelMapper mapper = this.services.createMapper(accountCache);
		while (currentHeight != wantedHeight) {
			final DbBlock block = this.blockDao.findByHeight(new BlockHeight(currentHeight));

			// if the transaction is in db, we should add it to unconfirmed transactions without a db check
			// (otherwise, since it is not removed from the database, the database hash check would fail).
			// at this point, only "state" (in nisCache and so on) is reverted.
			// removing (our) transactions from the db, is one of the last steps, mainly because that I/O is expensive, so someone
			// could try to spam us with "fake" responses during synchronization (and therefore force us to drop our blocks).
			mapper.mapTransactionsIf(block, tr -> !transactionHashes.contains(tr.getTransferHash())).stream()
					.forEach(revertedTransactions::add);

			currentHeight--;
		}

		return revertedTransactions;
	}
}
