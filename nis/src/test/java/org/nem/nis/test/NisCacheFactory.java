package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.nis.cache.*;
import org.nem.nis.test.NisUtils;

public class NisCacheFactory {

	//region createReal

	/**
	 * Creates a real NIS cache.
	 *
	 * @return The NIS cache.
	 */
	public static NisCache createReal() {
		return new NisCache(
				new AccountCache(),
				new SynchronizedPoiFacade(new DefaultPoiFacade(NisUtils.createImportanceCalculator())),
				new HashCache());
	}

	/**
	 * Creates a real NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache createReal(final DefaultPoiFacade poiFacade) {
		return new NisCache(
				new AccountCache(),
				new SynchronizedPoiFacade(poiFacade),
				new HashCache());
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
		return new NisCache(
				accountCache,
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final DefaultPoiFacade poiFacade) {
		return new NisCache(
				accountCache,
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final DefaultPoiFacade poiFacade) {
		return new NisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param poiFacade The poi facade.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final DefaultPoiFacade poiFacade, final HashCache hashCache) {
		return new NisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				hashCache);
	}

	//endregion

	//region createReadOnly

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(final AccountCache accountCache, final DefaultPoiFacade poiFacade) {
		return new ReadOnlyNisCache(
				accountCache,
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(final DefaultPoiFacade poiFacade) {
		return new ReadOnlyNisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class));
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param poiFacade The poi facade.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static ReadOnlyNisCache createReadOnly(final DefaultPoiFacade poiFacade, final HashCache hashCache) {
		return new ReadOnlyNisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				hashCache);
	}

	//endregion
}
