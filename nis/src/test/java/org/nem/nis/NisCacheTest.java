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

	// region delegation

	@Test
	public void copyDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.cache.copy();

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).copy();
	}

	@Test
	public void copyDelegatesToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.cache.copy();

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(1)).copy();
	}

	@Test
	public void copyDelegatesToTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.cache.copy();

		// Assert:
		Mockito.verify(context.transactionsHashCache, Mockito.times(1)).copy();
	}

	@Test
	public void shallowCopyToDelegatesToAccountCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache other = createCache();

		// Act:
		context.cache.shallowCopyTo(other);

		// Assert:
		Mockito.verify(context.accountCache, Mockito.times(1)).shallowCopyTo(other.getAccountCache());
	}


	@Test
	public void shallowCopyToDelegatesToPoiFacade() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache other = createCache();

		// Act:
		context.cache.shallowCopyTo(other);

		// Assert:
		Mockito.verify(context.poiFacade, Mockito.times(1)).shallowCopyTo(other.getPoiFacade());
	}

	@Test
	public void shallowCopyToDelegatesToTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final NisCache other = createCache();

		// Act:
		context.cache.shallowCopyTo(other);

		// Assert:
		Mockito.verify(context.transactionsHashCache, Mockito.times(1)).shallowCopyTo(other.getTransactionHashCache());
	}

	// endregion

	private static NisCache createCache() {
		final AccountCache accountCache = Mockito.mock(AccountCache.class);
		final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		return new NisCache(accountCache, poiFacade, transactionsHashCache);
	}

	private class TestContext {
		private final AccountCache accountCache = Mockito.mock(AccountCache.class);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final HashCache transactionsHashCache = Mockito.mock(HashCache.class);
		private final NisCache cache = new NisCache(accountCache, poiFacade, transactionsHashCache);
	}
}
