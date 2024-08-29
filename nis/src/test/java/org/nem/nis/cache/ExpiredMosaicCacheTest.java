package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.ForkConfiguration;
import org.nem.nis.NemNamespaceEntry;
import org.nem.nis.state.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public abstract class ExpiredMosaicCacheTest<T extends ExpiredMosaicCache & DeepCopyableCache<T> & CommittableCache> {
	/**
	 * Creates a cache.
	 *
	 * @return The cache.
	 */
	protected T createCache() {
		return this.createImmutableCache().copy();
	}

	/**
	 * Creates a cache that has auto-caching disabled.
	 *
	 * @return The cache.
	 */
	protected abstract T createImmutableCache();

	// region test utils

	private MosaicBalances createMosaicBalancesWithSingleBalance(final Address address, final long balance) {
		final MosaicBalances balances = new MosaicBalances();
		balances.incrementBalance(address, new Quantity(balance));
		return balances;
	}

	private void addFourExpirations(ExpiredMosaicCache cache) {
		final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(122), Utils.createMosaicId(111), balances1);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances2);

		final MosaicBalances balances3 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances3);

		final MosaicBalances balances4 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 40000);
		cache.addExpiration(new BlockHeight(124), Utils.createMosaicId(444), balances4);
	}

	// endregion

	// region constructor

	@Test
	public void cacheInitiallyIsEmpty() {
		// Act:
		final ExpiredMosaicCache cache = this.createCache();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(0));
	}

	// endregion

	// region findExpirationsAtHeight

	@Test
	public void findExpirationsAtHeightReturnsEmptyCollectionWhenNoExpirationsAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		final MosaicBalances balances = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(111), balances);

		// Act:
		Collection<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> expirations = cache.findExpirationsAtHeight(new BlockHeight(124));

		// Assert:
		MatcherAssert.assertThat(expirations.size(), IsEqual.equalTo(0));
	}

	@Test
	public void findExpirationsAtHeightReturnsExpirationsWhenPresentAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(122), Utils.createMosaicId(111), balances1);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances2);

		final MosaicBalances balances3 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances3);

		final MosaicBalances balances4 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 40000);
		cache.addExpiration(new BlockHeight(124), Utils.createMosaicId(444), balances4);

		// Act: expirations are unsorted, so sort by mosaic name
		Collection<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> expirations = cache.findExpirationsAtHeight(new BlockHeight(123));
		List<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> expirationsList = expirations
			.stream()
			.sorted((e1, e2) -> e1.getKey().getName().compareTo(e2.getKey().getName()))
			.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(expirations.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(expirationsList.get(0).getKey(), IsEqual.equalTo(Utils.createMosaicId(222)));
		MatcherAssert.assertThat(expirationsList.get(0).getValue(), IsSame.sameInstance(balances2));
		MatcherAssert.assertThat(expirationsList.get(1).getKey(), IsEqual.equalTo(Utils.createMosaicId(333)));
		MatcherAssert.assertThat(expirationsList.get(1).getValue(), IsSame.sameInstance(balances3));
	}

	// endregion

	// region addExpiration / removeAll

	@Test
	public void canAddSingleExpirationAtSingleHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		final MosaicBalances balances = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(111), balances);

		// Act:
		Collection<Map.Entry<MosaicId, ReadOnlyMosaicBalances>> expirations = cache.findExpirationsAtHeight(new BlockHeight(123));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddMultipleExpirationsAtSingleHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances1);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances2);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(2));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
	}

	@Test
	public void canAddMultipleExpirationsAtMultipleHeights() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		// Act:
		this.addFourExpirations(cache);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(4));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void canRemoveAllExpirationsAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeAll(new BlockHeight(123));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(2));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void removeAllAtHeightWithoutExpirationsDoesNotChangeState() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeAll(new BlockHeight(121));
		cache.removeAll(new BlockHeight(125));

		// Assert: no state changes
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(4));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	// endregion

	// region shallowCopyTo / copy / deepCopy

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Assert:
		this.assertBasicCopy(cache -> {
			final T copy = this.createCache();
			cache.shallowCopyTo(copy);
			return copy;
		});
	}

	@Test
	public void shallowCopyRemoveAllIsUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = this.createCache();
		cache.shallowCopyTo(copy);

		// Act: remove expirations
		copy.removeAll(new BlockHeight(123));

		// Assert: the expirations should be removed from only the copy but not the original
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(0));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void copyCopiesAllEntries() {
		this.assertBasicCopy(CopyableCache::copy);
	}

	@Test
	public void copyRemoveAllIsUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = cache.copy();

		// Act: remove expirations
		copy.removeAll(new BlockHeight(123));

		// Assert: the expirations should be removed from only the copy but not the original
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(0));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void deepCopyCopiesAllEntries() {
		this.assertBasicCopy(DeepCopyableCache::deepCopy);
	}

	@Test
	public void deepCopyRemoveAllIsUnlinked() {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act: remove expirations
		final T copy = cache.deepCopy();
		final T copyMutable = copy.copy();
		copyMutable.removeAll(new BlockHeight(123));
		copyMutable.commit();

		// Assert: the expirations should be removed from only the copy but not the original
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(0));
	}

	private T createCacheForCopyTests() {
		final T cache = this.createImmutableCache();
		final T copy = cache.copy();

		this.addFourExpirations(copy);

		copy.commit();
		return cache;
	}

	private void assertBasicCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCacheForCopyTests();

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert: initial copy
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(copy.deepSize(), IsEqual.equalTo(4));

		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(copy.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	// endregion
}
