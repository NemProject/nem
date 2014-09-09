package org.nem.nis;

import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.poi.PoiAccountState;
import org.nem.nis.poi.PoiFacade;
import org.nem.nis.poi.RemoteState;
import org.nem.nis.secret.AccountImportance;
import org.nem.nis.secret.BlockChainConstants;

import java.math.BigInteger;
import java.rmi.Remote;

/**
 * Provides functions for scoring block hits and targets.
 */
public class BlockScorer {

	/**
	 * BigInteger constant 2^64
	 */
	public static final BigInteger TWO_TO_THE_POWER_OF_64 = new BigInteger("18446744073709551616");

	/**
	 * BigInteger constant 2^56
	 */
	public static final long TWO_TO_THE_POWER_OF_54 = 18014398509481984L;

	/**
	 * Number of blocks which the calculation of difficulty should include
	 */
	public static final int NUM_BLOCKS_FOR_AVERAGE_CALCULATION = 60;

	/**
	 * Helper constant calculating the logarithm of BigInteger
	 */
	private static final double TWO_TO_THE_POWER_OF_256 = Math.pow(2.0, 256.0);

	/**
	 * Number of blocks that should be treated as a group for POI purposes.
	 * In other words, POI importances will only be calculated at blocks that
	 * are a multiple of this grouping number.
	 */
	private static final int POI_GROUPING = 31;

	public final PoiFacade poiFacade;

	public BlockScorer(final PoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	/**
	 * Forces a recalculation of the importances the next time calculateForgerBalance() is called.
	 */
	public void forceImportanceCalculation() {
		// TODO: fix me1
		//		this.poiFacade();
	}

	/**
	 * Calculates the hit score for block.
	 *
	 * @param block The block.
	 * @return the hit score.
	 */
	public BigInteger calculateHit(final Block block) {
		BigInteger val = new BigInteger(1, block.getGenerationHash().getRaw());
		final double tmp = Math.abs(Math.log(val.doubleValue() / TWO_TO_THE_POWER_OF_256));
		val = BigInteger.valueOf((long)(TWO_TO_THE_POWER_OF_54 * tmp));
		return val;
	}

	/**
	 * Calculates the target score for block given the previous block using an external block signer account.
	 *
	 * @param prevBlock The previous block.
	 * @param block The block.
	 * @return The target score.
	 */
	public BigInteger calculateTarget(final Block prevBlock, final Block block) {
		final int timeStampDifference = block.getTimeStamp().subtract(prevBlock.getTimeStamp());
		if (timeStampDifference < 0) {
			return BigInteger.ZERO;
		}

		final long forgerBalance = this.calculateForgerBalance(block);
		return BigInteger.valueOf(timeStampDifference)
				.multiply(BigInteger.valueOf(forgerBalance))
				.multiply(TWO_TO_THE_POWER_OF_64)
				.divide(block.getDifficulty().asBigInteger());
	}

	/**
	 * Gets the grouped height for the specified un-grouped height.
	 *
	 * @param height The un-grouped height.
	 * @return The grouped height.
	 */
	public BlockHeight getGroupedHeight(final BlockHeight height) {
		final long backInTime = height.getRaw() - 1;
		final long grouped = (backInTime / POI_GROUPING) * POI_GROUPING;
		return 0 == grouped ? BlockHeight.ONE : new BlockHeight(grouped);
	}

	/**
	 * Returns PoiAccountState for an address.
	 * If address is remote address, finds owner, and returns state of an owner.
	 *
	 * @param poiFacade Poi facade to use for searching.
	 * @param signerAddress Address of an account.
	 * @return PoiAccountState.
	 */
	public static PoiAccountState getForwardedAccountState(final PoiFacade poiFacade, final Address signerAddress) {
		PoiAccountState accountState = poiFacade.findStateByAddress(signerAddress);
		//@formatter:off
		if (accountState.hasRemoteState() &&
				accountState.getRemoteState().getDirection() == ImportanceTransferTransactionMode.Activate &&
				!accountState.getRemoteState().isOwner()) {
			accountState = poiFacade.findStateByAddress(accountState.getRemoteState().getRemoteAddress());
		}
		//@formatter:on
		return accountState;
	}


	private PoiAccountState getForwardedAccountState(final PoiFacade poiFacade, final Address signerAddress, final BlockHeight height) {
		PoiAccountState poiAccountState = poiFacade.findStateByAddress(signerAddress);
		if (poiAccountState.hasRemoteState()) {
			final RemoteState rState = poiAccountState.getRemoteState();
			final int mode = rState.getDirection();
			final boolean activate = (mode == ImportanceTransferTransactionMode.Activate);
			final boolean deactivate = (mode == ImportanceTransferTransactionMode.Deactivate);
			final long settingHeight = height.subtract(rState.getRemoteHeight());

			// only "valid" blocks should get here, so we have to deal only with two cases:
			//  * remote is active and operational,
			//  * remote isn'd deactivated yet
			// @formatter:off
			if (! rState.isOwner() &&
					(
							(activate && settingHeight >= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) ||
							(deactivate && settingHeight < BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)
					)) {
				poiAccountState = poiFacade.findStateByAddress(rState.getRemoteAddress());
			}
			// @formatter:on
		}
		return poiAccountState;
	}


	/**
	 * Calculates forager balance for block.
	 * This has the side-effect of recalculating importances.
	 *
	 * @param block The signed, "hit" block.
	 * @return The forager balance.
	 */
	public long calculateForgerBalance(final Block block) {
		final BlockHeight blockHeight = this.getGroupedHeight(block.getHeight());
		this.poiFacade.recalculateImportances(blockHeight);
		final long multiplier = NemesisBlock.AMOUNT.getNumNem();
		final Address signerAddress = block.getSigner().getAddress();
		final PoiAccountState accountState = getForwardedAccountState(this.poiFacade, signerAddress, block.getHeight());
		final AccountImportance accountImportance = accountState.getImportanceInfo();
		return (long)(accountImportance.getImportance(blockHeight) * multiplier);
	}

	/**
	 * Calculates the block score for the specified block.
	 *
	 * @param currentBlock The currently analyzed block.
	 * @return The block score.
	 */
	public long calculateBlockScore(final Block parentBlock, final Block currentBlock) {
		final int timeDiff = currentBlock.getTimeStamp().subtract(parentBlock.getTimeStamp());
		return this.calculateBlockScoreImpl(timeDiff, currentBlock.getDifficulty().getRaw());
	}

	private long calculateBlockScoreImpl(final int timeDiff, final long difficulty) {
		return difficulty - timeDiff;
	}

	/**
	 * Returns the default block difficulty scorer.
	 *
	 * @return The block difficulty scorer.
	 */
	public BlockDifficultyScorer getDifficultyScorer() {
		return new BlockDifficultyScorer();
	}
}
