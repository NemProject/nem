package org.nem.nis;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.HashCache;
import org.nem.nis.poi.PoiFacade;

public class NisCacheTest {

	@Test
	public void canCreateNisCache() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(Mockito.mock(AccountCache.class), Mockito.mock(PoiFacade.class));
		final HashCache transactionsHashCache = new HashCache();

		// Act:
		final NisCache cache = new NisCache(accountAnalyzer, transactionsHashCache);

		// Assert:
		Assert.assertThat(cache.getAccountAnalyzer(), IsSame.sameInstance(accountAnalyzer));
		Assert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
	}

	// region delegation

	@Test
	public void copyDelegatesToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		final NisCache cache =  new NisCache(accountAnalyzer, transactionsHashCache);

		// Act:
		cache.copy();

		// Assert:
		Mockito.verify(accountAnalyzer, Mockito.times(1)).copy();
	}

	@Test
	public void copyDelegatesToTransactionHashCache() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		final NisCache cache =  new NisCache(accountAnalyzer, transactionsHashCache);

		// Act:
		cache.copy();

		// Assert:
		Mockito.verify(transactionsHashCache, Mockito.times(1)).copy();
	}

	@Test
	public void shallowCopyToDelegatesToAccountAnalyzer() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		final NisCache cache =  new NisCache(accountAnalyzer, transactionsHashCache);
		final NisCache other =  createCache();

		// Act:
		cache.shallowCopyTo(other);

		// Assert:
		Mockito.verify(accountAnalyzer, Mockito.times(1)).shallowCopyTo(other.getAccountAnalyzer());
	}

	@Test
	public void shallowCopyToDelegatesToTransactionHashCache() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		final NisCache cache =  new NisCache(accountAnalyzer, transactionsHashCache);
		final NisCache other =  createCache();

		// Act:
		cache.shallowCopyTo(other);

		// Assert:
		Mockito.verify(transactionsHashCache, Mockito.times(1)).shallowCopyTo(other.getTransactionHashCache());
	}

	// endregion

	private NisCache createCache() {
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(Mockito.mock(AccountCache.class), Mockito.mock(PoiFacade.class));
		final HashCache transactionsHashCache = new HashCache();
		return new NisCache(accountAnalyzer, transactionsHashCache);
	}
}
