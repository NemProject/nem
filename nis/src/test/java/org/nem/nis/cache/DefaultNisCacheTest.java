package org.nem.nis.cache;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;

public class DefaultNisCacheTest {

	@Test
	public void canCreateNisCache() {
		// Arrange:
		final AccountCache accountCache = Mockito.mock(AccountCache.class);
		final SynchronizedAccountStateCache accountStateCache = Mockito.mock(SynchronizedAccountStateCache.class);
		final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);

		// Act:
		final ReadOnlyNisCache cache = new DefaultNisCache(
				accountCache,
				accountStateCache,
				poiFacade,
				transactionsHashCache);

		// Assert:
		Assert.assertThat(cache.getAccountCache(), IsSame.sameInstance(accountCache));
		Assert.assertThat(cache.getAccountStateCache(), IsSame.sameInstance(accountStateCache));
		Assert.assertThat(cache.getPoiFacade(), IsSame.sameInstance(poiFacade));
		Assert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
	}

	@Test
	public void copyCreatesNewCacheByDelegatingToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);

		// Act:
		final NisCache copy = context.cache.copy();

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).copy();
		Mockito.verify(context.accountStateCache, Mockito.only()).copy();
		Mockito.verify(context.poiFacade, Mockito.only()).copy();
		Mockito.verify(context.transactionsHashCache, Mockito.only()).copy();

		Assert.assertThat(copy.getAccountCache(), IsSame.sameInstance(context2.accountCache));
		Assert.assertThat(copy.getAccountStateCache(), IsSame.sameInstance(context2.accountStateCache));
		Assert.assertThat(copy.getPoiFacade(), IsSame.sameInstance(context2.poiFacade));
		Assert.assertThat(copy.getTransactionHashCache(), IsSame.sameInstance(context2.transactionsHashCache));
	}

	@Test
	public void commitDelegatesToComponents() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);

		// Act:
		context.cache.copy().commit();

		// Assert:
		Mockito.verify(context2.accountCache, Mockito.only()).shallowCopyTo(context.accountCache);
		Mockito.verify(context2.accountStateCache, Mockito.only()).shallowCopyTo(context.accountStateCache);
		Mockito.verify(context2.poiFacade, Mockito.only()).shallowCopyTo(context.poiFacade);
		Mockito.verify(context2.transactionsHashCache, Mockito.only()).shallowCopyTo(context.transactionsHashCache);
	}

	@Test
	public void nestedCopiesAreNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);
		final NisCache copyCache = context.cache.copy();

		// Act:
		ExceptionAssert.assertThrows(
				v -> copyCache.copy(),
				IllegalStateException.class);
	}

	private static void setupCopy(final TestContext original, final TestContext copy) {
		Mockito.when(original.accountCache.copy()).thenReturn(copy.accountCache);
		Mockito.when(original.accountStateCache.copy()).thenReturn(copy.accountStateCache);
		Mockito.when(original.poiFacade.copy()).thenReturn(copy.poiFacade);
		Mockito.when(original.transactionsHashCache.copy()).thenReturn(copy.transactionsHashCache);
	}

	private class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final SynchronizedAccountStateCache accountStateCache = Mockito.mock(SynchronizedAccountStateCache.class);
		private final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		private final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);
		private final ReadOnlyNisCache cache = new DefaultNisCache(
				this.accountCache,
				this.accountStateCache,
				this.poiFacade,
				this.transactionsHashCache);
	}
}
