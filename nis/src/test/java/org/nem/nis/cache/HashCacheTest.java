package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.HashCache;

import java.util.*;
import java.util.stream.Collectors;

public class HashCacheTest {

	// region construction

	@Test
	public void hashCacheIsInitiallyEmpty() {
		// Assert:
		Assert.assertThat(new HashCache().isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void hashCacheAppliesDefaultRetentionTime() {
		// Assert:
		Assert.assertThat(new HashCache().getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCannotHaveRetentionTimeBelowMinimum() {
		// Assert:
		Assert.assertThat(new HashCache(50, 35).getRetentionTime(), IsEqual.equalTo(36));
	}

	@Test
	public void hashCacheCanHaveUnlimitedRetentionTime() {
		// Assert:
		Assert.assertThat(new HashCache(50, -1).getRetentionTime(), IsEqual.equalTo(-1));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsCorrectSize() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
	}

	// endregion

	// region isEmpty

	@Test
	public void isEmptyReturnsTrueWhenHashCacheHasZeroElements() {
		// Arrange:
		final HashCache cache = new HashCache();

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(0));
		Assert.assertThat(cache.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void isEmptyReturnsFalseWhenHashCacheHasNonZeroElements() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(cache.isEmpty(), IsEqual.equalTo(false));
	}

	// endregion

	// region clear

	@Test
	public void clearEmptiesHashCache() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);

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
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(456)));

		// Assert:
		Assert.assertThat(cache.get(hash), IsEqual.equalTo(new HashMetaData(BlockHeight.ONE, new TimeInstant(456))));
	}

	@Test
	public void getReturnsNullTimeStampWhenHashIsNotInCache() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();

		// Assert:
		Assert.assertThat(cache.get(hash), IsNull.nullValue());
	}

	// endregion

	// region put

	@Test
	public void canPutHashesWithDifferentTimeStampsToCache() {
		// Assert:
		createHashCacheWithTimeStamps(123, 234, 345);
	}

	@Test
	public void canPutHashesWithSameTimeStampsToCache() {
		// Assert:
		createHashCacheWithTimeStamps(123, 123, 123);
	}

	@Test
	public void cannotPutSameHashTwiceToCache() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final HashCache cache = new HashCache();
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
		final HashCache cache = new HashCache();

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
		final HashCache cache = new HashCache();

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.putAll(pairs), IllegalArgumentException.class);
	}

	// endregion

	// region remove

	@Test
	public void removeRemovesHashFromHashCache() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);
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
		final HashCache cache = new HashCache();
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
		final HashCache cache = new HashCache();
		cache.put(new HashMetaDataPair(hash, createMetaDataWithTimeStamp(123)));

		// Assert:
		Assert.assertThat(cache.hashExists(hash), IsEqual.equalTo(true));
	}

	@Test
	public void hashExistsReturnsFalseIfHashIsNotInCache() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 124, 124);

		// Assert:
		Assert.assertThat(cache.hashExists(Utils.generateRandomHash()), IsEqual.equalTo(false));
	}

	// endregion

	// region anyHashExists

	@Test
	public void anyHashExistsReturnsTrueIfAnyOfTheGivenHashesIsInCache() {
		// Arrange:
		final List<Hash> hashes = createHashes(10);
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345, 456, 567);
		cache.put(new HashMetaDataPair(hashes.get(7), createMetaDataWithTimeStamp(10)));

		// Assert:
		Assert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsFalseIfNoneOfTheGivenHashesIsInCache() {
		// Arrange:
		final List<Hash> hashes = createHashes(10);
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345, 456, 567);

		// Assert:
		Assert.assertThat(cache.anyHashExists(hashes), IsEqual.equalTo(false));
	}

	// endregion

	// region prune

	@Test
	public void pruneRemovesAllHashesWithEarlierTimeStampThanGivenTimeStamp() {
		// Arrange:
		final HashCache cache = new HashCache();
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final Hash hash3 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(123)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(124)));
		cache.put(new HashMetaDataPair(hash3, createMetaDataWithTimeStamp(124)));
		cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(125)));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash3), IsEqual.equalTo(false));
	}

	@Test
	public void prunePreservesAllHashesWithTimeStampAtLeastAsOldAsGivenTimeStamp() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 124, 124);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(125)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(234)));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
	}

	@Test
	public void prunePreservesAllHashesIfRetentionTimeIsUnlimited() {
		// Arrange:
		final HashCache cache = new HashCache(50, -1);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.put(new HashMetaDataPair(hash1, createMetaDataWithTimeStamp(125)));
		cache.put(new HashMetaDataPair(hash2, createMetaDataWithTimeStamp(234)));

		// Act:
		cache.prune(new TimeInstant(500));

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
		final HashCache original = new HashCache(50, 789);
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(123)));
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(234)));
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(345)));

		// Act:
		final HashCache copy = original.copy();

		// Assert:
		Assert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		copy.stream().forEach(e -> Assert.assertThat(original.get(e.getKey()), IsSame.sameInstance(e.getValue())));
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Arrange:
		final HashCache original = new HashCache(50, 789);
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(123)));
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(234)));
		original.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(345)));
		final HashCache copy = createHashCacheWithTimeStamps(321, 432, 543);

		// Act:
		original.shallowCopyTo(copy);

		// Assert:
		Assert.assertThat(copy.getRetentionTime(), IsEqual.equalTo(789));
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		copy.stream().forEach(e -> Assert.assertThat(original.get(e.getKey()), IsSame.sameInstance(e.getValue())));
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
		final HashCache cache = new HashCache();
		pairs.forEach(cache::put);

		// Assert:
		Assert.assertThat(
				cache.stream().map(e -> new HashMetaDataPair(e.getKey(), e.getValue())).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(pairs));
	}

	// endregion

	private static HashMetaData createMetaDataWithTimeStamp(final int timeStamp) {
		return new HashMetaData(BlockHeight.ONE, new TimeInstant(timeStamp));
	}

	private static HashCache createHashCacheWithTimeStamps(final int... timeStamps) {
		final HashCache cache = new HashCache();
		Arrays.stream(timeStamps)
				.forEach(t -> cache.put(new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(t))));
		return cache;
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
}
