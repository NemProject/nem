package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class SmartTileSupplyCacheTest<T extends CopyableCache<T> & SmartTileSupplyCache> {

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected abstract T createCache();

	// region constructor

	@Test
	public void mosaicCacheIsInitiallyEmpty() {
		// Act:
		final SmartTileSupplyCache cache = this.createCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region getCurrentSupply

	@Test
	public void getCurrentSupplyReturnsExpectedSupply() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();
		cache.increaseSupply(createSmartTile(12, 123));
		cache.increaseSupply(createSmartTile(12, 234));

		final Quantity quantity = cache.getCurrentSupply(Utils.createMosaicId(12));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(357)));
	}

	@Test
	public void getCurrentSupplyReturnsQuantityZeroIfSmartTileTypeIsUnknown() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();
		cache.increaseSupply(createSmartTile(12, 123));
		cache.increaseSupply(createSmartTile(12, 234));

		final Quantity quantity = cache.getCurrentSupply(Utils.createMosaicId(15));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.ZERO));
	}

	// endregion

	// region increaseSupply

	@Test
	public void canIncreaseSupplyIfSmartTileTypeIsUnknown() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();

		// Act:
		final Quantity quantity = cache.increaseSupply(createSmartTile(12, 123));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(123)));
		Assert.assertThat(cache.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void canIncreaseSupplyIfSmartTileTypeIsKnown() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();
		cache.increaseSupply(createSmartTile(12, 123));

		// Act:
		final Quantity quantity = cache.increaseSupply(createSmartTile(12, 234));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(357)));
		Assert.assertThat(cache.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(357)));
	}

	// endregion

	// region decreaseSupply

	@Test
	public void canDecreaseSupplyIfCurrentSupplyIsLargeEnough() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();
		cache.increaseSupply(createSmartTile(12, 234));

		// Act:
		final Quantity quantity = cache.decreaseSupply(createSmartTile(12, 123));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(111)));
		Assert.assertThat(cache.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(111)));
	}

	@Test
	public void cannotDecreaseSupplyIfCurrentSupplyIsTooSmall() {
		// Arrange:
		final SmartTileSupplyCache cache = this.createCache();
		cache.increaseSupply(createSmartTile(12, 123));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.decreaseSupply(createSmartTile(12, 124)), NegativeQuantityException.class);
	}

	// endregion

	// region shallowCopyTo / copy

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Assert:
		this.assertCopy(cache -> {
			final T copy = this.createCache();
			cache.shallowCopyTo(copy);
			return copy;
		});
	}

	@Test
	public void copyCopiesAllEntries() {
		// Assert:
		this.assertCopy(CopyableCache::copy);
	}

	private void assertCopy(final Function<T, T> copyCache) {
		// Arrange:
		final T cache = this.createCache();
		addToCache(cache, 4);

		// Act:
		final T copy = copyCache.apply(cache);

		// Assert: initial copy
		Assert.assertThat(copy.size(), IsEqual.equalTo(4));
		IntStream.range(0, 4).forEach(i -> Assert.assertThat(
				copy.getCurrentSupply(Utils.createMosaicId(i + 1)),
				IsEqual.equalTo(Quantity.fromValue(i + 1))));

		// Act: change supply
		cache.increaseSupply(createSmartTile(3, 123));

		// Assert: the quantity should have changed the original but not in the copy
		Assert.assertThat(cache.getCurrentSupply(Utils.createMosaicId(3)), IsEqual.equalTo(Quantity.fromValue(126)));
		Assert.assertThat(copy.getCurrentSupply(Utils.createMosaicId(3)), IsEqual.equalTo(Quantity.fromValue(3)));
	}

	// endregion

	private static void addToCache(final SmartTileSupplyCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.increaseSupply(new SmartTile(Utils.createMosaicId(i + 1), Quantity.fromValue(i + 1))));
	}

	private static SmartTile createSmartTile(final int id, final long quantity) {
		return new SmartTile(Utils.createMosaicId(id), Quantity.fromValue(quantity));
	}
}
