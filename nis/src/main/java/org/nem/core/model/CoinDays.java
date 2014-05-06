package org.nem.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CoinDays {
	
	public static final long MIN_BLOCK_WAIT = BlockChainConstants.REWRITE_LIMIT + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY; //1.5 days
	
	public static final long MAX_BLOCKS_CONSIDERED = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY * 100; //100 days


	private List<HistoricalBalance> historicalBalances = new ArrayList<>();

	/**
	 * Add a new historical balance to the coindays.
	 * This method automatically groups coindays into 1440 block spans.
	 */
	public void addHistoricalBalance(HistoricalBalance historicalBalance) {

		if (historicalBalance == null || historicalBalance.getBalance() == null) {
			throw new IllegalArgumentException(
					"Coinday is null or contains a null amount.");
		}
		
		BlockHeight addingBlockHeight = historicalBalance.getHeight();

		if (historicalBalances == null || historicalBalances.size() < 1) {
			historicalBalances = new ArrayList<HistoricalBalance>();
			historicalBalances.add(historicalBalance);
			
		} else {
			int closestCoinDayIndex = -1;
			long closestCoinDay = Long.MAX_VALUE;

			for (int coinDayNdx = 0; coinDayNdx < this.historicalBalances.size(); coinDayNdx++) {
				HistoricalBalance coinDay = historicalBalances.get(coinDayNdx);
				
				long blockHeightDiff = Math.abs(coinDay.getHeight().subtract(addingBlockHeight));
				if (blockHeightDiff > closestCoinDay) {
					closestCoinDayIndex = coinDayNdx;
				}
			}
			
			//Add the Amount in historicalBalance to the closest HistoricalBalance, if it is within 1440 blocks.
			if (closestCoinDay <= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY && closestCoinDayIndex >= 0) {
				historicalBalances.get(closestCoinDayIndex).add(historicalBalance.getBalance());
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

		for (HistoricalBalance historicalBalance : historicalBalances) {
			long blockDiff = blockHeight.subtract(historicalBalance.getHeight());

			if (blockDiff < 0 || blockDiff < MIN_BLOCK_WAIT || blockDiff > MAX_BLOCKS_CONSIDERED) {
				continue; // skip blocks younger than the given blockHeight, 
						  // younger than MIN_BLOCK_WAIT,
						  // or older than the max number of block considered
			}

			//We want to lower the weight so that the first MIN_BLOCK_WAIT don't count
			// TODO: that must be redone, we definitelly don't want to have that double here....
			// TODO: weight this a different way that doesn't require a double 
			double weight = (1d * blockDiff - MIN_BLOCK_WAIT) / MAX_BLOCKS_CONSIDERED;

			long currentBalance = historicalBalance.getBalance().getNumMicroNem();
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
		return historicalBalances.size();
	}
}
