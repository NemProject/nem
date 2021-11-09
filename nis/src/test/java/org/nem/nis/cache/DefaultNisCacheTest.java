package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
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
		final SynchronizedPoxFacade poxFacade = Mockito.mock(SynchronizedPoxFacade.class);
		final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);
		final SynchronizedNamespaceCache namespaceCache = Mockito.mock(SynchronizedNamespaceCache.class);

		// Act:
		final ReadOnlyNisCache cache = new DefaultNisCache(accountCache, accountStateCache, poxFacade, transactionsHashCache,
				namespaceCache);

		// Assert:
		MatcherAssert.assertThat(cache.getAccountCache(), IsSame.sameInstance(accountCache));
		MatcherAssert.assertThat(cache.getAccountStateCache(), IsSame.sameInstance(accountStateCache));
		MatcherAssert.assertThat(cache.getPoxFacade(), IsSame.sameInstance(poxFacade));
		MatcherAssert.assertThat(cache.getTransactionHashCache(), IsSame.sameInstance(transactionsHashCache));
		MatcherAssert.assertThat(cache.getNamespaceCache(), IsSame.sameInstance(namespaceCache));
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

	private static void assertFunctionCreatesNewCacheByDelegatingToComponents(final Function<DefaultNisCache, ReadOnlyNisCache> createCopy,
			final boolean isCopyShallow) {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);

		// Act:
		final ReadOnlyNisCache copy = createCopy.apply(context.cache);

		// Assert:
		Mockito.verify(context.poxFacade, Mockito.only()).copy();

		if (isCopyShallow) {
			Mockito.verify(context.accountCache, Mockito.only()).copy();
			Mockito.verify(context.accountStateCache, Mockito.only()).copy();
			Mockito.verify(context.transactionsHashCache, Mockito.only()).copy();
			Mockito.verify(context.namespaceCache, Mockito.only()).copy();
		} else {
			Mockito.verify(context.accountCache, Mockito.only()).deepCopy();
			Mockito.verify(context.accountStateCache, Mockito.only()).deepCopy();
			Mockito.verify(context.transactionsHashCache, Mockito.only()).deepCopy();
			Mockito.verify(context.namespaceCache, Mockito.only()).deepCopy();
		}

		MatcherAssert.assertThat(copy.getPoxFacade(), IsSame.sameInstance(context2.poxFacade));
		MatcherAssert.assertThat(copy.getAccountCache(), IsSame.sameInstance(context2.accountCache));
		MatcherAssert.assertThat(copy.getAccountStateCache(), IsSame.sameInstance(context2.accountStateCache));
		MatcherAssert.assertThat(copy.getNamespaceCache(), IsSame.sameInstance(context2.namespaceCache));
		MatcherAssert.assertThat(copy.getTransactionHashCache(), IsSame.sameInstance(context2.transactionsHashCache));
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
		Mockito.verify(context2.poxFacade, Mockito.only()).shallowCopyTo(context.poxFacade);
		Mockito.verify(context2.accountCache, Mockito.only()).commit();
		Mockito.verify(context2.accountStateCache, Mockito.only()).commit();
		Mockito.verify(context2.transactionsHashCache, Mockito.only()).commit();
		Mockito.verify(context2.namespaceCache, Mockito.only()).commit();
	}

	@Test
	public void nestedCopiesAreNotAllowed() {
		// Arrange:
		final TestContext context = new TestContext();
		final TestContext context2 = new TestContext();
		setupCopy(context, context2);
		final NisCache copyCache = context.cache.copy();

		// Act:
		ExceptionAssert.assertThrows(v -> copyCache.copy(), IllegalStateException.class);
	}

	private static void setupCopy(final TestContext original, final TestContext copy) {
		Mockito.when(original.poxFacade.copy()).thenReturn(copy.poxFacade);
		Mockito.when(original.accountCache.copy()).thenReturn(copy.accountCache);
		Mockito.when(original.accountCache.deepCopy()).thenReturn(copy.accountCache);
		Mockito.when(original.accountStateCache.copy()).thenReturn(copy.accountStateCache);
		Mockito.when(original.accountStateCache.deepCopy()).thenReturn(copy.accountStateCache);
		Mockito.when(original.transactionsHashCache.copy()).thenReturn(copy.transactionsHashCache);
		Mockito.when(original.transactionsHashCache.deepCopy()).thenReturn(copy.transactionsHashCache);
		Mockito.when(original.namespaceCache.copy()).thenReturn(copy.namespaceCache);
		Mockito.when(original.namespaceCache.deepCopy()).thenReturn(copy.namespaceCache);
	}

	private static class TestContext {
		private final SynchronizedAccountCache accountCache = Mockito.mock(SynchronizedAccountCache.class);
		private final SynchronizedAccountStateCache accountStateCache = Mockito.mock(SynchronizedAccountStateCache.class);
		private final SynchronizedPoxFacade poxFacade = Mockito.mock(SynchronizedPoxFacade.class);
		private final SynchronizedHashCache transactionsHashCache = Mockito.mock(SynchronizedHashCache.class);
		private final SynchronizedNamespaceCache namespaceCache = Mockito.mock(SynchronizedNamespaceCache.class);
		private final DefaultNisCache cache = new DefaultNisCache(this.accountCache, this.accountStateCache, this.poxFacade,
				this.transactionsHashCache, this.namespaceCache);
	}
}
