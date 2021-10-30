package org.nem.nis.harvesting;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.nis.cache.ReadOnlyNisCache;

@RunWith(Enclosed.class)
public class DefaultUnconfirmedTransactionsTest {

	public static class DefaultUnconfirmedTransactionsMultisigTest extends UnconfirmedTransactionsMultisigTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return DefaultUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class DefaultUnconfirmedTransactionsFilterTest extends UnconfirmedTransactionsFilterTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return DefaultUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class DefaultUnconfirmedTransactionsStateDelegationTest extends UnconfirmedTransactionsStateDelegationTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return DefaultUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class DefaultUnconfirmedTransactionsOtherTest extends UnconfirmedTransactionsOtherTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return DefaultUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
			final ReadOnlyNisCache nisCache) {
		return new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
	}
}
