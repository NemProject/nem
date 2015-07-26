package org.nem.nis.cache;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.test.ExceptionAssert;

import java.util.function.Function;

public class DefaultNisCacheTest {

	@Test
	public void canCreateNisCache() {
		// Arrange:
		final SynchronizedAccountCache accountCache = Mockito.mock(SynchronizedAccountCache.class);
		final SynchronizedAccountStateCache accountStateCache = Mockito.mock(SynchronizedAccountStateCache.class);
		final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);
		final SynchronizedNamespaceCache namespaceCache = Mockito.mock(SynchronizedNamespaceCache.class);

		// Act:
		final ReadOnlyNisCache cache = new DefaultNisCache(
				accountCache,
				accountStateCache,
				poiFacade,
				transactionsHashCache,
				namespaceCache);

		// Assert:
		Assert.assertThat(cache.getAccountCache(), IsSame.sameInstance(accountCache));
		Assert.assertThat(cache.getAccountStateCache(), IsSame.sameInstance(accountStateCache));
		Assert.assertThat(cache.getPoiFacade(), IsSame.sameInstance(poiFacade));
		Assert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
		Assert.assertThat(cache.getNamespaceCache(), IsSame.sameInstance(namespaceCache));
	}

	@Test
	public void copyCreatesNewCacheByDelegatingToComponents() {
		// Assert:
		assertFunctionCreatesNewCacheByDelegatingToComponents(DefaultNisCache::copy, true);
	}

	@Test
	public void deepCopyCreatesNewCacheByDelegatingToComponents() {
		// Assert:
		assertFunctionCreatesNewCacheByDelegatingToComponents(DefaultNisCache::deepCopy, false);
	}

	private static void assertFunctionCreatesNewCacheByDelegatingToComponents(
			final Function<DefaultNisCache, ReadOnlyNisCache> createCopy,
			final boolean isCopyAutoCached) {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);

		// Act:
		final ReadOnlyNisCache copy = createCopy.apply(context.cache);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.only()).copy();
		Mockito.verify(context.accountStateCache, Mockito.only()).copy();
		Mockito.verify(context.poiFacade, Mockito.only()).copy();
		Mockito.verify(context.transactionsHashCache, Mockito.only()).copy();
		Mockito.verify(context.namespaceCache, Mockito.only()).copy();

		if (isCopyAutoCached) {
			Assert.assertThat(copy.getAccountCache(), IsSame.sameInstance(context2.accountAutoCache));
			Assert.assertThat(copy.getAccountStateCache(), IsSame.sameInstance(context2.accountStateAutoCache));
		} else {
			Assert.assertThat(copy.getAccountCache(), IsSame.sameInstance(context2.accountCache));
			Assert.assertThat(copy.getAccountStateCache(), IsSame.sameInstance(context2.accountStateCache));
		}

		Assert.assertThat(copy.getPoiFacade(), IsSame.sameInstance(context2.poiFacade));
		Assert.assertThat(copy.getTransactionHashCache(), IsSame.sameInstance(context2.transactionsHashCache));
		Assert.assertThat(copy.getNamespaceCache(), IsSame.sameInstance(context2.namespaceCache));
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
		Mockito.verify(context2.namespaceCache, Mockito.only()).shallowCopyTo(context.namespaceCache);
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
		Mockito.when(original.namespaceCache.copy()).thenReturn(copy.namespaceCache);

		Mockito.when(copy.accountCache.asAutoCache()).thenReturn(copy.accountAutoCache);
		Mockito.when(copy.accountStateCache.asAutoCache()).thenReturn(copy.accountStateAutoCache);
	}

	private static class TestContext {
		private final SynchronizedAccountCache accountCache = Mockito.mock(SynchronizedAccountCache.class);
		private final AccountCache accountAutoCache = Mockito.mock(AccountCache.class);
		private final SynchronizedAccountStateCache accountStateCache = Mockito.mock(SynchronizedAccountStateCache.class);
		private final AccountStateCache accountStateAutoCache = Mockito.mock(AccountStateCache.class);
		private final SynchronizedPoiFacade poiFacade = Mockito.mock(SynchronizedPoiFacade.class);
		private final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);
		private final SynchronizedNamespaceCache namespaceCache = Mockito.mock(SynchronizedNamespaceCache.class);
		private final DefaultNisCache cache = new DefaultNisCache(
				this.accountCache,
				this.accountStateCache,
				this.poiFacade,
				this.transactionsHashCache,
				this.namespaceCache);
	}
}
