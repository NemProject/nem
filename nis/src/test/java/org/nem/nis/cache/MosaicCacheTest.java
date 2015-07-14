package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.*;
import org.nem.core.test.*;

import java.util.function.Function;
import java.util.stream.IntStream;

public abstract class MosaicCacheTest<T extends CopyableCache<T> & MosaicCache> {

	/**
	 * Creates a cache.
	 *
	 * @return The cache
	 */
	protected abstract T createCache();

	// region constructor

	@Test
	public void mosaicCacheIsInitiallyOnlyContainsXemMosaic() {
		// Act:
		final MosaicCache cache = this.createCache();
		final Mosaic xemMosaic = cache.get(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem"));
		final MosaicProperties properties = xemMosaic.getProperties();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(xemMosaic.getCreator(), IsEqual.equalTo(NamespaceConstants.LESSOR));
		Assert.assertThat(xemMosaic.getId(), IsEqual.equalTo(new MosaicId(NamespaceConstants.NAMESPACE_ID_NEM, "xem")));
		Assert.assertThat(xemMosaic.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("reserved xem mosaic")));
		Assert.assertThat(xemMosaic.toString(), IsEqual.equalTo("nem * xem"));
		Assert.assertThat(properties.getQuantity(), IsEqual.equalTo(8999999999000000L));
		Assert.assertThat(properties.getDivisibility(), IsEqual.equalTo(6));
		Assert.assertThat(properties.isQuantityMutable(), IsEqual.equalTo(false));
		Assert.assertThat(properties.isTransferable(), IsEqual.equalTo(true));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedMosaic() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		final Mosaic original = Utils.createMosaic("vouchers", "gift vouchers");
		cache.add(original);

		// Act:
		final Mosaic mosaic = cache.get(new MosaicId(new NamespaceId("vouchers"), "gift vouchers"));

		// Assert:
		Assert.assertThat(mosaic, IsEqual.equalTo(original));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicExistsInCache() {
		// Arrange:
		final String[] namespaceIds = { "id1", "id1", "id2", "id3", };
		final String[] names = { "name1", "name2", "name1", "name3", };
		final MosaicCache cache = this.createCache();
		addToCache(cache, namespaceIds, names);

		// Assert:
		IntStream.range(0, namespaceIds.length)
				.forEach(i -> Assert.assertThat(
						cache.contains(new MosaicId(new NamespaceId(namespaceIds[i]), names[i])),
						IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicDoesNotExistInCache() {
		// Arrange:
		final String[] namespaceIds = { "id1", "id1", "id2", "id3", };
		final String[] names = { "name1", "name2", "name1", "name3", };
		final MosaicCache cache = this.createCache();
		addToCache(cache, namespaceIds, names);

		// Assert:
		Assert.assertThat(cache.contains(new MosaicId(new NamespaceId("id1"), "name3")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new MosaicId(new NamespaceId("id2"), "name2")), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new MosaicId(new NamespaceId("id3"), "name1")), IsEqual.equalTo(false));
	}

	// endregion

	// region add/remove

	@Test
	public void canAddDifferentMosaicsToCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();

		// Act:
		addToCache(cache, 3);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 3));
		IntStream.range(0, 3).forEach(i -> Assert.assertThat(cache.contains(Utils.createMosaicId(i + 1)),	IsEqual.equalTo(true)));
	}

	@Test
	public void cannotAddSameMosaicTwiceToCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(Utils.createMosaic(2)), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveExistingMosaicFromCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(Utils.createMosaic(2));
		cache.remove(Utils.createMosaic(4));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1 + 3));
		Assert.assertThat(cache.contains(Utils.createMosaicId(1)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(Utils.createMosaicId(2)), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(Utils.createMosaicId(3)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(Utils.createMosaicId(4)), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(Utils.createMosaicId(5)), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNonExistingMosaicFromCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(Utils.createMosaic(7)), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemovePreviouslyExistingNonExistingMosaicFromCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 3);
		cache.remove(Utils.createMosaic(2));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(Utils.createMosaic(2)), IllegalArgumentException.class);
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
		Assert.assertThat(copy.size(), IsEqual.equalTo(1 + 4));
		IntStream.range(0, 4).forEach(i -> Assert.assertThat(copy.contains(Utils.createMosaicId(i + 1)), IsEqual.equalTo(true)));

		// Act: remove a mosaic
		cache.remove(Utils.createMosaic(3));

		// Assert: the mosaic should always be removed from the original but not removed from the copy
		Assert.assertThat(cache.contains(Utils.createMosaicId(3)), IsEqual.equalTo(false));
		Assert.assertThat(copy.contains(Utils.createMosaicId(3)), IsEqual.equalTo(true));
	}

	// endregion

	private static void addToCache(final MosaicCache cache, final String[] namespaceIds, final String[] names) {
		IntStream.range(0, namespaceIds.length)
				.forEach(i -> cache.add(Utils.createMosaic(namespaceIds[i], names[i])));
	}

	private static void addToCache(final MosaicCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.add(Utils.createMosaic(i + 1)));
	}
}
