package org.nem.nis.secret;

import org.nem.nis.ForkConfiguration;
import org.nem.nis.cache.*;
import org.nem.nis.secret.pruning.*;

import java.util.EnumSet;

/**
 * Factory for creating BlockTransactionObserver objects.
 */
public class BlockTransactionObserverFactory {
	private static final int DEFAULT_ESTIMATED_BLOCKS_PER_YEAR = 60 * 24 * 365;
	private final EnumSet<ObserverOption> observerOptions;
	private final int estimatedBlocksPerYear;
	private final ForkConfiguration forkConfiguration;

	/**
	 * Creates a new transaction observer factory with no additional options.
	 *
	 * @param forkConfiguration The fork configuration.
	 */
	public BlockTransactionObserverFactory(final ForkConfiguration forkConfiguration) {
		this(EnumSet.noneOf(ObserverOption.class), DEFAULT_ESTIMATED_BLOCKS_PER_YEAR, forkConfiguration);
	}

	/**
	 * Creates a new transaction observer factory which uses additional options.
	 *
	 * @param observerOptions The observer options.
	 * @param forkConfiguration The fork configuration.
	 */
	public BlockTransactionObserverFactory(final EnumSet<ObserverOption> observerOptions, final ForkConfiguration forkConfiguration) {
		this(observerOptions, DEFAULT_ESTIMATED_BLOCKS_PER_YEAR, forkConfiguration);
	}

	/**
	 * Creates a new transaction observer factory which uses additional options and the estimated number of blocks per year.
	 *
	 * @param observerOptions The observer options.
	 * @param estimatedBlocksPerYear The estimated number of blocks per year.
	 * @param forkConfiguration The fork configuration.
	 */
	public BlockTransactionObserverFactory(final EnumSet<ObserverOption> observerOptions, final int estimatedBlocksPerYear,
			final ForkConfiguration forkConfiguration) {
		this.observerOptions = observerOptions;
		this.estimatedBlocksPerYear = estimatedBlocksPerYear;
		this.forkConfiguration = forkConfiguration;
	}

	/**
	 * Creates a block transaction observer that commits all changes in order. This observer is appropriate for an execute operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createExecuteCommitObserver(final NisCache nisCache) {
		return this.createBuilder(nisCache, this.observerOptions).build();
	}

	/**
	 * Creates a block transaction observer that commits all changes in reverse order. This observer is appropriate for an undo operation.
	 *
	 * @param nisCache The NIS cache.
	 * @return The observer.
	 */
	public BlockTransactionObserver createUndoCommitObserver(final NisCache nisCache) {
		return this.createBuilder(nisCache, this.observerOptions).buildReverse();
	}

	private AggregateBlockTransactionObserverBuilder createBuilder(final NisCache nisCache, final EnumSet<ObserverOption> options) {
		final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
		final AggregateBlockTransactionObserverBuilder builder = new AggregateBlockTransactionObserverBuilder();
		builder.add(new WeightedBalancesObserver(accountStateCache));
		builder.add(new AccountsHeightObserver(nisCache));
		builder.add(new BalanceCommitTransferObserver(accountStateCache));
		builder.add(new HarvestRewardCommitObserver(accountStateCache));
		builder.add(new RemoteObserver(accountStateCache, this.forkConfiguration.getMosaicRedefinitionForkHeight()));
		builder.add(new MultisigCosignatoryModificationObserver(accountStateCache));
		builder.add(new MultisigMinCosignatoriesModificationObserver(accountStateCache));
		builder.add(new TransactionHashesObserver(nisCache.getTransactionHashCache()));
		builder.add(new ProvisionNamespaceObserver(nisCache.getNamespaceCache(), accountStateCache));
		builder.add(new MosaicDefinitionCreationObserver(nisCache.getNamespaceCache()));
		builder.add(new MosaicSupplyChangeObserver(nisCache.getNamespaceCache(), accountStateCache));
		builder.add(new MosaicTransferObserver(nisCache.getNamespaceCache()));

		// depends on MosaicDefinitionCreationObserver and MosaicTransferObserver
		builder.add(new AccountInfoMosaicIdsObserver(nisCache.getNamespaceCache(), nisCache.getAccountStateCache()));
		builder.add(
				new ExpiredNamespacesObserver(nisCache.getNamespaceCache(), nisCache.getAccountStateCache(), this.estimatedBlocksPerYear));

		// pruners
		builder.add(new AccountStateCachePruningObserver(nisCache.getAccountStateCache(),
				!options.contains(ObserverOption.NoHistoricalDataPruning)));
		builder.add(new NamespaceCachePruningObserver(nisCache.getNamespaceCache()));
		builder.add(new TransactionHashCachePruningObserver(nisCache.getTransactionHashCache()));

		// optional observers
		if (!options.contains(ObserverOption.NoIncrementalPoi)) {
			builder.add(new RecalculateImportancesObserver(nisCache));
		}

		if (!options.contains(ObserverOption.NoOutlinkObserver)) {
			builder.add(new OutlinkObserver(accountStateCache));
		}

		return builder;
	}
}
