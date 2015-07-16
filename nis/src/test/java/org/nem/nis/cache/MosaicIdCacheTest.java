package org.nem.nis.cache;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.DbMosaicId;

import java.util.stream.*;

public abstract class MosaicIdCacheTest<T extends MosaicIdCache> {

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
		final MosaicIdCache cache = this.createCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedMosaicId() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final MosaicId mosaicId = cache.get(new DbMosaicId(3L));

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(Utils.createMosaicId(3)));
	}

	@Test
	public void getReturnsExpectedDbMosaicId() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final DbMosaicId dbMosaicId = cache.get(Utils.createMosaicId(3));

		// Assert:
		Assert.assertThat(dbMosaicId, IsEqual.equalTo(new DbMosaicId(3L)));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicIdExistsInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		IntStream.range(1, 6).forEach(i -> Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsTrueIfDbMosaicIdExistsInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		LongStream.range(1, 6).forEach(i -> Assert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicIdDoesNotExistInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		IntStream.range(6, 10).forEach(i -> Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(false)));
	}

	@Test
	public void containsReturnsFalseIfDbMosaicIdDoesNotExistInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		LongStream.range(6, 10).forEach(i -> Assert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(false)));
	}

	// endregion

	// region add/remove

	@Test
	public void canAddAMosaicIdDbMosaicIdMappingToCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();

		// Act:
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	// TODO 20150715: i would break the remove into to tests; one for each overload
	@Test
	public void canRemoveAMosaicIdDbMosaicIdMappingFromCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(Utils.createMosaicId(2));
		cache.remove(new DbMosaicId(4L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		IntStream.range(1, 6).forEach(i -> Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(i % 2 == 1)));
		LongStream.range(1, 6).forEach(i -> Assert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(i % 2 == 1)));
	}

	// endregion

	private static void addToCache(final MosaicIdCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.add(Utils.createMosaicId(i + 1), new DbMosaicId((long)i + 1)));
	}
}
