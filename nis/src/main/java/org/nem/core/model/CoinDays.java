package org.nem.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CoinDays {
	
	public static final long MIN_BLOCK_WAIT = BlockChainConstants.REWRITE_LIMIT + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY; //1.5 days
	
	public static final long MAX_BLOCKS_CONSIDERED = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY * 100; //100 days


	private List<CoinDay> coindays = new ArrayList<CoinDay>();

	/**
	 * Add a new historical balance to the coindays.
	 * This method automatically groups coindays into 1440 block spans.
	 */
	public void addCoinDay(CoinDay coinDay) {

		if (coinDay == null || coinDay.getBalance() == null) {
			throw new IllegalArgumentException(
					"CoinDayis null or contains a null amount.");
		}
		
		BlockHeight addingBlockHeight = coinDay.getHeight();

		if (coindays == null || coindays.size() < 1) {
			coindays = new ArrayList<CoinDay>();
			coindays.add(coinDay);
			
		} else {
			int closestCoinDayIndex = -1;
			long closestCoinDay = Long.MAX_VALUE;

			for (int coinDayNdx = 0; coinDayNdx < this.coindays.size(); coinDayNdx++) {
				CoinDay currCoinDay = coindays.get(coinDayNdx);
				
				long blockHeightDiff = Math.abs(currCoinDay.getHeight().subtract(addingBlockHeight));
				if (blockHeightDiff > closestCoinDay) {
					closestCoinDayIndex = coinDayNdx;
				}
			}
			
			//Add the Amount to the closest coinday, if it is within 1440 blocks.
			if (closestCoinDay <= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY && closestCoinDayIndex >= 0) {
				coindays.get(closestCoinDayIndex).add(coinDay.getBalance());
			} else {
				coindays.add(coinDay);
			}
		}
	}

	/**
	 * This method applies the calculates the weighted balance in historicalBalances for the currentBlockHeight
	 * 
	 * @return
	 */
	public CoinDayAmount getCoinDayWeightedBalance(BlockHeight blockHeight) {
		long coinDayBalance = 0l;
		long runningBalance = 0l;

		for (CoinDay coinDay: coindays) {
			long blockDiff = blockHeight.subtract(coinDay.getHeight());

			if (blockDiff < 0 || blockDiff < MIN_BLOCK_WAIT || blockDiff > MAX_BLOCKS_CONSIDERED) {
				continue; // skip blocks younger than the given blockHeight, 
						  // younger than MIN_BLOCK_WAIT,
						  // or older than the max number of block considered
			}

			//We want to lower the weight so that the first MIN_BLOCK_WAIT don't count
			// TODO: that must be redone, we definitelly don't want to have that double here....
			// TODO: weight this a different way that doesn't require a double 
			double weight = (1d * blockDiff - MIN_BLOCK_WAIT) / MAX_BLOCKS_CONSIDERED;

			long currentBalance = coinDay.getBalance().getNumMicroNem();
			coinDayBalance += weight * currentBalance;
			runningBalance += currentBalance;
		}

		return new CoinDayAmount(Amount.fromMicroNem(runningBalance), Amount.fromMicroNem(coinDayBalance));
	}

	/**
	 * Gets the size of the list
	 * 
	 * @return the size of the list
	 */
	public int size() {
		return coindays.size();
	}
}
