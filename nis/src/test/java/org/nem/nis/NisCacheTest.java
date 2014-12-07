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
		final AccountCache accountCache = Mockito.mock(AccountCache.class);
		final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);

		// Act:
		final NisCache cache = new NisCache(accountCache, poiFacade, transactionsHashCache);

		// Assert:
		Assert.assertThat(cache.getAccountCache(), IsSame.sameInstance(accountCache));
		Assert.assertThat(cache.getPoiFacade(), IsSame.sameInstance(poiFacade));
		Assert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
	}

	@Test
	public void copyCreatesNewCacheByDelegatingToComponents() {
		// Arrange:
		final AccountCache copyAccountCache = Mockito.mock(AccountCache.class);
		final PoiFacade copyPoiFacade = Mockito.mock(PoiFacade.class);
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
		Assert.assertThat(copy.getPoiFacade(), IsSame.sameInstance(copyPoiFacade));
		Assert.assertThat(copy.getTransactionHashCache(), IsSame.sameInstance(copyTransactionsHashCache));
	}

	@Test
	public void shallowCopyToDelegatesToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext targetContext = new TestContext();

		// Act:
		context.cache.shallowCopyTo(targetContext.cache);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).shallowCopyTo(targetContext.accountCache);
		Mockito.verify(context.poiFacade, Mockito.only()).shallowCopyTo(targetContext.poiFacade);
		Mockito.verify(context.transactionsHashCache, Mockito.only()).shallowCopyTo(targetContext.transactionsHashCache);
	}

	private class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		private final NisCache cache = new NisCache(this.accountCache, this.poiFacade, this.transactionsHashCache);
	}
}
