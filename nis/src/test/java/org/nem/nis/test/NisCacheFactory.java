package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.ForkConfiguration;
import org.nem.nis.cache.*;

public class NisCacheFactory {

	// region createReal

	/**
	 * Creates a real NIS cache.
	 *
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReal() {
		final ForkConfiguration forkConfiguration = new ForkConfiguration.Builder().build();
		return createReal(new DefaultPoxFacade(NisUtils.createImportanceCalculator()), forkConfiguration.getMosaicRedefinitionForkHeight());
	}

	/**
	 * Creates a real NIS cache around a pox facade.
	 *
	 * @param poxFacade The pox facade.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReal(final DefaultPoxFacade poxFacade, final BlockHeight mosaicRedefinitionForkHeight) {
		return new DefaultNisCache(new SynchronizedAccountCache(new DefaultAccountCache()),
				new SynchronizedAccountStateCache(new DefaultAccountStateCache()), new SynchronizedPoxFacade(poxFacade),
				new SynchronizedHashCache(new DefaultHashCache()),
				new SynchronizedNamespaceCache(new DefaultNamespaceCache(mosaicRedefinitionForkHeight)),
				new SynchronizedExpiredMosaicCache(new DefaultExpiredMosaicCache()));
	}

	// endregion

	// region create

	/**
	 * Creates a NIS cache around an account cache and an account state cache.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final AccountStateCache accountStateCache) {
		return create(accountCache, accountStateCache, null, null, null, null);
	}

	/**
	 * Creates a NIS cache around an account state cache.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache) {
		return create(null, accountStateCache, null, null, null, null);
	}

	/**
	 * Creates a NIS cache around an account state cache and a pox facade.
	 *
	 * @param accountStateCache The account state cache.
	 * @param poxFacade The poxFacade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache, final DefaultPoxFacade poxFacade) {
		return create(null, accountStateCache, poxFacade, null, null, null);
	}

	private static NisCache create(final AccountCache accountCache, final AccountStateCache accountStateCache,
			final DefaultPoxFacade poxFacade, final DefaultHashCache hashCache, final DefaultNamespaceCache namespaceCache,
			final DefaultExpiredMosaicCache expiredMosaicCache) {
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
			public PoxFacade getPoxFacade() {
				return null == poxFacade ? Mockito.mock(PoxFacade.class) : poxFacade;
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
			public DefaultExpiredMosaicCache getExpiredMosaicCache() {
				return null == expiredMosaicCache ? Mockito.mock(DefaultExpiredMosaicCache.class) : expiredMosaicCache;
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

	// endregion

	// region createReadOnly

	/**
	 * Creates a NIS cache around an account cache and an account state cache.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final ReadOnlyAccountStateCache accountStateCache) {
		return createReadOnly(accountCache, accountStateCache, null, null, null, null);
	}

	private static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final ReadOnlyAccountStateCache accountStateCache,
			final DefaultHashCache hashCache, final ReadOnlyPoxFacade poxFacade, final ReadOnlyNamespaceCache namespaceCache,
			final ReadOnlyExpiredMosaicCache expiredMosaicCache) {
		return new ReadOnlyNisCache() {
			@Override
			public ReadOnlyAccountCache getAccountCache() {
				return null == accountCache ? Mockito.mock(AccountCache.class) : accountCache;
			}

			@Override
			public ReadOnlyAccountStateCache getAccountStateCache() {
				return null == accountStateCache ? Mockito.mock(AccountStateCache.class) : accountStateCache;
			}

			@Override
			public ReadOnlyPoxFacade getPoxFacade() {
				return null == poxFacade ? Mockito.mock(PoxFacade.class) : poxFacade;
			}

			@Override
			public ReadOnlyHashCache getTransactionHashCache() {
				return null == hashCache ? Mockito.mock(HashCache.class) : hashCache;
			}

			@Override
			public ReadOnlyNamespaceCache getNamespaceCache() {
				return null == namespaceCache ? Mockito.mock(NamespaceCache.class) : namespaceCache;
			}

			@Override
			public ReadOnlyExpiredMosaicCache getExpiredMosaicCache() {
				return null == expiredMosaicCache ? Mockito.mock(ExpiredMosaicCache.class) : expiredMosaicCache;
			}

			@Override
			public NisCache copy() {
				throw new IllegalStateException();
			}
		};
	}

	// endregion
}
