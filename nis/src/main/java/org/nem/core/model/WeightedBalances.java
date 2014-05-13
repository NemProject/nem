package org.nem.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for vested balances.
 *
 * Methods of this class, assume, that they are called in paired order
 */
public class WeightedBalances {
	/**
	 * Limit of history of balances (just not to let the list grow infinitely)
	 */
	public final long MAX_HISTORY = BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY + BlockChainConstants.REWRITE_LIMIT;

	private final List<WeightedBalance> balances;
	public final HistoricalBalances historicalBalances;

	private WeightedBalances(final HistoricalBalances historicalBalances) {
		this.balances = new ArrayList<>();
		this.historicalBalances = historicalBalances;
	}
	public WeightedBalances() {
		this(new HistoricalBalances());
	}

	public WeightedBalances copy() {
		return new WeightedBalances(this.historicalBalances.copy());
	}

	/**
	 * Adds receive operation of amount at height.
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void addReceive(final BlockHeight height, final Amount amount) {
		final WeightedBalance weightedBalance = createVestedBalance(height, amount);

		int index = Collections.binarySearch(balances, weightedBalance);
		if (index >= 0) {
			balances.get(index).receive(amount);

		} else {
			int newIndex = -index-1;
			if (newIndex == 0) {
				balances.add(newIndex, weightedBalance);

			} else {
				newIndex = iterateBalances(height, newIndex);
				balances.get(newIndex).receive(amount);
			}
		}
	}

	private WeightedBalance createVestedBalance(final BlockHeight height, final Amount amount) {
		long h = calculateBucket(height);
		return new WeightedBalance(new BlockHeight(h), amount);
	}

	/**
	 * Undoes receive operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void undoReceive(final BlockHeight height, final Amount amount) {
		final WeightedBalance weightedBalance = createVestedBalance(height, amount);

		int index = Collections.binarySearch(balances, weightedBalance);
		if (index >= 0) {
			index = undoIterateBalances(index);
			balances.get(index).undoReceive(amount);
		} else {
			throw new IllegalArgumentException("trying to undo non-existent receive or too far in past");
		}
	}

	/**
	 * Adds send operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void addSend(final BlockHeight height, final Amount amount) {
		final WeightedBalance weightedBalance = createVestedBalance(height, amount);

		int index = Collections.binarySearch(balances, weightedBalance);
		if (index >= 0) {
			balances.get(index).send(amount);

		} else {
			int newIndex = -index-1;
			if (newIndex == 0) {
				throw new IllegalArgumentException("trying to send from empty account");

			} else {
				newIndex = iterateBalances(height, newIndex);
				balances.get(newIndex).send(amount);
			}
		}
	}

	/**
	 * Undoes send operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void undoSend(final BlockHeight height, final Amount amount) {
		final WeightedBalance weightedBalance = createVestedBalance(height, amount);

		int index = Collections.binarySearch(balances, weightedBalance);
		if (index >= 0) {
			index = undoIterateBalances(index);
			balances.get(index).undoSend(amount);

		} else {
			throw new IllegalArgumentException("trying to undo non-existent send or too far in past");
		}
	}

	public Amount getUnvested(final BlockHeight height) {
		final WeightedBalance weightedBalance = createVestedBalance(height, Amount.ZERO);
		int index = Collections.binarySearch(balances, weightedBalance);
		if (index < 0) {
			index = -index-1;
			index = iterateBalances(height, index);
		}
		return balances.get(index).getUnvestedBalance();
	}

	private long calculateBucket(BlockHeight blockHeight) {
		return  ((blockHeight.getRaw() + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY - 1) / BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY) * BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
	}

	private int iterateBalances(final BlockHeight height, int newIndex) {
		newIndex -= 1;

		while (balances.get(newIndex).getBlockHeight().compareTo(height) <= 0) {
			balances.add(balances.get(newIndex).next());
			newIndex++;
		}
		return newIndex;
	}

	private int undoIterateBalances(int index) {
		int currentIndex = balances.size() - 1;
		while (currentIndex > index) {
			balances.remove(currentIndex);
			currentIndex--;
		}
		return currentIndex;
	}
}
