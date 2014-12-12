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
		return new DefaultNisCache(
				new AccountCache(),
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
				Mockito.mock(SynchronizedPoiFacade.class),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around an account cache and poi facade.
	 *
	 * @param accountCache The account cache.
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final AccountCache accountCache, final DefaultPoiFacade poiFacade) {
		return new DefaultNisCache(
				accountCache,
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade.
	 *
	 * @param poiFacade The poi facade.
	 * @return The NIS cache.
	 */
	public static NisCache create(final DefaultPoiFacade poiFacade) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				Mockito.mock(HashCache.class)).copy();
	}

	/**
	 * Creates a NIS cache around a poi facade and hash cache.
	 *
	 * @param poiFacade The poi facade.
	 * @param hashCache The hash cache.
	 * @return The NIS cache.
	 */
	public static NisCache create(final DefaultPoiFacade poiFacade, final HashCache hashCache) {
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				hashCache).copy();
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
		return new DefaultNisCache(
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
		return new DefaultNisCache(
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
		return new DefaultNisCache(
				Mockito.mock(AccountCache.class),
				new SynchronizedPoiFacade(poiFacade),
				hashCache);
	}

	//endregion
}
