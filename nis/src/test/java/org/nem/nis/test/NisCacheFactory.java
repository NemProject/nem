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
				new DefaultAccountStateRepository(),
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
				new DefaultAccountStateRepository(),
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
				Mockito.mock(AccountStateRepository.class),
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param accountStateRepository The account state repository.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final AccountStateRepository accountStateRepository) {
		return new DefaultNisCache(
				accountCache,
				accountStateRepository,
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param accountStateRepository The account state repository.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateRepository accountStateRepository) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				accountStateRepository,
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param accountStateRepository The account state repository.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountStateRepository accountStateRepository, final HashCache hashCache) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				accountStateRepository,
				Mockito.mock(SynchronizedPoiFacade.class),
				hashCache).copy();
	}

	//endregion

	//region createReadOnly

	/**
	* Creates a NIS cache around an account cache and poi facade.
	*
	* @param accountCache The account cache.
	* @param accountStateRepository The account state repository.
	* @return The NIS cache.
	*/
	public static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final ReadOnlyAccountStateRepository accountStateRepository) {
		return createReadOnly(
				accountCache,
				accountStateRepository,
				Mockito.mock(HashCache.class));
	}

	///**
	// * Creates a NIS cache around a poi facade.
	// *
	// * @param poiFacade The poi facade.
	// * @return The NIS cache.
	// */
	//public static ReadOnlyNisCache createReadOnly(final AccountStateRepository poiFacade) {
	//	//return new DefaultNisCache(
	//	//		Mockito.mock(AccountCache.class),
	//	//		Mockito.mock(AccountStateRepository.class),
	//	//		new SynchronizedPoiFacade(poiFacade),
	//	//		Mockito.mock(HashCache.class));
	//}
	//
	///**
	// * Creates a NIS cache around a poi facade.
	// *
	// * @param accountStateRepository The account state repository.
	// * @return The NIS cache.
	// */
	//public static ReadOnlyNisCache createReadOnly(final ReadOnlyAccountStateRepository accountStateRepository) {
	//	//return new DefaultNisCache(
	//	//		Mockito.mock(AccountCache.class),
	//	//		accountStateRepository,
	//	//		Mockito.mock(SynchronizedPoiFacade.class),
	//	//		Mockito.mock(HashCache.class));
	//	return null;
	//}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param accountStateRepository The account state repository.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(
			final ReadOnlyAccountStateRepository accountStateRepository,
			final HashCache hashCache) {
		return createReadOnly(
				Mockito.mock(AccountCache.class),
				accountStateRepository,
				hashCache);
	}

	private static ReadOnlyNisCache createReadOnly(
			final AccountCache accountCache,
			final ReadOnlyAccountStateRepository accountStateRepository,
			final HashCache hashCache) {
		return new ReadOnlyNisCache() {
			@Override
			public AccountCache getAccountCache() {
				return accountCache;
			}

			@Override
			public ReadOnlyAccountStateRepository getAccountStateCache() {
				return accountStateRepository;
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
