package org.nem.nis.test.BlockChain;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.mockito.Mockito;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyAccountState;
import org.nem.nis.test.MockBlockDao;

public class BlockChainUtils {

	public static void assertNisCacheCalls(
			final ReadOnlyNisCache nisCache,
			final int getAccountCacheCalls,
			final int getAccountStateCacheCalls,
			final int getPoxFacadeCalls,
			final int copyCalls) {
		Mockito.verify(nisCache, Mockito.times(getAccountCacheCalls)).getAccountCache();
		Mockito.verify(nisCache, Mockito.times(getAccountStateCacheCalls)).getAccountStateCache();
		Mockito.verify(nisCache, Mockito.times(getPoxFacadeCalls)).getPoxFacade();
		Mockito.verify(nisCache, Mockito.times(copyCalls)).copy();
	}

	public static void assertNisCachesAreEquivalent(final ReadOnlyNisCache lhs, final ReadOnlyNisCache rhs) {
		// we are only interested in the accounts and balances of the accounts
		final ReadOnlyAccountStateCache lhsCache = lhs.getAccountStateCache();
		final ReadOnlyAccountStateCache rhsCache = rhs.getAccountStateCache();

		Assert.assertThat(lhsCache.size(), IsEqual.equalTo(rhsCache.size()));
		for (final ReadOnlyAccountState accountState : lhsCache.contents()) {
			Assert.assertThat(lhs.getAccountCache().isKnownAddress(accountState.getAddress()), IsEqual.equalTo(true));
			Assert.assertThat(rhsCache.findStateByAddress(accountState.getAddress()).getAccountInfo().getBalance(),
					IsEqual.equalTo(accountState.getAccountInfo().getBalance()));
		}
	}

	public static void assertMockBlockDaosAreEquivalent(final MockBlockDao lhs, final MockBlockDao rhs) {
		Assert.assertThat(lhs.equals(rhs), IsEqual.equalTo(true));
	}
}
