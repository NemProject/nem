package org.nem.core.model;

import java.util.ArrayList;
import java.util.List;

import org.nem.nis.BlockChain;

/**
 *
 */
public class CoinDays {
	
	public static final long MIN_BLOCK_WAIT = BlockChainConstants.REWRITE_LIMIT + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY; //1.5 days
	
	public static final long MAX_BLOCKS_CONSIDERED = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY * 100; //100 days

	private Amount unweightedBalance = Amount.ZERO;

	private List<CoinDay> coinDays = new ArrayList<>();

	/**
	 * Add a new coinday to the coindays.
	 * This method automatically groups coindays into 1440 block spans.
	 */
	public void addCoinDay(CoinDay coinDayToAdd) {

		if (coinDayToAdd == null || coinDayToAdd.getAmount() == null) {
			throw new IllegalArgumentException(
					"Coinday is null or contains a null amount.");
		}
		
		BlockHeight addingBlockHeight = coinDayToAdd.getHeight();

		if (coinDays == null || coinDays.size() < 1) {
			// ain't got no coindays
			coinDays = new ArrayList<CoinDay>();
			coinDays.add(coinDayToAdd);
			
		} else {
			int closestCoinDayIndex = -1;
			long closestCoinDay = Long.MAX_VALUE;

			for (int coinDayNdx = 0; coinDayNdx < this.coinDays.size(); coinDayNdx++) {
				CoinDay coinDay = coinDays.get(coinDayNdx);
				
				long blockHeightDiff = Math.abs(coinDay.getHeight().subtract(addingBlockHeight));
				if (blockHeightDiff > closestCoinDay) {
					closestCoinDayIndex = coinDayNdx;
				}
			}
			
			//Add the Amount in coinDayToAdd to the closest CoinDay, if it is within 1440 blocks.
			if (closestCoinDay <= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY && closestCoinDayIndex >= 0) {
				coinDays.get(closestCoinDayIndex).addAmount(coinDayToAdd.getAmount());
			}
		}
	}

	/**
	 * This method applies the <code>coindays</code> to the balance and returns
	 * the result.
	 * 
	 * @return
	 */
	public Amount getCoinDayWeightedBalance(BlockHeight currentBlockHeight) {
		long coinDayBalance = 0L; // XXX: we might want to cache this in the future
		long runningBalance = 0;

		for (CoinDay coinDay : coinDays) {
			long blockDiff = currentBlockHeight.subtract(coinDay.getHeight());

			if (blockDiff < MIN_BLOCK_WAIT || blockDiff > MAX_BLOCKS_CONSIDERED) {
				continue; // skip blocks younger than MIN_BLOCK_WAIT or over the max number of blocks
							// considered, then skip it
			}

			//We want to lower the weight so that the first MIN_BLOCK_WAIT don't count
			// TODO: that must be redone, we definitelly don't want to have that double here....
			double weighting = (1d * blockDiff - MIN_BLOCK_WAIT) / MAX_BLOCKS_CONSIDERED;

			long currentBalance = coinDay.getAmount().getNumMicroNem();
			coinDayBalance += weighting * currentBalance;
			runningBalance += currentBalance;
		}

		setUnweightedBalance(runningBalance);

		return Amount.fromMicroNem(coinDayBalance);
	}

	/**
	 * @return the unweightedBalance
	 */
	public Amount getUnweightedBalance() {
		return unweightedBalance;
	}

	/**
	 * @param unweightedBalance
	 *            the unweightedBalance to set
	 */
	private void setUnweightedBalance(long unweightedBalance) {
		this.unweightedBalance = Amount.fromMicroNem(unweightedBalance);
	}

}
