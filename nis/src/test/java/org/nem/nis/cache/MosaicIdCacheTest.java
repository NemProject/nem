package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.Arrays;
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
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(0));
	}

	// endregion

	// region deepSize

	@Test
	public void deepSizeRespectsMosaicIdVersions() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(7));
	}

	// endregion

	// region get

	// region db mosaic id -> mosaic id

	@Test
	public void getReturnsExpectedMosaicIdWhenOneMosaicVersionExists() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final MosaicId mosaicId = cache.get(new DbMosaicId(3L));

		// Assert:
		Assert.assertThat(mosaicId, IsEqual.equalTo(Utils.createMosaicId(3)));
	}

	@Test
	public void getReturnsExpectedMosaicIdWhenMultipleMosaicVersionsExist() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Assert:
		Arrays.asList(3L, 13L, 14L).stream()
				.map(DbMosaicId::new)
				.forEach(id -> Assert.assertThat(cache.get(id), IsEqual.equalTo(Utils.createMosaicId(3))));
	}

	@Test
	public void getReturnsNullWhenDbMosaicIdIsUnknown() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final MosaicId mosaicId = cache.get(new DbMosaicId(7L));

		// Assert:
		Assert.assertThat(mosaicId, IsNull.nullValue());
	}

	// endregion

	// region mosaic id -> db mosaic id

	@Test
	public void getReturnsLastDbMosaicIdInList() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Act:
		final DbMosaicId dbMosaicId = cache.get(Utils.createMosaicId(3));

		// Assert:
		Assert.assertThat(dbMosaicId, IsEqual.equalTo(new DbMosaicId(14L)));
	}

	@Test
	public void getReturnsNullWhenMosaicIdIsUnknown() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final DbMosaicId dbMosaicId = cache.get(Utils.createMosaicId(7));

		// Assert:
		Assert.assertThat(dbMosaicId, IsNull.nullValue());
	}

	// endregion

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
		addToCache(cache, 3);
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Assert:
		Arrays.asList(1L, 2L, 3L, 13L, 14L).forEach(i -> Assert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(true)));
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

	// region add

	@Test
	public void canAddIdMappingToCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();

		// Act:
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	@Test
	public void canAddIdMappingToCacheIfMosaicIdIsAlreadyInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Act:
		cache.add(Utils.createMosaicId(12), new DbMosaicId(14L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(2));
		Assert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new DbMosaicId(14L)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddIdMappingToCacheIfDbMosaicIdIsAlreadyInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Act:
		ExceptionAssert.assertThrows(
				v -> cache.add(Utils.createMosaicId(14), new DbMosaicId(13L)),
				IllegalArgumentException.class);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
		Assert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		Assert.assertThat(cache.contains(Utils.createMosaicId(14)), IsEqual.equalTo(false));
		Assert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	//endregion

	// region remove

	// region mosaic id

	@Test
	public void canRemoveIdMappingFromCacheForGivenMosaicId() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(2), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(2), new DbMosaicId(14L));

		// Act:
		cache.remove(Utils.createMosaicId(2));
		cache.remove(Utils.createMosaicId(4));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(3));
		IntStream.range(1, 6).forEach(i -> {
			final boolean isExpectedInCache = i % 2 == 1;
			Assert.assertThat(cache.contains(new DbMosaicId((long)i)), IsEqual.equalTo(isExpectedInCache));
			Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(isExpectedInCache));
		});
	}

	@Test
	public void removeDoesNothingWhenRemovingANonExistentMosaicId() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(Utils.createMosaicId(7));
		cache.remove(Utils.createMosaicId(11));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(5));
		IntStream.range(1, 6).forEach(i -> {
			Assert.assertThat(cache.contains(new DbMosaicId((long)i)), IsEqual.equalTo(true));
			Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true));
		});
	}

	// endregion

	// region db mosaic id

	@Test
	public void removeRemovesIdMappingFromCacheForGivenDbMosaicIdIfOnlyOneMosaicVersionExists() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(new DbMosaicId(2L));
		cache.remove(new DbMosaicId(4L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(3));
		IntStream.range(1, 6).forEach(i -> {
			final boolean isExpectedInCache = i % 2 == 1;
			Assert.assertThat(cache.contains(new DbMosaicId((long)i)), IsEqual.equalTo(isExpectedInCache));
			Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(isExpectedInCache));
		});
	}

	@Test
	public void removeUpdatesIdMappingInCacheForGivenDbMosaicIdIfMultipleMosaicVersionsExist() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(2), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(2), new DbMosaicId(14L));

		// sanity check
		Assert.assertThat(cache.get(Utils.createMosaicId(2)), IsEqual.equalTo(new DbMosaicId(14L)));

		// Act:
		cache.remove(new DbMosaicId(14L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(6));
		Assert.assertThat(cache.get(Utils.createMosaicId(2)), IsEqual.equalTo(new DbMosaicId(13L)));
	}

	@Test
	public void removeDoesNothingWhenRemovingANonExistentDbMosaicId() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(new DbMosaicId(7L));
		cache.remove(new DbMosaicId(11L));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(5));
		IntStream.range(1, 6).forEach(i -> {
			Assert.assertThat(cache.contains(new DbMosaicId((long)i)), IsEqual.equalTo(true));
			Assert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true));
		});
	}

	// endregion

	// endregion

	// region clear

	@Test
	public void clearEmptiesCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(2), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(2), new DbMosaicId(14L));

		// sanity check
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(7));

		// Act:
		cache.clear();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.deepSize(), IsEqual.equalTo(0));
	}

	// endregion

	private static void addToCache(final MosaicIdCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.add(Utils.createMosaicId(i + 1), new DbMosaicId((long)i + 1)));
	}
}
