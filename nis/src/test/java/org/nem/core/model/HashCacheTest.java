package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.stream.Collectors;

public class HashCacheTest {

	// region construction

	@Test
	public void hashCacheIsInitiallyEmpty() {
		// Assert:
		Assert.assertThat(new HashCache().size(), IsEqual.equalTo(0));
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
	public void getReturnsCorrectTimeStamp() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 234, 345);
		final Hash hash = Utils.generateRandomHash();
		cache.put(new HashTimeInstantPair(hash, new TimeInstant(456)));

		// Assert:
		Assert.assertThat(cache.get(hash), IsEqual.equalTo(new TimeInstant(456)));
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
		cache.put(new HashTimeInstantPair(hash, new TimeInstant(123)));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.put(new HashTimeInstantPair(hash, new TimeInstant(234))), IllegalArgumentException.class);
	}

	// endregion

	// region putAll

	@Test
	public void canPutAllHashesFromListToCache() {
		// Arrange:
		final List<HashTimeInstantPair> pairs = createPairs(10);
		final HashCache cache = new HashCache();

		// Assert:
		cache.putAll(pairs);
	}

	@Test
	public void cannotPutAllWhenAtLeastOneHashIsKnown() {
		// Arrange:
		final List<HashTimeInstantPair> pairs = createPairs(10);
		pairs.add(new HashTimeInstantPair(pairs.get(6).getHash(), new TimeInstant(789)));
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
		final Hash hash = Utils.generateRandomHash();
		cache.put(new HashTimeInstantPair(hash, new TimeInstant(456)));
		Assert.assertThat(cache.size(), IsEqual.equalTo(4));
		Assert.assertThat(cache.get(hash), IsEqual.equalTo(new TimeInstant(456)));

		// Act:
		cache.remove(hash);

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(3));
		Assert.assertThat(null == cache.get(hash), IsEqual.equalTo(true));
	}

	// endregion

	// region removeAll

	@Test
	public void removeAllRemovesHashesFromHashCache() {
		// Arrange:
		final List<HashTimeInstantPair> pairs = createPairs(10);
		final HashCache cache = new HashCache();
		cache.putAll(pairs);
		Assert.assertThat(cache.size(), IsEqual.equalTo(10));

		// Act:
		cache.removeAll(pairs.stream().limit(5).map(HashTimeInstantPair::getHash).collect(Collectors.toList()));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(5));
		pairs.subList(5,10).stream().forEach(p -> Assert.assertThat(null != cache.get(p.getHash()), IsEqual.equalTo(true)));

		// Act:
		cache.removeAll(pairs.subList(5,10).stream().map(HashTimeInstantPair::getHash).collect(Collectors.toList()));

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
		cache.put(new HashTimeInstantPair(hash, new TimeInstant(123)));

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
		cache.put(new HashTimeInstantPair(hashes.get(7), new TimeInstant(10)));

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
		cache.put(new HashTimeInstantPair(hash1, new TimeInstant(123)));
		cache.put(new HashTimeInstantPair(hash2, new TimeInstant(124)));
		cache.put(new HashTimeInstantPair(hash3, new TimeInstant(124)));
		cache.put(new HashTimeInstantPair(Utils.generateRandomHash(), new TimeInstant(125)));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(false));
		Assert.assertThat(cache.hashExists(hash3), IsEqual.equalTo(false));
	}


	@Test
	public void prunePreservesAllHashesWithEarlierTimeStampThanGivenTimeStamp() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 124, 124);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.put(new HashTimeInstantPair(hash1, new TimeInstant(125)));
		cache.put(new HashTimeInstantPair(hash2, new TimeInstant(234)));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.hashExists(hash1), IsEqual.equalTo(true));
		Assert.assertThat(cache.hashExists(hash2), IsEqual.equalTo(true));
	}

	// endregion

	// region shallowCopy

	@Test
	public void shallowCopyCopiesAllEntries() {
		// Arrange:
		final HashCache original = createHashCacheWithTimeStamps(123, 234, 345);

		// Act:
		final HashCache copy = original.shallowCopy();

		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		copy.stream().forEach(e -> Assert.assertThat(original.get(e.getKey()), IsEqual.equalTo(e.getValue())));
	}

	// endregion

	// region shallowCopyTo

	@Test
	public void shallowCopyToCopiesAllEntries() {
		// Arrange:
		final HashCache original = createHashCacheWithTimeStamps(123, 234, 345);
		final HashCache copy = createHashCacheWithTimeStamps(321, 432, 543);

		// Act:
		original.shallowCopyTo(copy);

		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(original.size()));
		copy.stream().forEach(e -> Assert.assertThat(original.get(e.getKey()), IsEqual.equalTo(e.getValue())));
	}

	// endregion

	// region stream

	@Test
	public void streamIteratesThroughAllEntries() {
		// Arrange:
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final Hash hash3 = Utils.generateRandomHash();
		final HashCache cache = new HashCache();
		cache.put(new HashTimeInstantPair(hash1, new TimeInstant(123)));
		cache.put(new HashTimeInstantPair(hash2, new TimeInstant(234)));
		cache.put(new HashTimeInstantPair(hash3, new TimeInstant(345)));

		// Assert:
		Assert.assertThat(cache.stream().count(), IsEqual.equalTo(3L));
		Assert.assertThat(cache.stream().anyMatch(e -> e.getKey().equals(hash1) && e.getValue().equals(new TimeInstant(123))), IsEqual.equalTo(true));
		Assert.assertThat(cache.stream().anyMatch(e -> e.getKey().equals(hash2) && e.getValue().equals(new TimeInstant(234))), IsEqual.equalTo(true));
		Assert.assertThat(cache.stream().anyMatch(e -> e.getKey().equals(hash3) && e.getValue().equals(new TimeInstant(345))), IsEqual.equalTo(true));
	}

	// endregion

	private HashCache createHashCacheWithTimeStamps(final int... timeStamps) {
		final HashCache cache = new HashCache();
		Arrays.stream(timeStamps).forEach(t -> cache.put(new HashTimeInstantPair(Utils.generateRandomHash(), new TimeInstant(t))));
		return cache;
	}

	private List<HashTimeInstantPair> createPairs(final int count) {
		final List<HashTimeInstantPair> pairs = new ArrayList<>();
		for (int i=0; i<count; i++) {
			pairs.add(new HashTimeInstantPair(Utils.generateRandomHash(), Utils.generateRandomTimeStamp()));
		}

		return pairs;
	}

	private List<Hash> createHashes(final int count) {
		final List<Hash> hashes = new ArrayList<>();
		for (int i=0; i<count; i++) {
			hashes.add(Utils.generateRandomHash());
		}

		return hashes;
	}
}
