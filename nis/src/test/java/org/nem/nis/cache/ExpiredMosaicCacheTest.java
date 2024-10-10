package org.nem.nis.cache;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;

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
		cache.addExpiration(new BlockHeight(122), Utils.createMosaicId(111), balances1, ExpiredMosaicType.Expired);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances2, ExpiredMosaicType.Expired);

		final MosaicBalances balances3 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances3, ExpiredMosaicType.Restored);

		final MosaicBalances balances4 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 40000);
		cache.addExpiration(new BlockHeight(124), Utils.createMosaicId(444), balances4, ExpiredMosaicType.Expired);
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
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(111), balances, ExpiredMosaicType.Expired);

		// Act:
		Collection<ExpiredMosaicEntry> expirations = cache.findExpirationsAtHeight(new BlockHeight(124));

		// Assert:
		MatcherAssert.assertThat(expirations.size(), IsEqual.equalTo(0));
	}

	private static Boolean AreMosaicBalancesEqual(final ReadOnlyMosaicBalances lhs, final ReadOnlyMosaicBalances rhs) {
		if (lhs.size() != rhs.size()) {
			return false;
		}

		for (final Address owner : lhs.getOwners()) {
			if (lhs.getBalance(owner).getRaw() != rhs.getBalance(owner).getRaw()) {
				return false;
			}
		}

		return true;
	}

	@Test
	public void findExpirationsAtHeightReturnsExpirationsWhenPresentAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(122), Utils.createMosaicId(111), balances1, ExpiredMosaicType.Expired);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances2, ExpiredMosaicType.Expired);

		final MosaicBalances balances3 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances3, ExpiredMosaicType.Restored);

		final MosaicBalances balances4 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 40000);
		cache.addExpiration(new BlockHeight(124), Utils.createMosaicId(444), balances4, ExpiredMosaicType.Expired);

		// Act: expirations are unsorted, so sort by mosaic name
		Collection<ExpiredMosaicEntry> expirations = cache.findExpirationsAtHeight(new BlockHeight(123));
		List<ExpiredMosaicEntry> expirationsList = expirations.stream()
				.sorted((e1, e2) -> e1.getMosaicId().getName().compareTo(e2.getMosaicId().getName())).collect(Collectors.toList());

		// Assert: balances should be copied, not same instance
		MatcherAssert.assertThat(expirations.size(), IsEqual.equalTo(2));

		MatcherAssert.assertThat(expirationsList.get(0).getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(222)));
		MatcherAssert.assertThat(expirationsList.get(0).getBalances(), IsNot.not(IsSame.sameInstance(balances2)));
		MatcherAssert.assertThat(AreMosaicBalancesEqual(expirationsList.get(0).getBalances(), balances2), IsEqual.equalTo(true));
		MatcherAssert.assertThat(expirationsList.get(0).getExpiredMosaicType(), IsEqual.equalTo(ExpiredMosaicType.Expired));

		MatcherAssert.assertThat(expirationsList.get(1).getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(333)));
		MatcherAssert.assertThat(expirationsList.get(1).getBalances(), IsNot.not(IsSame.sameInstance(balances3)));
		MatcherAssert.assertThat(AreMosaicBalancesEqual(expirationsList.get(1).getBalances(), balances3), IsEqual.equalTo(true));
		MatcherAssert.assertThat(expirationsList.get(1).getExpiredMosaicType(), IsEqual.equalTo(ExpiredMosaicType.Restored));
	}

	// endregion

	// region addExpiration

	@Test
	public void canAddSingleExpirationAtSingleHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		// Act:
		final MosaicBalances balances = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 10000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(111), balances, ExpiredMosaicType.Expired);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void canAddMultipleExpirationsAtSingleHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		// Act:
		final MosaicBalances balances1 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 20000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(222), balances1, ExpiredMosaicType.Expired);

		final MosaicBalances balances2 = this.createMosaicBalancesWithSingleBalance(Utils.generateRandomAddress(), 30000);
		cache.addExpiration(new BlockHeight(123), Utils.createMosaicId(333), balances2, ExpiredMosaicType.Restored);

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

	// endregion

	// region removeExpiration

	@Test
	public void canRemoveSingleExpirationAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeExpiration(new BlockHeight(123), Utils.createMosaicId(222));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(3));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));

		// - only the other (unremoved) expired mosaic entry remains
		List<ExpiredMosaicEntry> expirationsListAt123 = cache.findExpirationsAtHeight(new BlockHeight(123)).stream()
				.collect(Collectors.toList());
		MatcherAssert.assertThat(expirationsListAt123.get(0).getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(333)));
	}

	@Test
	public void canRemoveAllExpirationsIndividuallyAtHeight() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeExpiration(new BlockHeight(123), Utils.createMosaicId(222));
		cache.removeExpiration(new BlockHeight(123), Utils.createMosaicId(333));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(2));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void removeAtHeightWithoutExpirationsDoesNotChangeState() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeExpiration(new BlockHeight(121), Utils.createMosaicId(222));
		cache.removeExpiration(new BlockHeight(125), Utils.createMosaicId(222));

		// Assert: no state changes
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(4));

		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(122)).size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(123)).size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.findExpirationsAtHeight(new BlockHeight(124)).size(), IsEqual.equalTo(1));
	}

	@Test
	public void removeAtHeightWithoutMatchingMosaicExpirationDoesNotChangeState() {
		// Arrange:
		final ExpiredMosaicCache cache = this.createCache();

		this.addFourExpirations(cache);

		// Act:
		cache.removeExpiration(new BlockHeight(123), Utils.createMosaicId(111));
		cache.removeExpiration(new BlockHeight(123), Utils.createMosaicId(444));

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
		copy.removeExpiration(new BlockHeight(123), Utils.createMosaicId(222));
		copy.removeExpiration(new BlockHeight(123), Utils.createMosaicId(333));

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
		copy.removeExpiration(new BlockHeight(123), Utils.createMosaicId(222));
		copy.removeExpiration(new BlockHeight(123), Utils.createMosaicId(333));

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
		copyMutable.removeExpiration(new BlockHeight(123), Utils.createMosaicId(222));
		copyMutable.removeExpiration(new BlockHeight(123), Utils.createMosaicId(333));
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
