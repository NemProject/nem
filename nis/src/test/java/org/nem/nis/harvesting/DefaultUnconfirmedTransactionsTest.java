package org.nem.nis.harvesting;

import org.nem.nis.cache.ReadOnlyNisCache;

public class DefaultUnconfirmedTransactionsTest extends UnconfirmedTransactionsMultisigTest {

	@Override
	public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory, final ReadOnlyNisCache nisCache) {
		return new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
	}
}