package org.nem.nis.state;

import org.nem.core.model.NemGlobals;
import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WeightedBalances implementation that converts balances from unvested to vested as time passes.
 */
public class TimeBasedVestingWeightedBalances implements WeightedBalances {

	private final List<WeightedBalance> balances;

	/**
	 * Creates a new weighted balances instance.
	 */
	public TimeBasedVestingWeightedBalances() {
		this(new ArrayList<>());
	}

	private TimeBasedVestingWeightedBalances(final List<WeightedBalance> balances) {
		this.balances = balances;
	}

	// region ReadOnlyWeightedBalances

	@Override
	public int size() {
		return this.balances.size();
	}

	@Override
	public Amount getVested(final BlockHeight height) {
		return this.getAmountSafe(height, WeightedBalance::getVestedBalance);
	}

	@Override
	public Amount getUnvested(final BlockHeight height) {
		return this.getAmountSafe(height, WeightedBalance::getUnvestedBalance);
	}

	private Amount getAmountSafe(final BlockHeight height, final Function<WeightedBalance, Amount> getAmount) {
		if (this.balances.isEmpty()) {
			return Amount.ZERO;
		}

		final int index = this.findElement(height);
		if (index < 0) {
			// This can happen during pruning.
			// An index < 0 here means that all elements in this.balances (if any) have a height smaller than the given height.
			// The corresponding account had no receives up to the given block height which means the unvested part is 0.
			return Amount.ZERO;
		}

		return getAmount.apply(this.balances.get(index));
	}

	// endregion

	// region WeightedBalances

	@Override
	public TimeBasedVestingWeightedBalances copy() {
		return new TimeBasedVestingWeightedBalances(this.balances.stream().map(WeightedBalance::copy).collect(Collectors.toList()));
	}

	@Override
	public void addFullyVested(final BlockHeight height, final Amount amount) {
		this.balances.add(WeightedBalance.createVested(height, amount));
	}

	@Override
	public void addReceive(final BlockHeight height, final Amount amount) {
		if (!this.balances.isEmpty()) {
			final int idx = this.balances.size() - 1;
			final WeightedBalance last = this.balances.get(idx);
			if (height.compareTo(last.getBlockHeight()) < 0) {
				throw new IllegalArgumentException("invalid height passed to addReceive");
			}
			this.iterateBalances(height);
		}

		final WeightedBalance prev = this.balances.isEmpty() ? WeightedBalance.ZERO : this.balances.get(this.balances.size() - 1);
		this.balances.add(prev.createReceive(height, amount));
	}

	@Override
	public void undoReceive(final BlockHeight height, final Amount amount) {
		this.undoChain(height);
		final int idx = this.balances.size() - 1;
		final WeightedBalance last = this.balances.get(idx);

		if (last.getBlockHeight().equals(height) && last.getAmount().equals(amount)) {
			this.balances.remove(idx);
		} else {
			throw new IllegalArgumentException("trying to undo non-existent receive or too far in past");
		}
	}

	@Override
	public void addSend(final BlockHeight height, final Amount amount) {
		if (!this.balances.isEmpty()) {
			final int idx = this.balances.size() - 1;
			final WeightedBalance last = this.balances.get(idx);
			if (height.compareTo(last.getBlockHeight()) < 0) {
				throw new IllegalArgumentException("invalid height passed to addSend");
			}
			this.iterateBalances(height);
		}

		final WeightedBalance prev = this.balances.isEmpty() ? WeightedBalance.ZERO : this.balances.get(this.balances.size() - 1);
		this.balances.add(prev.createSend(height, amount));
	}

	@Override
	public void undoSend(final BlockHeight height, final Amount amount) {
		this.undoChain(height);
		final int idx = this.balances.size() - 1;
		final WeightedBalance last = this.balances.get(idx);

		if (last.getBlockHeight().equals(height) && last.getAmount().equals(amount)) {
			this.balances.remove(idx);
		} else {
			throw new IllegalArgumentException("trying to undo non-existent send or too far in past");
		}
	}

	private int findElement(final BlockHeight height) {
		if (!this.balances.isEmpty()) {
			this.iterateBalances(height);
		}
		int index = Collections.binarySearch(this.balances, WeightedBalance.ZERO.createReceive(height, Amount.ZERO));
		if (index < 0) {
			index = -2 - index;
			// if index is negative here it's probably wrong anyway,
		} else {
			index = this.findLast(this.balances, index);
		}
		return index;
	}

	@Override
	public void convertToFullyVested() {
		if (1 != this.balances.size()) {
			throw new IllegalArgumentException("invalid call to convertToFullyVested " + this.balances.size());
		}
		final WeightedBalance weightedBalance = this.balances.get(0);
		if (!weightedBalance.getBlockHeight().equals(BlockHeight.ONE)) {
			throw new IllegalArgumentException("invalid call to convertToFullyVested at height " + weightedBalance.getBlockHeight());
		}

		this.undoReceive(weightedBalance.getBlockHeight(), weightedBalance.getBalance());
		this.addFullyVested(weightedBalance.getBlockHeight(), weightedBalance.getBalance());
	}

	private int findLast(final List<WeightedBalance> balances, int index) {
		final BlockHeight current = balances.get(index).getBlockHeight();
		while (index < balances.size() - 1) {
			if (balances.get(index + 1).getBlockHeight().equals(current)) {
				index++;
			} else {
				break;
			}
		}
		return index;
	}

	// requires non-empty balances list
	private void iterateBalances(final BlockHeight height) {
		final int idx = this.balances.size() - 1;
		final long h = this.balances.get(idx).getBlockHeight().getRaw();
		final long estimatedBlocksPerDay = NemGlobals.getBlockChainConfiguration().getEstimatedBlocksPerDay();
		long multiple = ((h + estimatedBlocksPerDay - 1) / estimatedBlocksPerDay) * estimatedBlocksPerDay;

		while (height.getRaw() > multiple) {
			final WeightedBalance prev = this.balances.get(this.balances.size() - 1);
			this.balances.add(prev.next());
			multiple += estimatedBlocksPerDay;
		}
	}

	@Override
	public void undoChain(final BlockHeight height) {
		while (this.balances.size() > 1) {
			if (this.balances.get(this.balances.size() - 1).getBlockHeight().compareTo(height) > 0) {
				this.balances.remove(this.balances.size() - 1);
			} else {
				break;
			}
		}
	}

	@Override
	public void prune(final BlockHeight minHeight) {
		final Amount vested = this.getVested(minHeight);
		final Amount unvested = this.getUnvested(minHeight);

		final WeightedBalance consolidatedBalance = WeightedBalance.create(minHeight, vested, unvested);
		this.balances.removeIf(balance -> balance.getBlockHeight().compareTo(minHeight) <= 0);
		this.balances.add(0, consolidatedBalance);
	}

	// endregion
}
