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
	public static NisCache createReal() {
		return new DefaultNisCache(
				new AccountCache(),
				new DefaultAccountStateCache(),
				new SynchronizedPoiFacade(new DefaultPoiFacade(NisUtils.createImportanceCalculator())),
				new HashCache()).copy();
	}

	/**
	 * Creates a real NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache createReal(final DefaultPoiFacade poiFacade) {
		return new DefaultNisCache(
				new AccountCache(),
				new DefaultAccountStateCache(),
				new SynchronizedPoiFacade(poiFacade),
				new HashCache()).copy();
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
		return new DefaultNisCache(
				accountCache,
				Mockito.mock(AccountStateCache.class),
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final AccountStateCache accountStateCache) {
		return new DefaultNisCache(
				accountCache,
				accountStateCache,
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				accountStateCache,
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param accountStateCache The account state cache.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateCache accountStateCache, final HashCache hashCache) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				accountStateCache,
				Mockito.mock(SynchronizedPoiFacade.class),
				hashCache).copy();
	}

	//endregion

	//region createReadOnly

	/**
	* Creates a NIS cache around an account cache and poi facade.
	*
	* @param accountCache The account cache.
	* @param accountStateCache The account state cache.
	* @return The NIS cache.
	*/
	public static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final ReadOnlyAccountStateCache accountStateCache) {
		return createReadOnly(
				accountCache,
				accountStateCache,
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param accountStateCache The account state cache.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(
			final ReadOnlyAccountStateCache accountStateCache,
			final HashCache hashCache) {
		return createReadOnly(
				Mockito.mock(AccountCache.class),
				accountStateCache,
				hashCache);
	}

	private static ReadOnlyNisCache createReadOnly(
			final AccountCache accountCache,
			final ReadOnlyAccountStateCache accountStateCache,
			final HashCache hashCache) {
		return new ReadOnlyNisCache() {
			@Override
			public AccountCache getAccountCache() {
				return accountCache;
			}

			@Override
			public ReadOnlyAccountStateCache getAccountStateCache() {
				return accountStateCache;
			}

			@Override
			public ReadOnlyPoiFacade getPoiFacade() {
				return Mockito.mock(SynchronizedPoiFacade.class);
			}

			@Override
			public HashCache getTransactionHashCache() {
				return hashCache;
			}

			@Override
			public NisCache copy() {
				throw new IllegalStateException();
			}
		};
	}

	//endregion
}
