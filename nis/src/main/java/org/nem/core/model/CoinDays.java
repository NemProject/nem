package org.nem.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CoinDays {
	
	public static final long BLOCK_HEIGHT_GROUPING  = 1440;
	
	public static final long MIN_BLOCK_WAIT = 2160; //1.5 days
	
	public static final long MAX_BLOCKS_CONSIDERED = 144000; //100 days

	private long unweightedBalance = 0l;

	private List<CoinDay> coinDays;

	/**
	 * Add a new coinday to the coindays.
	 * This method automatically groups coindays into 1440 block spans.
	 */
	public void addCoinDay(CoinDay coinDayToAdd) {

		if (coinDayToAdd == null || coinDayToAdd.getAmount() == null) {
			throw new IllegalArgumentException(
					"Coinday is null or contains a null amount.");
		}
		
		long addingBlockHeight = coinDayToAdd.getBlockHeight();

		if (coinDays == null || coinDays.size() < 1) {
			// ain't got no coindays
			coinDays = new ArrayList<CoinDay>();
			coinDays.add(coinDayToAdd);
			
		} else {
			int closestCoinDayIndex = -1;
			long closestCoinDay = Long.MAX_VALUE;

			for (int coinDayNdx = 0; coinDayNdx < this.coinDays.size(); coinDayNdx++) {
				CoinDay coinDay = coinDays.get(coinDayNdx);
				
				long blockHeightDiff = Math.abs(coinDay.getBlockHeight() - addingBlockHeight);
				if (blockHeightDiff > closestCoinDay) {
					closestCoinDayIndex = coinDayNdx;
				}
			}
			
			//Add the Amount in coinDayToAdd to the closest CoinDay, if it is within 1440 blocks.
			if (closestCoinDay <= BLOCK_HEIGHT_GROUPING && closestCoinDayIndex >= 0) {
				Amount currAmt = coinDays.get(closestCoinDayIndex).getAmount();
				Amount addedAmt = currAmt.add(coinDayToAdd.getAmount());
				coinDays.get(closestCoinDayIndex).setAmount(addedAmt);
			}
		}
	}

	/**
	 * This method applies the <code>coindays</code> to the balance and returns
	 * the result.
	 * 
	 * @return
	 */
	public long getCoinDayWeightedBalance(long currentBlockHeight) {
		long coinDayBalance = 0; // XXX: we might want to cache this in the
									// future

		long runningBalance = 0;

		for (CoinDay coinDay : coinDays) {
			long blockDiff = currentBlockHeight - coinDay.getBlockHeight();

			if (blockDiff < MIN_BLOCK_WAIT || blockDiff > MAX_BLOCKS_CONSIDERED) {
				continue; // skip blocks over the max number of blocks
							// considered
			}

			double currAmount = coinDay.getAmount().getNumMicroNem();

			double weighting = 1d * blockDiff / MAX_BLOCKS_CONSIDERED;

			coinDayBalance += weighting * currAmount;

			runningBalance += currAmount;
		}

		setUnweightedBalance(runningBalance);

		return coinDayBalance;
	}

	/**
	 * @return the unweightedBalance
	 */
	public long getUnweightedBalance() {
		return unweightedBalance;
	}

	/**
	 * @param unweightedBalance
	 *            the unweightedBalance to set
	 */
	public void setUnweightedBalance(long unweightedBalance) {
		this.unweightedBalance = unweightedBalance;
	}

}
