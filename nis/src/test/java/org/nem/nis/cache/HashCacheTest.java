package org.nem.nis.cache;

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
	protected abstract T createWritableCacheWithRetentionTime(final int retentionTime);

	/**
	 * Creates a read only cache with the specified retention time.
	 *
	 * @param retentionTime The retention time.
	 * @return The cache
	 */
	protected abstract T createReadOnlyCacheWithRetentionTime(final int retentionTime);

	//region constructor

	@Test
	public void hashCacheIsInitiallyEmpty() {
		// Assert:
		Assert.assertThat(this.createWritableCache().size(), IsEqual.equalTo(0));
	}

	@Test
	public void hashCacheAppliesDefaultRetentionTime() {
		// Assert:
		Assert.assertThat(this.createWritableCache().getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCannotHaveRetentionTimeBelowMinimum() {
		// Assert:
		Assert.assertThat(this.createWritableCacheWithRetentionTime(35).getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCanHaveUnlimitedRetentionTime() {
		// Assert:
		Assert.assertThat(this.createWritableCacheWithRetentionTime(-1).getRetentionTime(), IsEqual.equalTo(-1));
	}

	//endregion

	// endregion

	// region size

	@Test
	public void sizeReturnsCorrectSize() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsZeroWhenHashCacheHasZeroElements() {
		// Arrange:
		final HashCache cache = this.createWritableCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
	}

	@Test
	public void sizeReturnsNonZeroWhenHashCacheHasNonZeroElements() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
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
		Assert.assertThat(cache.get(hash), IsEqual.equalTo(new HashMetaData(BlockHeight.ONE, new TimeInstant(456))));
	}

	@Test
	public void getReturnsNullTimeStampWhenHashIsNotInCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();

		// Assert:
		Assert.assertThat(cache.get(hash), IsNull.nullValue());
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
		ExceptionAssert.assertThrows(v -> cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(234))), IllegalArgumentException.class);
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(10));
		for (final HashMetaDataPair pair : pairs) {
			Assert.assertThat(cache.hashExists(pair.getHash()), IsEqual.equalTo(true));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		Assert.assertThat(cache.get(pairToRemove.getHash()), IsEqual.equalTo(pairToRemove.getMetaData()));

		// Act:
		cache.remove(pairToRemove.getHash());

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(cache.get(pairToRemove.getHash()), IsNull.nullValue());
	}

	// endregion

	// region removeAll

	@Test
	public void removeAllRemovesHashesFromHashCache() {
		// Arrange:
		final List<HashMetaDataPair> pairs = createPairs(10);
		final HashCache cache = this.createWritableCache();
		cache.putAll(pairs);
		Assert.assertThat(cache.size(), IsEqual.equalTo(10));

		// Act:
		cache.removeAll(pairs.stream().limit(5).map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		pairs.subList(5, 10).stream().forEach(p -> Assert.assertThat(null != cache.get(p.getHash()), IsEqual.equalTo(true)));

		// Act:
		cache.removeAll(pairs.subList(5, 10).stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
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
		Assert.assertThat(cache.hashExists(hash), IsEqual.equalTo(true));
	}

	@Test
	public void hashExistsReturnsFalseIfHashIsNotInCache() {
		// Arrange:
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 124, 124);

		// Assert:
		Assert.assertThat(cache.hashExists(Utils.generateRandomHash()), IsEqual.equalTo(false));
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
		Assert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsFalseIfNoneOfTheGivenHashesIsInCache() {
		// Arrange:
		final List<Hash> hashes = createHashes(10);
		final HashCache cache = this.createHashCacheWithTimeStamps(123, 234, 345, 456, 567);

		// Assert:
		Assert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(false));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash3), IsEqual.equalTo(false));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
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
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllEntries() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final T original = this.createReadOnlyCacheWithRetentionTime(789);
		final T tmp = original.copy();
		tmp.putAll(pairs);
		tmp.commit();


		// Act:
		final T copy = original.copy();

		// Assert:
		Assert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		assertSameContents(copy, pairs);
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
		Assert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		assertSameContents(copy, pairs);
	}

	// endregion

	// region stream

	@Test
	public void streamIteratesThroughAllEntries() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(
				new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(123)),
				new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(234)),
				new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(345)));
		final HashCache cache = this.createWritableCache();
		pairs.forEach(cache::put);

		// Assert:
		assertEquivalentContents(cache, pairs);
	}

	// endregion

	//region utilities

	private T createHashCacheWithTimeStamps(final int... timeStamps) {
		final T cache = this.createWritableCache();
		Arrays.stream(timeStamps)
				.forEach(t -> cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(t))));
		cache.commit();
		return cache;
	}

	/**
	 * Creates a HashMetaData with the specified time stamp.
	 *
	 * @param timeStamp The time stamp.
	 * @return The hash meta data.
	 */
	protected static HashMetaData createMetaDataWithTimeStamp(final int timeStamp) {
		return new HashMetaData(BlockHeight.ONE, new TimeInstant(timeStamp));
	}

	/**
	 * Creates multiple hash metadata pairs.
	 *
	 * @param count The number of pairs.
	 * @return The pairs.
	 */
	protected static List<HashMetaDataPair> createPairs(final int count) {
		return createHashes(count).stream()
				.map(hash -> new HashMetaDataPair(hash, createMetaDataWithTimeStamp(Utils.generateRandomTimeStamp().getRawTime())))
				.collect(Collectors.toList());
	}

	/**
	 * Creates multiple hashes.
	 *
	 * @param count The number of hashes.
	 * @return The hashes.
	 */
	protected static List<Hash> createHashes(final int count) {
		final List<Hash> hashes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			hashes.add(Utils.generateRandomHash());
		}

		return hashes;
	}

	private static void assertSameContents(final ReadOnlyHashCache cache, final List<HashMetaDataPair> pairs) {
		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(pairs.size()));
		for (final HashMetaDataPair pair : pairs) {
			Assert.assertThat(cache.get(pair.getHash()), IsSame.sameInstance(pair.getMetaData()));
		}
	}

	private static void assertEquivalentContents(final ReadOnlyHashCache cache, final List<HashMetaDataPair> pairs) {
		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(pairs.size()));
		for (final HashMetaDataPair pair : pairs) {
			Assert.assertThat(cache.get(pair.getHash()), IsEqual.equalTo(pair.getMetaData()));
		}
	}

	//endregion
}
