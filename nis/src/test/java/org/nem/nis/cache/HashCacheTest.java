package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

public abstract class HashCacheTest<T extends CopyableCache<T> & HashCache & CommittableCache> {
	private static final int RETENTION_TIME = 36 * 60 * 60;

	/**
	 * Creates a cache to which data can be added.
	 *
	 * @return The cache
	 */
	protected abstract T createWritableCache();

	/**
	 * Creates a cache with the specified retention time and to which data can be added.
	 *
	 * @param retentionTime The retention time.
	 * @return The cache
	 */
	protected T createWritableCacheWithRetentionTime(final int retentionTime) {
		return this.createReadOnlyCacheWithRetentionTime(retentionTime).copy();
	}

	/**
	 * Creates a read only cache with the specified retention time.
	 *
	 * @param retentionTime The retention time.
	 * @return The cache
	 */
	protected abstract T createReadOnlyCacheWithRetentionTime(final int retentionTime);

	// region constructor

	@Test
	public void hashCacheIsInitiallyEmpty() {
		// Assert:
		MatcherAssert.assertThat(this.createWritableCache().size(), IsEqual.equalTo(0));
	}

	@Test
	public void hashCacheAppliesDefaultRetentionTime() {
		// Assert:
		MatcherAssert.assertThat(this.createWritableCache().getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCannotHaveRetentionTimeBelowMinimum() {
		// Assert:
		MatcherAssert.assertThat(this.createWritableCacheWithRetentionTime(35).getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCanHaveUnlimitedRetentionTime() {
		// Assert:
		MatcherAssert.assertThat(this.createWritableCacheWithRetentionTime(-1).getRetentionTime(), IsEqual.equalTo(-1));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsCorrectSize() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsZeroWhenHashCacheHasZeroElements() {
		// Arrange:
		final HashCache cache = this.createWritableCache();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void sizeReturnsNonZeroWhenHashCacheHasNonZeroElements() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(3));
	}

	// endregion

	// region clear

	@Test
	public void clearEmptiesHashCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);

		// Act:
		cache.clear();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region get

	@Test
	public void getReturnsCorrectTimeStampWhenHashIsInCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(456)));

		// Assert:
		MatcherAssert.assertThat(cache.get(hash), IsEqual.equalTo(new HashMetaData(BlockHeight.ONE, new TimeInstant(456))));
	}

	@Test
	public void getReturnsNullTimeStampWhenHashIsNotInCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();

