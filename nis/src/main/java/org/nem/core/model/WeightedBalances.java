package org.nem.core.model;

import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.stream.Collectors;

// TODO: needs more comprehensive tests around edge cases

/**
 * Container for vested balances.
 *
 * Methods of this class, assume, that they are called in paired order
 */
public class WeightedBalances {

	private final List<WeightedBalance> balances;
	// TODO: this should not be a public field
	public final HistoricalBalances historicalBalances;

	/**
	 * Creates a new weighted balances instance.
	 */
	public WeightedBalances() {
		this(new ArrayList<>(), new HistoricalBalances());
	}

	private WeightedBalances(final List<WeightedBalance> balances, final HistoricalBalances historicalBalances) {
		this.balances = balances;
		this.historicalBalances = historicalBalances;
	}

	/**
	 * Creates a deep copy of this weighted balances instance.
	 *
	 * @return A copy of this weighted balances instance.
	 */
	public WeightedBalances copy() {
		return new WeightedBalances(
				balances.stream().map(WeightedBalance::copy).collect(Collectors.toList()),
				this.historicalBalances.copy());
	}

	private WeightedBalance createReceive(final WeightedBalance parent, final BlockHeight blockHeight, final Amount amount) {
		return parent.createReceive(blockHeight, amount);
	}

	private WeightedBalance createSend(final WeightedBalance parent, final BlockHeight blockHeight, final Amount amount) {
		return parent.createSend(blockHeight, amount);
	}

	/**
	 * Adds fully vested amount at height.
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void addFullyVested(final BlockHeight height, final Amount amount) {
		this.historicalBalances.add(height, amount);
		this.balances.add(WeightedBalance.createVested(height, amount));
	}
	/**
	 * Adds receive operation of amount at height.
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void addReceive(final BlockHeight height, final Amount amount) {
		this.historicalBalances.add(height, amount);

		if (! this.balances.isEmpty()) {
			int idx = this.balances.size() - 1;
			final WeightedBalance last = this.balances.get(idx);
			if (height.compareTo(last.getBlockHeight()) < 0) {
				throw new IllegalArgumentException("invalid height passed to addReceive");
			}
			iterateBalances(height);
		}

		final WeightedBalance prev = this.balances.isEmpty() ? WeightedBalance.ZERO : this.balances.get(this.balances.size() - 1);
		this.balances.add(createReceive(prev, height, amount));
	}

	/**
	 * Undoes receive operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void undoReceive(final BlockHeight height, final Amount amount) {
		this.historicalBalances.subtract(height, amount);

		this.undoChain(height);
		int idx = this.balances.size() - 1;
		final WeightedBalance last = this.balances.get(idx);

		if (last.getBlockHeight().equals(height) && last.getAmount().equals(amount)) {
			this.balances.remove(idx);

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
		this.historicalBalances.subtract(height, amount);

		if (! this.balances.isEmpty()) {
			int idx = this.balances.size() - 1;
			final WeightedBalance last = this.balances.get(idx);
			if (height.compareTo(last.getBlockHeight()) < 0) {
				throw new IllegalArgumentException("invalid height passed to addSend");
			}
			iterateBalances(height);
		}

		final WeightedBalance prev = this.balances.isEmpty() ? WeightedBalance.ZERO : this.balances.get(this.balances.size() - 1);
		this.balances.add(createSend(prev, height, amount));
	}

	/**
	 * Undoes send operation of amount at height
	 *
	 * @param height The height.
	 * @param amount The amount.
	 */
	public void undoSend(final BlockHeight height, final Amount amount) {
		this.historicalBalances.add(height, amount);

		this.undoChain(height);
		int idx = this.balances.size() - 1;
		final WeightedBalance last = this.balances.get(idx);

		if (last.getBlockHeight().equals(height) && last.getAmount().equals(amount)) {
			this.balances.remove(idx);

		} else {
			throw new IllegalArgumentException("trying to undo non-existent receive or too far in past");
		}
	}

	private int findElement(final BlockHeight height) {
		if (! this.balances.isEmpty()) {
			this.iterateBalances(height);
		}
		int index = Collections.binarySearch(this.balances, WeightedBalance.ZERO.createReceive(height, Amount.ZERO));
		if (index < 0) {
			index=-2-index;
			// if index is negative here it's probably wrong anyway,
		} else {
			index = findLast(this.balances, index);
		}
		return index;
	}
	/**
	 * Gets the vested amount at the specified height.
	 *
	 * @param height The height.
	 * @return The vested amount.
	 */
	public Amount getVested(final BlockHeight height) {
		if (balances.size() == 0) {
			return Amount.ZERO;
		}
		final int index = findElement(height);
		return this.balances.get(index).getVestedBalance();
	}

	/**
	 * Gets the unvested amount at the specified height.
	 *
	 * @param height The height.
	 * @return The unvested amount.
	 */
	public Amount getUnvested(final BlockHeight height) {
		if (balances.size() == 0) {
			return Amount.ZERO;
		}
		final int index = findElement(height);
		return this.balances.get(index).getUnvestedBalance();
	}

	public int size() {
		return this.balances.size();
	}

	public void convertToFullyVested() {
		if (this.balances.size() > 1) {
			throw new IllegalArgumentException("invalid call to convertToFullyVested " + this.balances.size());
		}
		final WeightedBalance weightedBalance =  this.balances.get(0);
		this.undoReceive(weightedBalance.getBlockHeight(), weightedBalance.getBalance());
		addFullyVested(weightedBalance.getBlockHeight(), weightedBalance.getBalance());
	}

	private int findLast(final List<WeightedBalance> balances, int index) {
		final BlockHeight current = balances.get(index).getBlockHeight();
		while (index < balances.size() - 1) {
			if (balances.get(index+1).getBlockHeight().equals(current)) {
				index++;
			} else {
				break;
			}
		}
		return index;
	}

	// requires non-empty balances list
	private void iterateBalances(final BlockHeight height) {
		int idx = this.balances.size() - 1;
		long h = this.balances.get(idx).getBlockHeight().getRaw();
		long multiple = ((h + BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY - 1)/BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY)* BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;

		while (height.getRaw() > multiple) {
			final WeightedBalance prev = this.balances.get(this.balances.size() - 1);
			this.balances.add(prev.next());
			multiple += BlockChainConstants.ESTIMATED_BLOCKS_PER_DAY;
		}
	}

	private void undoChain(final BlockHeight height) {
		while (this.balances.size() > 1) {
			if (this.balances.get(this.balances.size()-1).getBlockHeight().compareTo(height) > 0) {
				balances.remove(this.balances.size()-1);
			} else {
				break;
			}
		}
	}
}
