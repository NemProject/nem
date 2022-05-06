package org.nem.nis.harvesting;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.nem.nis.cache.ReadOnlyNisCache;

@RunWith(Enclosed.class)
public class SynchronizedUnconfirmedTransactionsTest {

	public static class SynchronizedUnconfirmedTransactionsMultisigTest extends UnconfirmedTransactionsMultisigTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return SynchronizedUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class SynchronizedUnconfirmedTransactionsFilterTest extends UnconfirmedTransactionsFilterTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return SynchronizedUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class SynchronizedUnconfirmedTransactionsStateDelegationTest extends UnconfirmedTransactionsStateDelegationTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return SynchronizedUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	public static class SynchronizedUnconfirmedTransactionsOtherTest extends UnconfirmedTransactionsOtherTest {
		@Override
		public UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
				final ReadOnlyNisCache nisCache) {
			return SynchronizedUnconfirmedTransactionsTest.createUnconfirmedTransactions(unconfirmedStateFactory, nisCache);
		}
	}

	private static UnconfirmedTransactions createUnconfirmedTransactions(final UnconfirmedStateFactory unconfirmedStateFactory,
			final ReadOnlyNisCache nisCache) {
		return new SynchronizedUnconfirmedTransactions(new DefaultUnconfirmedTransactions(unconfirmedStateFactory, nisCache));
	}
}
