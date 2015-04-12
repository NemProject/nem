package org.nem.nis.state;

import org.nem.core.model.primitive.BlockHeight;

import java.util.HashMap;

/**
 * Container for historical account importance information.
 */
public class HistoricalImportances implements ReadOnlyHistoricalImportances {
	private final HashMap<BlockHeight, AccountImportance> importances = new HashMap<>();

	/**
	 * Creates a deep copy of this historical importances instance.
	 *
	 * @return A copy of this historical importances instance.
	 */
	public HistoricalImportances copy() {
		final HistoricalImportances copy = new HistoricalImportances();
		this.importances.values().stream().forEach(importance -> copy.addHistoricalImportance(importance.copy()));
		return copy;
	}

	@Override
	public double getHistoricalImportance(final BlockHeight height) {
		final AccountImportance importance = this.importances.get(height);
		return null == importance ? 0.0 : importance.getImportance(height);
	}

	@Override
	public double getHistoricalPageRank(final BlockHeight height) {
		final AccountImportance importance = this.importances.get(height);
		return null == importance ? 0.0 : importance.getLastPageRank();
	}

	@Override
	public int size() {
		return this.importances.size();
	}

	/**
	 * Adds an account importance to the historical importances.
	 *
	 * @param importance The account importance.
	 */
	public void addHistoricalImportance(final AccountImportance importance) {
		final BlockHeight height = importance.getHeight();
		if (null != this.importances.get(height)) {
			throw new IllegalArgumentException(String.format("historical importance already set at height %s", height));
		}

		this.importances.put(height, importance);
	}

	/**
	 * Clears the historical importances.
	 */
	public void prune() {
		this.importances.clear();
	}
}
