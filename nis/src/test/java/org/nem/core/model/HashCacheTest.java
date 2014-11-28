package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.Arrays;

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

	// region add

	@Test
	public void canAddHashesWithDifferentTimeStampsToCache() {
		// Assert:
		createHashCacheWithTimeStamps(123, 234, 345);
	}

	@Test
	public void canAddHashesWithSameTimeStampsToCache() {
		// Assert:
		createHashCacheWithTimeStamps(123, 123, 123);
	}

	@Test
	public void cannotAddSameHashTwiceToCache() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final HashCache cache = new HashCache();
		cache.add(hash, new TimeInstant(123));

		// Assert:
		ExceptionAssert.assertThrows(v -> cache.add(hash, new TimeInstant(234)), IllegalArgumentException.class);
	}

	// endregion

	// region isKnown

	@Test
	public void isKnownReturnsTrueIfHashIsInCache() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final HashCache cache = new HashCache();
		cache.add(hash, new TimeInstant(123));

		// Assert:
		Assert.assertThat(cache.isKnown(hash), IsEqual.equalTo(true));
	}

	@Test
	public void isKnownReturnsFalseIfHashIsNotInCache() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 124, 124);

		// Assert:
		Assert.assertThat(cache.isKnown(Utils.generateRandomHash()), IsEqual.equalTo(false));
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
		cache.add(hash1, new TimeInstant(123));
		cache.add(hash2, new TimeInstant(124));
		cache.add(hash3, new TimeInstant(124));
		cache.add(Utils.generateRandomHash(), new TimeInstant(125));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(1));
		Assert.assertThat(cache.isKnown(hash1), IsEqual.equalTo(false));
		Assert.assertThat(cache.isKnown(hash2), IsEqual.equalTo(false));
		Assert.assertThat(cache.isKnown(hash3), IsEqual.equalTo(false));
	}


	@Test
	public void prunePreservesAllHashesWithEarlierTimeStampThanGivenTimeStamp() {
		// Arrange:
		final HashCache cache = createHashCacheWithTimeStamps(123, 124, 124);
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		cache.add(hash1, new TimeInstant(125));
		cache.add(hash2, new TimeInstant(234));

		// Act:
		cache.prune(new TimeInstant(125));

		// Assert:
		Assert.assertThat(cache.size(), IsEqual.equalTo(2));
		Assert.assertThat(cache.isKnown(hash1), IsEqual.equalTo(true));
		Assert.assertThat(cache.isKnown(hash2), IsEqual.equalTo(true));
	}

	// endregion

	private HashCache createHashCacheWithTimeStamps(final int... timeStamps) {
		final HashCache cache = new HashCache();
		Arrays.stream(timeStamps).forEach(t -> cache.add(Utils.generateRandomHash(), new TimeInstant(t)));
		return cache;
	}
}
