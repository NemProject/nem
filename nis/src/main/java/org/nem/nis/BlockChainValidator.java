package org.nem.nis;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.poi.RemoteState;
import org.nem.nis.secret.BlockChainConstants;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.logging.Logger;

// TODO 20140909 J-G: why can't these checks happen in checkValidity? (i'm probably missing something)
// G-J: which checks?
// TODO 20140909 J-G: all of the changes to this class - i'm not sure why it needs to know about importance transfers at all
// it knows about transactions but i don't think it should know about concrete transactions

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {
	private static final Logger LOGGER = Logger.getLogger(BlockChainValidator.class.getName());
	private static final int MAX_ALLOWED_SECONDS_AHEAD_OF_TIME = 60;

	private final PoiFacade poiFacade;
	private final Consumer<Block> executor;
	private final BlockScorer scorer;
	private final int maxChainSize;
	private final Predicate<Hash> transactionExists;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param executor The block executor to use.
	 * @param scorer The block scorer to use.
	 * @param maxChainSize The maximum chain size.
	 */
	public BlockChainValidator(
			final PoiFacade poiFacade,
			final Consumer<Block> executor,
			final BlockScorer scorer,
			final int maxChainSize,
			final Predicate<Hash> transactionExists) {
		this.poiFacade = poiFacade;
		this.executor = executor;
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
		this.transactionExists = transactionExists;
	}

	public static boolean canAccountForageAtHeight(final RemoteState rState, final BlockHeight height) {
		final ImportanceTransferTransaction.Mode mode = ImportanceTransferTransaction.Mode.fromValueOrDefault(rState.getDirection());
		boolean activate = (mode == ImportanceTransferTransaction.Mode.Activate);
		boolean deactivate = (mode == ImportanceTransferTransaction.Mode.Deactivate);

		if (!activate && !deactivate) {
			throw new IllegalStateException("unhandled importance transfer mode");
		}
		long settingHeight = height.subtract(rState.getRemoteHeight());

		// remote already activated, or already deactivated
		if (((rState.isOwner() && activate) || (!rState.isOwner() && deactivate)) && (settingHeight >= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)) {
			return false;
		}

		// remote already activated, or already deactivated
		if (((rState.isOwner() && deactivate) || (!rState.isOwner() && activate)) && (settingHeight < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)) {
			return false;
		}
		return true;
	}

	private boolean couldSignerForage(final Block block) {
		final PoiAccountState accountState = this.poiFacade.findStateByAddress(block.getSigner().getAddress());
		if (accountState.hasRemoteState()) {
			if (!canAccountForageAtHeight(accountState.getRemoteState(), block.getHeight())) {
				return false;
			}
		}

		return true;
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
			return false;
		}

		final Set<Hash> chainHashes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight()) || !block.verify()) {
				return false;
			}

			// TODO-CR: not sure if i like having a hard dependency on NisMain here instead of injecting the TimeProvider
			final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
			if (block.getTimeStamp().compareTo(currentTime.addSeconds(MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0) {
				return false;
			}

			if (!couldSignerForage(block)) {
				return false;
			}

			if (!this.isBlockHit(parentBlock, block)) {
				LOGGER.fine(String.format("hit failed on block %s gen %s", block.getHeight(), block.getGenerationHash()));
				return false;
			}

			for (final Transaction transaction : block.getTransactions()) {
				if (ValidationResult.SUCCESS != transaction.checkValidity() ||
						!transaction.verify() ||
						transaction.getTimeStamp().compareTo(currentTime.addSeconds(MAX_ALLOWED_SECONDS_AHEAD_OF_TIME)) > 0 ||
						transaction.getSigner().equals(block.getSigner())) {
					return false;
				}

				if (transaction.getType() == TransactionTypes.IMPORTANCE_TRANSFER) {
					if (!verifyImportanceTransfer(this.poiFacade, block.getHeight(), (ImportanceTransferTransaction)transaction))
					{
						return false;
					}
				}

				if (block.getHeight().getRaw() >= BlockMarkerConstants.FATAL_TX_BUG_HEIGHT) {
					final Hash hash = HashUtils.calculateHash(transaction);
					if (this.transactionExists.test(hash) || chainHashes.contains(hash)) {
						LOGGER.info("received block with duplicate TX");
						return false;
					}
					chainHashes.add(hash);
				}
			}

			parentBlock = block;
			expectedHeight = expectedHeight.next();

			this.executor.accept(block);
		}

		return true;
	}

	public static boolean verifyImportanceTransfer(final PoiFacade poiFacade, final BlockHeight height, final ImportanceTransferTransaction transaction) {
		final PoiAccountState state = poiFacade.findStateByAddress(transaction.getSigner().getAddress());
		final int direction = transaction.getMode().value();
		if (direction == ImportanceTransferTransaction.Mode.Activate.value()) {
			if (state.hasRemoteState() && state.getRemoteState().getDirection() == direction) {
				return false;
			}
			// means there is previous state, which was "Deactivate" (due to check above)
			if (state.hasRemoteState() && height.subtract(state.getRemoteState().getRemoteHeight()) < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
				return false;
			}
			return true;
		} else if (direction == ImportanceTransferTransaction.Mode.Deactivate.value()) {
			if (!state.hasRemoteState() || state.getRemoteState().getDirection() == direction) {
				return false;
			}
			// means there is previous state which was "Activate"
			if (state.hasRemoteState() && height.subtract(state.getRemoteState().getRemoteHeight()) < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
