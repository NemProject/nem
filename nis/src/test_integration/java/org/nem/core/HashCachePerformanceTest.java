package org.nem.core;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.HashCache;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.logging.Logger;

public class HashCachePerformanceTest {
	private static final Logger LOGGER = Logger.getLogger(HashCachePerformanceTest.class.getName());

	@Test
	public void putPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);

		// Warm-up:
		for (int i=0; i<count; i++) {
			context.cache.put(context.hashes.get(i), context.timeStamps.get(i));
		}

		// Act:
		context.cache.clear();
		final long start = System.currentTimeMillis();
		for (int i=0; i<count; i++) {
			context.cache.put(context.hashes.get(i), context.timeStamps.get(i));
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Adding %d entries to the hash cache needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(context.cache.size(), IsEqual.equalTo(count));
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void putAllPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);

		// Warm-up:
		context.cache.putAll(context.hashes, context.timeStamps);

		// Act:
		context.cache.clear();
		final long start = System.currentTimeMillis();
		context.cache.putAll(context.hashes, context.timeStamps);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Add all %d entries to the hash cache needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(context.cache.size(), IsEqual.equalTo(count));
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void shallowCopyPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		context.cache.putAll(context.hashes, context.timeStamps);

		// Act:
		final long start = System.currentTimeMillis();
		final HashCache copy = context.cache.shallowCopy();
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("shallow copy with %d entries needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(count));
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void shallowCopyToPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final HashCache copy = new HashCache();
		context.cache.putAll(context.hashes, context.timeStamps);

		// Act:
		final long start = System.currentTimeMillis();
		context.cache.shallowCopyTo(copy);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("shallow copy to with %d entries needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(count));
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void hashExistsPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final Hash hash = Utils.generateRandomHash();
		context.cache.putAll(context.hashes, context.timeStamps);

		// Warm-up:
		for (int i=0; i<count; i++) {
			context.cache.hashExists(hash);
		}

		// Act:
		long start = System.currentTimeMillis();
		for (int i=0; i<count; i++) {
			context.cache.hashExists(hash);
		}

		long stop = System.currentTimeMillis();
		LOGGER.info(String.format("search for existent hash %d times needed %dms", count, (stop - start)));

		// Act:
		start = System.currentTimeMillis();
		for (int i=0; i<count; i++) {
			context.cache.hashExists(context.hashes.get(i));
		}

		stop = System.currentTimeMillis();
		LOGGER.info(String.format("search for non existent hash %d times needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final List<Hash> hashes = context.createHashes(120);
		context.cache.putAll(context.hashes, context.timeStamps);

		// Warm-up:
		context.cache.anyHashExists(hashes);

		// Act:
		long start = System.currentTimeMillis();
		for (int i=0; i<1000; i++) {
			context.cache.anyHashExists(hashes);
		}

		long stop = System.currentTimeMillis();
		LOGGER.info(String.format("%d batch searches for %d non existent hashes needed %dms", 1000, 120, (stop - start)));

		// Assert:
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	private class TestContext {
		private final HashCache cache;
		private final List<Hash> hashes;
		private final List<TimeInstant> timeStamps;

		private TestContext(final int count) {
			this.cache = new HashCache(count);
			this.hashes = createHashes(count);
			this.timeStamps = createTimeStamps(count);
		}

		private List<Hash> createHashes(final int count) {
			final List<Hash> hashes = new ArrayList<>();
			for (int i=0; i<count; i++) {
				hashes.add(Utils.generateRandomHash());
			}

			return hashes;
		}

		private List<TimeInstant> createTimeStamps(final int count) {
			final List<TimeInstant> timeStamps = new ArrayList<>();
			for (int i=0; i<count; i++) {
				timeStamps.add(Utils.generateRandomTimeStamp());
			}

			return timeStamps;
		}
	}
}
