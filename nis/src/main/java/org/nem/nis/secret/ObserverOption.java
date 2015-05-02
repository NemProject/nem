package org.nem.nis.secret;

/**
 * Options to customize the observers created by the BlockTransactionObserverFactory.
 */
public enum ObserverOption {
	/**
	 * The default options.
	 */
	Default,

	/**
	 * Excludes the incremental poi observer.
	 */
	NoIncrementalPoi,

	/**
	 * No pruning of historical data.
	 */
	NoHistoricalDataPruning
}