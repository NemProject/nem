package org.nem.core;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HashCachePerformanceTest {
	private static final Logger LOGGER = Logger.getLogger(HashCachePerformanceTest.class.getName());

	@Test
	public void putPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);

		// Warm-up:
		for (int i=0; i<count; i++) {
			context.cache.put(context.pairs.get(i));
		}

		// Act:
		context.cache.clear();
		final long start = System.currentTimeMillis();
		for (int i=0; i<count; i++) {
			context.cache.put(context.pairs.get(i));
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
		context.cache.putAll(context.pairs);

		// Act:
		context.cache.clear();
		final long start = System.currentTimeMillis();
		context.cache.putAll(context.pairs);
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
		context.cache.putAll(context.pairs);

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
		context.cache.putAll(context.pairs);

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
		context.cache.putAll(context.pairs);

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
		LOGGER.info(String.format("search for non existent hash %d times needed %dms", count, (stop - start)));

		// Act:
		start = System.currentTimeMillis();
		for (int i=0; i<count; i++) {
			context.cache.hashExists(context.pairs.get(i).getHash());
		}

		stop = System.currentTimeMillis();
		LOGGER.info(String.format("search for existent hash %d times needed %dms", count, (stop - start)));

		// Assert:
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final List<HashTimeInstantPair> pairs = context.createPairs(120);
		context.cache.putAll(pairs);

		// Warm-up:
		context.cache.anyHashExists(pairs.stream().map(HashTimeInstantPair::getHash).collect(Collectors.toList()));

		// Act:
		long start = System.currentTimeMillis();
		for (int i=0; i<1000; i++) {
			context.cache.anyHashExists(pairs.stream().map(HashTimeInstantPair::getHash).collect(Collectors.toList()));
		}

		long stop = System.currentTimeMillis();
		LOGGER.info(String.format("%d batch searches for %d non existent hashes needed %dms", 1000, 120, (stop - start)));

		// Assert:
		Assert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	private class TestContext {
		private final HashCache cache;
		private final List<HashTimeInstantPair> pairs;

		private TestContext(final int count) {
			this.cache = new HashCache(count);
			this.pairs = createPairs(count);
		}

		private List<HashTimeInstantPair> createPairs(final int count) {
			final List<HashTimeInstantPair> pairs = new ArrayList<>();
			for (int i=0; i<count; i++) {
				pairs.add(new HashTimeInstantPair(Utils.generateRandomHash(), Utils.generateRandomTimeStamp()));
			}

			return pairs;
		}
	}
}
