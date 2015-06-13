package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.nis.cache.*;

public class NisCacheFactory {

	//region createReal

	/**
	 * Creates a real NIS cache.
	 *
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReal() {
		return createReal(new DefaultPoiFacade(NisUtils.createImportanceCalculator()));
	}

	/**
	 * Creates a real NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReal(final DefaultPoiFacade poiFacade) {
		return new DefaultNisCache(
				new SynchronizedAccountCache(new DefaultAccountCache()),
				new SynchronizedAccountStateCache(new DefaultAccountStateCache()),
				new SynchronizedPoiFacade(poiFacade),
				new SynchronizedHashCache(new DefaultHashCache()),
				new SynchronizedNamespaceCache(new DefaultNamespaceCache()));
	}

	//endregion

	//region create

	/**
	 * Creates a NIS cache around an account.
	 *
	 * @param accountCache The account cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache) {
		return create(accountCache, null, null, null, null);
	}

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final AccountStateCache accountStateCache) {
		return create(accountCache, accountStateCache, null, null, null);
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache) {
		return create(null, accountStateCache, null, null, null);
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param accountStateCache The account state cache.
	 * @param poiFacade The poiFacade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache, final DefaultPoiFacade poiFacade) {
		return create(null, accountStateCache, poiFacade, null, null);
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param accountStateCache The account state cache.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache, final DefaultHashCache hashCache) {
		return create(null, accountStateCache, null, hashCache, null);
	}

	private static NisCache create(
			final AccountCache accountCache,
			final AccountStateCache accountStateCache,
			final DefaultPoiFacade poiFacade,
			final DefaultHashCache hashCache,
			final DefaultNamespaceCache namespaceCache) {
		return new NisCache() {
			@Override
			public AccountCache getAccountCache() {
				return null == accountCache ? Mockito.mock(AccountCache.class) : accountCache;
			}

			@Override
			public AccountStateCache getAccountStateCache() {
				return null == accountStateCache ? Mockito.mock(AccountStateCache.class) : accountStateCache;
			}

			@Override
			public PoiFacade getPoiFacade() {
				return null == poiFacade ? Mockito.mock(SynchronizedPoiFacade.class) : new SynchronizedPoiFacade(poiFacade);
			}

			@Override
			public DefaultHashCache getTransactionHashCache() {
				return null == hashCache ? Mockito.mock(DefaultHashCache.class) : hashCache;
			}

			@Override
			public DefaultNamespaceCache getNamespaceCache() {
				return null == namespaceCache ? Mockito.mock(DefaultNamespaceCache.class) : namespaceCache;
			}

			@Override
			public void commit() {
			}

			@Override
			public NisCache copy() {
				throw new IllegalStateException();
			}
		};
	}

	//endregion

	//region createReadOnly

	/**
	 * Creates a NIS cache around an account cache and an account state cache.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final ReadOnlyAccountStateCache accountStateCache) {
		return createReadOnly(accountCache, accountStateCache, null, null, null);
	}

	/**
	 * Creates a NIS cache around an account state cache and hash cache.
	 *
	 * @param accountStateCache The account state cache.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(
			final ReadOnlyAccountStateCache accountStateCache,
			final DefaultHashCache hashCache) {
		return createReadOnly(null, accountStateCache, hashCache, null, null);
	}

	/**
	 * Creates a NIS cache around an account state cache, a hash cache and a poi facade.
	 *
	 * @param accountStateCache The account state cache.
	 * @param hashCache The hash cache.
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(
			final ReadOnlyAccountStateCache accountStateCache,
			final DefaultHashCache hashCache,
			final ReadOnlyPoiFacade poiFacade) {
		return createReadOnly(null, accountStateCache, hashCache, poiFacade, null);
	}

	private static ReadOnlyNisCache createReadOnly(
			final AccountCache accountCache,
			final ReadOnlyAccountStateCache accountStateCache,
			final DefaultHashCache hashCache,
			final ReadOnlyPoiFacade poiFacade,
			final ReadOnlyNamespaceCache namespaceCache) {
		return new ReadOnlyNisCache() {
			@Override
			public AccountCache getAccountCache() {
				return null == accountCache ? Mockito.mock(AccountCache.class) : accountCache;
			}

			@Override
			public ReadOnlyAccountStateCache getAccountStateCache() {
				return null == accountStateCache ? Mockito.mock(AccountStateCache.class) : accountStateCache;
			}

			@Override
			public ReadOnlyPoiFacade getPoiFacade() {
				return null == poiFacade ? Mockito.mock(PoiFacade.class) : poiFacade;
			}

			@Override
			public DefaultHashCache getTransactionHashCache() {
				return null == hashCache ? Mockito.mock(DefaultHashCache.class) : hashCache;
			}

			@Override
			public ReadOnlyNamespaceCache getNamespaceCache() {
				return null == namespaceCache ? Mockito.mock(NamespaceCache.class) : namespaceCache;
			}

			@Override
			public NisCache copy() {
				throw new IllegalStateException();
			}
		};
	}

	//endregion
}
