package org.nem.nis.cache;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;

public class DefaultNisCacheTest {

	@Test
	public void canCreateNisCache() {
		// Arrange:
		final AccountCache accountCache = Mockito.mock(AccountCache.class);
		final AccountStateRepository accountStateRepository = Mockito.mock(AccountStateRepository.class);
		final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);

		// Act:
		final ReadOnlyNisCache cache = new DefaultNisCache(
				accountCache,
				accountStateRepository,
				poiFacade,
				transactionsHashCache);

		// Assert:
		Assert.assertThat(cache.getAccountCache(), IsSame.sameInstance(accountCache));
		Assert.assertThat(cache.getAccountStateCache(), IsSame.sameInstance(accountStateRepository));
		Assert.assertThat(cache.getPoiFacade(), IsSame.sameInstance(poiFacade));
		Assert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
	}

	@Test
	public void copyCreatesNewCacheByDelegatingToComponents() {
		// Arrange:
		final AccountCache copyAccountCache = Mockito.mock(AccountCache.class);
		final SynchronizedPoiFacade copyPoiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		final HashCache copyTransactionsHashCache = Mockito.mock(HashCache.class);

		final TestContext context = new TestContext();
		Mockito.when(context.accountCache.copy()).thenReturn(copyAccountCache);
		Mockito.when(context.poiFacade.copy()).thenReturn(copyPoiFacade);
		Mockito.when(context.transactionsHashCache.copy()).thenReturn(copyTransactionsHashCache);

		// Act:
		final NisCache copy = context.cache.copy();

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).copy();
		Mockito.verify(context.poiFacade, Mockito.only()).copy();
		Mockito.verify(context.transactionsHashCache, Mockito.only()).copy();

		Assert.assertThat(copy.getAccountCache(), IsSame.sameInstance(copyAccountCache));
		Assert.assertThat(copy.getAccountStateCache(), IsSame.sameInstance(copyAccountCache));
		Assert.assertThat(copy.getPoiFacade(), IsSame.sameInstance(copyPoiFacade));
		Assert.assertThat(copy.getTransactionHashCache(), IsSame.sameInstance(copyTransactionsHashCache));
	}

	@Test
	public void shallowCopyToDelegatesToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext targetContext = new TestContext();

		// Act:
		//context.cache.shallowCopyTo(targetContext.cache);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).shallowCopyTo(targetContext.accountCache);
		// TODO: Mockito.verify(context.accountStateRepository, Mockito.only()).shallowCopyTo(targetContext.accountStateRepository);
		Mockito.verify(context.poiFacade, Mockito.only()).shallowCopyTo(targetContext.poiFacade);
		Mockito.verify(context.transactionsHashCache, Mockito.only()).shallowCopyTo(targetContext.transactionsHashCache);
	}

	private class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final AccountStateRepository accountStateRepository = Mockito.mock(AccountStateRepository.class);
		private final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		private final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		private final ReadOnlyNisCache cache = new DefaultNisCache(
				this.accountCache,
				this.accountStateRepository,
				this.poiFacade,
				this.transactionsHashCache);
	}
}
