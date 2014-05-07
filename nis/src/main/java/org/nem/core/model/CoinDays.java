package org.nem.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class CoinDays {
	
	public static final long MIN_BLOCK_WAIT = BlockChainConstants.REWRITE_LIMIT + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY; //1.5 days
	
	public static final long MAX_BLOCKS_CONSIDERED = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY * 100; //100 days

	private final List<CoinDay> coindays = new ArrayList<>();

	/**
	 * Add a new coinday to the coindays.
	 * This method automatically groups coindays into BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY block spans.
	 *
	 * note: this assumes that coindays are added IN ORDER
	 */
	public void addCoinDay(final CoinDay rawCoinDay) {
		long h = calculateBucket(rawCoinDay.getHeight());
		final CoinDay coinDay = new CoinDay(new BlockHeight(h), rawCoinDay.getBalance());

		int index = Collections.binarySearch(coindays, coinDay);
		if (index >= 0) {
			coindays.get(index).add(coinDay.getBalance());
		} else {
			coindays.add(-index-1, coinDay);
		}
	}

	// "sub" is a wrong name, this will mostly be used in "undoing" so I've temporarily choosen revert
	public void revertCoinDay(CoinDay rawCoinDay) {
		long h = calculateBucket(rawCoinDay.getHeight());
		final CoinDay coinDay = new CoinDay(new BlockHeight(h), rawCoinDay.getBalance());

		int index = Collections.binarySearch(coindays, coinDay);
		if (index >= 0) {
			coindays.get(index).subtract(coinDay.getBalance());
		} else {
			throw new IllegalArgumentException("Trying to revert unknown coinday");
		}
	}

	/**
	 * This method applies the calculates the weighted balance in historicalBalances for the currentBlockHeight
	 * 
	 * @return
	 */
	public CoinDayAmount getCoinDayWeightedBalance(final BlockHeight blockHeight) {
		long coinDayBalance = 0l;
		long fullBalance = 0l;

		long h = calculateBucket(blockHeight);
		for (final CoinDay coinDay: coindays) {
			if (coinDay.getHeight().getRaw() >= h) {
				break;
			}
			long blockDiff =  h - coinDay.getHeight().getRaw();

			if (blockDiff > MAX_BLOCKS_CONSIDERED) {
				break;
			}

			final long weight = blockDiff / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

			long currentBalance = coinDay.getBalance().getNumMicroNem();
			coinDayBalance += weight * currentBalance;
			fullBalance += currentBalance;
		}

		return new CoinDayAmount(Amount.fromMicroNem(fullBalance), Amount.fromMicroNem(coinDayBalance / 100));
	}

	private long calculateBucket(BlockHeight blockHeight) {
		return  ((blockHeight.getRaw() + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY - 1) / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	}

	/**
	 * Gets the size of the list
	 * 
	 * @return the size of the list
	 */
	public int size() {
		return coindays.size();
	}
	
	/**
	 * Method for finding the closest bucket of coindays (1440 blocks), if it exists in <code>coinDays</code>.
	 * TODO: keep this.coinDays sorted and use binary search.
	 * 
	 * @param input - CoinDay we are trying to find a bucket for 
	 * @return the index in <code>coinDays</code> of the closest coinday, -1 otherwise.
	 */
	private int findClosestCoinDayBucket(CoinDay input) {
		int closestCoinDayIndex = -1;
		long closestCoinDay = Long.MAX_VALUE;
		
		BlockHeight inputBlockHeight = input.getHeight();

		for (int coinDayNdx = 0; coinDayNdx < this.coindays.size(); coinDayNdx++) {
			CoinDay currCoinDay = this.coindays.get(coinDayNdx);
			
			long blockHeightDiff = Math.abs(currCoinDay.getHeight().subtract(inputBlockHeight));
			if (blockHeightDiff > closestCoinDay && closestCoinDay <= BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) {
				closestCoinDayIndex = coinDayNdx;
				closestCoinDay = blockHeightDiff;
			}
		}
		
		return closestCoinDayIndex;
	}
}
