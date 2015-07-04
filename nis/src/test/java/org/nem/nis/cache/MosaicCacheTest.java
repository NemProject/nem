package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
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
	public void mosaicCacheIsInitiallyEmpty() {
		// Act:
		final MosaicCache cache = this.createCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		IntStream.range(0, 3).forEach(i -> Assert.assertThat(cache.contains(createMosaicId(i + 1)),	IsEqual.equalTo(true)));
	}

	@Test
	public void cannotAddSameMosaicTwiceToCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(createMosaic(2)), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveExistingMosaicFromCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(createMosaic(2));
		cache.remove(createMosaic(4));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(cache.contains(createMosaicId(1)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(createMosaicId(2)), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(createMosaicId(3)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(createMosaicId(4)), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(createMosaicId(5)), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNonExistingMosaicFromCache() {
		// Arrange:
		final MosaicCache cache = this.createCache();
		addToCache(cache, 3);

		// Act:
		cache.remove(createMosaic(2));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.remove(createMosaic(2)), IllegalArgumentException.class);
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		IntStream.range(0, 3).forEach(i -> Assert.assertThat(cache.contains(createMosaicId(i + 1)), IsEqual.equalTo(true)));

		// Act: remove a mosaic
		cache.remove(createMosaic(3));

		// Assert: the mosaic should always be removed from the original but not removed from the copy
		Assert.assertThat(cache.contains(createMosaicId(3)), IsEqual.equalTo(false));
		Assert.assertThat(copy.contains(createMosaicId(3)), IsEqual.equalTo(true));
	}

	// endregion

	private static void addToCache(final MosaicCache cache, final String[] namespaceIds, final String[] names) {
		IntStream.range(0, namespaceIds.length)
				.forEach(i -> cache.add(Utils.createMosaic(namespaceIds[i], names[i])));
	}

	private static void addToCache(final MosaicCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.add(createMosaic(i + 1)));
	}

	private static Mosaic createMosaic(final int i) {
		return Utils.createMosaic(String.format("id%d", i), String.format("name%d", i));
	}

	private static MosaicId createMosaicId(final int i) {
		return new MosaicId(new NamespaceId(String.format("id%d", i)), String.format("name%d", i));
	}
}
