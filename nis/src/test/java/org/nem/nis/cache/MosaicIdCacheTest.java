package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.DbMosaicId;

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
	public void mosaicCacheIsInitiallyOnlyContainsXemMosaicIdMapping() {
		// Act:
		final MosaicIdCache cache = this.createCache();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.contains(MosaicConstants.MOSAIC_ID_XEM), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(0L)), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(7 + 1));
	}

	// endregion

	// region get - db mosaic id -> mosaic id

	@Test
	public void getReturnsExpectedMosaicIdWhenOneMosaicVersionExists() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final MosaicId mosaicId = cache.get(new DbMosaicId(3L));

		// Assert:
		MatcherAssert.assertThat(mosaicId, IsEqual.equalTo(Utils.createMosaicId(3)));
	}

	@Test
	public void getReturnsExpectedMosaicIdWhenMultipleMosaicVersionsExist() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Assert:
		Arrays.asList(3L, 13L, 14L).stream().map(DbMosaicId::new)
				.forEach(id -> MatcherAssert.assertThat(cache.get(id), IsEqual.equalTo(Utils.createMosaicId(3))));
	}

	@Test
	public void getReturnsNullWhenDbMosaicIdIsUnknown() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final MosaicId mosaicId = cache.get(new DbMosaicId(7L));

		// Assert:
		MatcherAssert.assertThat(mosaicId, IsNull.nullValue());
	}

	// endregion

	// region get - mosaic id -> db mosaic id

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
		MatcherAssert.assertThat(dbMosaicId, IsEqual.equalTo(new DbMosaicId(14L)));
	}

	@Test
	public void getReturnsNullWhenMosaicIdIsUnknown() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		final DbMosaicId dbMosaicId = cache.get(Utils.createMosaicId(7));

		// Assert:
		MatcherAssert.assertThat(dbMosaicId, IsNull.nullValue());
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicIdExistsInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		IntStream.range(1, 6).forEach(i -> MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsTrueIfDbMosaicIdExistsInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 3);
		cache.add(Utils.createMosaicId(3), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(3), new DbMosaicId(14L));

		// Assert:
		Arrays.asList(1L, 2L, 3L, 13L, 14L)
				.forEach(i -> MatcherAssert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicIdDoesNotExistInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		IntStream.range(6, 10).forEach(i -> MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(false)));
	}

	@Test
	public void containsReturnsFalseIfDbMosaicIdDoesNotExistInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Assert:
		LongStream.range(6, 10).forEach(i -> MatcherAssert.assertThat(cache.contains(new DbMosaicId(i)), IsEqual.equalTo(false)));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	@Test
	public void canAddIdMappingToCacheIfMosaicIdIsAlreadyInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Act:
		cache.add(Utils.createMosaicId(12), new DbMosaicId(14L));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(2 + 1));
		MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(14L)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	@Test
	public void cannotAddIdMappingToCacheIfDbMosaicIdIsAlreadyInCache() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		cache.add(Utils.createMosaicId(12), new DbMosaicId(13L));

		// Act:
		ExceptionAssert.assertThrows(v -> cache.add(Utils.createMosaicId(14), new DbMosaicId(13L)), IllegalArgumentException.class);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1 + 1));
		MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(12)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(14)), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(13L)), IsEqual.equalTo(true));
	}

	// endregion

	// region remove - mosaic id

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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(3 + 1));
		IntStream.range(1, 6).forEach(i -> {
			final boolean isExpectedInCache = i % 2 == 1;
			MatcherAssert.assertThat(cache.contains(new DbMosaicId((long) i)), IsEqual.equalTo(isExpectedInCache));
			MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(isExpectedInCache));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(5 + 1));
		IntStream.range(1, 6).forEach(i -> {
			MatcherAssert.assertThat(cache.contains(new DbMosaicId((long) i)), IsEqual.equalTo(true));
			MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true));
		});
	}

	// endregion

	// region remove - db mosaic id

	@Test
	public void removeRemovesIdMappingFromCacheForGivenDbMosaicIdIfOnlyOneMosaicVersionExists() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);

		// Act:
		cache.remove(new DbMosaicId(2L));
		cache.remove(new DbMosaicId(4L));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(3 + 1));
		IntStream.range(1, 6).forEach(i -> {
			final boolean isExpectedInCache = i % 2 == 1;
			MatcherAssert.assertThat(cache.contains(new DbMosaicId((long) i)), IsEqual.equalTo(isExpectedInCache));
			MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(isExpectedInCache));
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
		MatcherAssert.assertThat(cache.get(Utils.createMosaicId(2)), IsEqual.equalTo(new DbMosaicId(14L)));

		// Act:
		cache.remove(new DbMosaicId(14L));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(6 + 1));
		MatcherAssert.assertThat(cache.get(Utils.createMosaicId(2)), IsEqual.equalTo(new DbMosaicId(13L)));
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
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(5 + 1));
		IntStream.range(1, 6).forEach(i -> {
			MatcherAssert.assertThat(cache.contains(new DbMosaicId((long) i)), IsEqual.equalTo(true));
			MatcherAssert.assertThat(cache.contains(Utils.createMosaicId(i)), IsEqual.equalTo(true));
		});
	}

	// endregion

	// region clear

	@Test
	public void clearEmptiesCacheAndAddXemMosaicIdMapping() {
		// Arrange:
		final MosaicIdCache cache = this.createCache();
		addToCache(cache, 5);
		cache.add(Utils.createMosaicId(2), new DbMosaicId(13L));
		cache.add(Utils.createMosaicId(2), new DbMosaicId(14L));

		// sanity check
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5 + 1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(7 + 1));

		// Act:
		cache.clear();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.deepSize(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.contains(MosaicConstants.MOSAIC_ID_XEM), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.contains(new DbMosaicId(0L)), IsEqual.equalTo(true));
	}

	// endregion

	private static void addToCache(final MosaicIdCache cache, final int count) {
		IntStream.range(0, count).forEach(i -> cache.add(Utils.createMosaicId(i + 1), new DbMosaicId((long) i + 1)));
	}
}
