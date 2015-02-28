package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.secret.BlockTransactionObserverFactory;
import org.nem.nis.validators.*;

import java.util.List;

/**
 * A block aware NewBlockTransactionsProvider implementation.
 */
public class BlockAwareNewBlockTransactionsProvider implements NewBlockTransactionsProvider {
	private final NewBlockTransactionsProvider v1;
	private final NewBlockTransactionsProvider latest;

	/**
	 * Creates a new transactions provider.
	 *
	 * @param nisCache The NIS cache.
	 * @param validatorFactory The validator factory.
	 * @param blockValidatorFactory The block validator factory.
	 * @param observerFactory The observer factory.
	 * @param unconfirmedTransactions The unconfirmed transactions.
	 */
	public BlockAwareNewBlockTransactionsProvider(
			final ReadOnlyNisCache nisCache,
			final TransactionValidatorFactory validatorFactory,
			final BlockValidatorFactory blockValidatorFactory,
			final BlockTransactionObserverFactory observerFactory,
			final UnconfirmedTransactionsFilter unconfirmedTransactions) {
		this.v1 = new NewBlockTransactionsProviderV1(nisCache, validatorFactory, unconfirmedTransactions);
		this.latest = new DefaultNewBlockTransactionsProvider(nisCache, validatorFactory, blockValidatorFactory, observerFactory, unconfirmedTransactions);
	}

	@Override
	public List<Transaction> getBlockTransactions(final Address harvesterAddress, final TimeInstant blockTime) {
		return this.latest.getBlockTransactions(harvesterAddress, blockTime);
	}
}