		// Assert:
		MatcherAssert.assertThat(cache.get(hash), IsNull.nullValue());
	}

	// endregion

	// region put

	@Test
	public void canPutHashesWithDifferentTimeStampsToCache() {
		// Assert:
		this.createHashCacheWithTimeStamps(123, 234, 345);
	}

	@Test
	public void canPutHashesWithSameTimeStampsToCache() {
		// Assert:
		this.createHashCacheWithTimeStamps(123, 123, 123);
	}

	@Test
	public void cannotPutSameHashTwiceToCache() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final HashCache cache = this.createWritableCache();
		cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(123)));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(234))),
				IllegalArgumentException.class);
	}

	// endregion

	// region putAll

	@Test
	public void canPutAllHashesFromListToCache() {
		// Arrange:
		final List<HashMetaDataPair> pairs = createPairs(10);
		final HashCache cache = this.createWritableCache();

		// Act:
		cache.putAll(pairs);

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(10));
		for (final HashMetaDataPair pair : pairs) {
			MatcherAssert.assertThat(cache.hashExists(pair.getHash()), IsEqual.equalTo(true));
		}
	}

	@Test
	public void cannotPutAllWhenAtLeastOneHashIsKnown() {
		// Arrange:
		final List<HashMetaDataPair> pairs = createPairs(10);
		pairs.add(new HashMetaDataPair(pairs.get(6).getHash(), createMetaDataWithTimeStamp(789)));
		final HashCache cache = this.createWritableCache();

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.putAll(pairs), IllegalArgumentException.class);
	}

	// endregion

	// region remove

	@Test
	public void removeRemovesHashFromHashCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);
		final HashMetaDataPair pairToRemove = new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(456));
		cache.put(pairToRemove);
		cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(567)));
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(cache.get(pairToRemove.getHash()), IsEqual.equalTo(pairToRemove.getMetaData()));

		// Act:
		cache.remove(pairToRemove.getHash());

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(cache.get(pairToRemove.getHash()), IsNull.nullValue());
	}

	// endregion

	// region removeAll

	@Test
	public void removeAllRemovesHashesFromHashCache() {
		// Arrange:
		final List<HashMetaDataPair> pairs = createPairs(10);
		final HashCache cache = this.createWritableCache();
		cache.putAll(pairs);
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(10));

		// Act:
		cache.removeAll(pairs.stream().limit(5).map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(5));
		pairs.subList(5, 10).stream().forEach(p -> MatcherAssert.assertThat(null != cache.get(p.getHash()), IsEqual.equalTo(true)));

		// Act:
		cache.removeAll(pairs.subList(5, 10).stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region hashExists

	@Test
	public void hashExistsReturnsTrueIfHashIsInCache() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final HashCache cache = this.createWritableCache();
		cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(123)));

		// Assert:
		MatcherAssert.assertThat(cache.hashExists(hash), IsEqual.equalTo(true));
	}

	@Test
	public void hashExistsReturnsFalseIfHashIsNotInCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 124, 124);

		// Assert:
		MatcherAssert.assertThat(cache.hashExists(Utils.generateRandomHash()), IsEqual.equalTo(false));
	}

	// endregion

	// region anyHashExists

	@Test
	public void anyHashExistsReturnsTrueIfAnyOfTheGivenHashesIsInCache() {
		// Arrange:
		final List<Hash> hashes = createHashes(10);
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345, 456, 567);
		cache.put(new HashMetaDataPair(hashes.get(7), createMetaDataWithTimeStamp(10)));

		// Assert:
		MatcherAssert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsFalseIfNoneOfTheGivenHashesIsInCache() {
		// Arrange:
		final List<Hash> hashes = createHashes(10);
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345, 456, 567);

		// Assert:
		MatcherAssert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(false));
	}

	// endregion

	// region prune

	@Test
	public void pruneRemovesAllHashesWithEarlierTimeStampThanGivenTimeStampMinusRetentionTime() {
		// Arrange:
		final HashCache cache = this.createWritableCache();
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final Hash hash3 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(123)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(124)));
		cache.put(new HashMetaDataPair(hash3, createMetaDataWithTimeStamp(124)));
		cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(125)));

		// Act:
		cache.prune(new TimeInstant(RETENTION_TIME + 125));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(false));
		MatcherAssert.assertThat(cache.hashExists(hash3), IsEqual.equalTo(false));
	}

	@Test
	public void prunePreservesAllHashesWithTimeStampAtLeastAsOldAsGivenTimeStampMinusRetentionTime() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 124, 124);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(125)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(234)));

		// Act:
		cache.prune(new TimeInstant(RETENTION_TIME + 125));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
	}

	@Test
	public void prunePreservesAllHashesIfRetentionTimeIsUnlimited() {
		// Arrange:
		final HashCache cache = this.createWritableCacheWithRetentionTime(-1);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(125)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(234)));

		// Act:
		cache.prune(new TimeInstant(RETENTION_TIME + 500));

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		MatcherAssert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllEntries() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final T original = this.createCacheWithValues(789, pairs);

		// Act:
		final T copy = original.copy();

		// Assert:
		MatcherAssert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		assertSameContents(copy, pairs);
	}

	private T createCacheWithValues(final int retentionTime, final List<HashMetaDataPair> pairs) {
		final T original = this.createReadOnlyCacheWithRetentionTime(retentionTime);
		final T copy = original.copy();
		copy.putAll(pairs);
		copy.commit();
		return original;
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final T original = this.createWritableCacheWithRetentionTime(789);
		original.putAll(pairs);

		final T copy = this.createHashCacheWithTimeStamps(321, 432, 543);

		// Act:
		original.shallowCopyTo(copy);

		// Assert:
		MatcherAssert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		assertSameContents(copy, pairs);
	}

	// endregion

	// region commit

	@Test
	public void commitAddsAllNewEntriesToHashMap() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345, 456).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final T cache = this.createReadOnlyCacheWithRetentionTime(123);
		final T copy = cache.copy();
		copy.putAll(pairs);

		// sanity check
		pairs.forEach(p -> MatcherAssert.assertThat(cache.hashExists(p.getHash()), IsEqual.equalTo(false)));

		// Act:
		copy.commit();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(4));
		pairs.forEach(p -> MatcherAssert.assertThat(cache.hashExists(p.getHash()), IsEqual.equalTo(true)));
	}

	@Test
	public void commitRemovesAllDeletedEntriesFromHashMap() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345, 456).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final T cache = this.createReadOnlyCacheWithRetentionTime(123);
		final T copy = cache.copy();
		copy.putAll(pairs);
		copy.commit();
		copy.removeAll(pairs.stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// sanity check
		pairs.forEach(p -> MatcherAssert.assertThat(cache.hashExists(p.getHash()), IsEqual.equalTo(true)));

		// Act:
		copy.commit();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(0));
		pairs.forEach(p -> MatcherAssert.assertThat(cache.hashExists(p.getHash()), IsEqual.equalTo(false)));
	}

	// endregion

	// rollback related

	@Test
	public void removingAndAddingSameEntryWithCommitUpdatesHashMetaData() {
		// Arrange: create hash cache with some entries
		final DefaultHashCache cache = new DefaultHashCache(100, -1);
		for (int i = 0; i < 5; ++i) {
			cache.put(new HashMetaDataPair(Utils.generateRandomHash(), new HashMetaData(new BlockHeight(i + 1), new TimeInstant(2 * i))));
		}

		// - add another entry
		final Hash hash = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash, new HashMetaData(new BlockHeight(456), new TimeInstant(126))));

		cache.commit();

		// Sanity:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(6));

		// Act:
		cache.remove(hash);
		cache.put(new HashMetaDataPair(hash, new HashMetaData(new BlockHeight(678), new TimeInstant(579))));
		cache.commit();

		// Assert: size is unchanged
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(6));

		// - meta data should be updated
		HashMetaData metadata = cache.get(hash);
		MatcherAssert.assertThat(metadata.getHeight(), IsEqual.equalTo(new BlockHeight(678)));
		MatcherAssert.assertThat(metadata.getTimeStamp(), IsEqual.equalTo(new TimeInstant(579)));
	}

	// endregion

	// region utilities

	private T createHashCacheWithTimeStamps(final int... timeStamps) {
		final T cache = this.createWritableCache();
		Arrays.stream(timeStamps).forEach(t -> cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(t))));
		cache.commit();
		return cache;
	}

	protected static HashMetaData createMetaDataWithTimeStamp(final int timeStamp) {
		return new HashMetaData(BlockHeight.ONE, new TimeInstant(timeStamp));
	}

	private static List<HashMetaDataPair> createPairs(final int count) {
		return createHashes(count).stream()
				.map(hash -> new HashMetaDataPair(hash, createMetaDataWithTimeStamp(Utils.generateRandomTimeStamp().getRawTime())))
				.collect(Collectors.toList());
	}

	private static List<Hash> createHashes(final int count) {
		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			hashes.add(Utils.generateRandomHash());
		}

		return hashes;
	}

	private static void assertSameContents(final ReadOnlyHashCache cache, final List<HashMetaDataPair> pairs) {
		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(pairs.size()));
		for (final HashMetaDataPair pair : pairs) {
			MatcherAssert.assertThat(cache.get(pair.getHash()), IsSame.sameInstance(pair.getMetaData()));
		}
	}

	// endregion
}
