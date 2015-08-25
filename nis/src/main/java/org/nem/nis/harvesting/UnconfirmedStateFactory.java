package org.nem.nis.harvesting;

import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.secret.*;
import org.nem.nis.validators.TransactionValidatorFactory;

import java.util.function.*;

/**
 * Factory for creating unconfirmed state.
 */
public class UnconfirmedStateFactory {
	private final TransactionValidatorFactory validatorFactory;
	private final Function<NisCache, TransactionObserver> observerFactory;
	private final ReadOnlyNisCache nisCache;
	private final TimeProvider timeProvider;
	private final Supplier<BlockHeight> blockHeightSupplier;

	/**
	 * Creates an unconfirmed state factory.
	 *
	 * @param validatorFactory The validator factory.
	 * @param observerFactory The observer factory.
	 * @param nisCache The nis cache.
	 * @param timeProvider The time provider.
	 * @param blockHeightSupplier The block height supplier.
	 */
	public UnconfirmedStateFactory(
			final TransactionValidatorFactory validatorFactory,
			final Function<NisCache, TransactionObserver> observerFactory,
			final ReadOnlyNisCache nisCache,
			final TimeProvider timeProvider,
			final Supplier<BlockHeight> blockHeightSupplier) {
		this.validatorFactory = validatorFactory;
		this.observerFactory = observerFactory;
		this.nisCache = nisCache;
		this.timeProvider = timeProvider;
		this.blockHeightSupplier = blockHeightSupplier;
	}

	/**
	 * Creates unconfirmed state.
	 *
	 * @param nisCache The nis cache.
	 * @param transactions the unconfirmed transactions.
	 * @return The unconfirmed state.
	 */
	public DefaultUnconfirmedState create(
			final NisCache nisCache,
			final UnconfirmedTransactionsCache transactions) {
		final UnconfirmedBalancesObserver unconfirmedBalances = new UnconfirmedBalancesObserver(this.nisCache.getAccountStateCache());
		final UnconfirmedMosaicBalancesObserver unconfirmedMosaicBalances = new UnconfirmedMosaicBalancesObserver(nisCache.getNamespaceCache());
		return new DefaultUnconfirmedState(
				transactions,
				unconfirmedBalances,
				unconfirmedMosaicBalances,
				this.validatorFactory,
				this.observerFactory.apply(nisCache),
				new TransactionSpamFilter(this.nisCache, transactions),
				this.nisCache,
				this.timeProvider,
				this.blockHeightSupplier);
	}
}
