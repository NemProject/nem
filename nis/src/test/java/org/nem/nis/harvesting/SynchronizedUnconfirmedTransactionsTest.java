package org.nem.nis.harvesting;

import org.nem.nis.cache.ReadOnlyNisCache;

public class SynchronizedUnconfirmedTransactionsTest extends UnconfirmedTransactionsMultisigTest {

	@Override
	public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory, final ReadOnlyNisCache nisCache) {
		return new SynchronizedUnconfirmedTransactions(new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache));
	}
}