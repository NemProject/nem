package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultHashCachePerformanceITCase {
	private static final Logger LOGGER = Logger.getLogger(DefaultHashCachePerformanceITCase.class.getName());

	@Test
	public void putPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);

		// Warm-up:
		for (int i = 0; i < count; i++) {
			context.cache.put(context.pairs.get(i));
		}

		// Act:
		context.cache.clear();
		final long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			context.cache.put(context.pairs.get(i));
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("Adding %d entries to the hash cache needed %dms", count, (stop - start)));

		// Assert:
		MatcherAssert.assertThat(context.cache.size(), IsEqual.equalTo(count));
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(context.cache.size(), IsEqual.equalTo(count));
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void copyPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		context.cache.putAll(context.pairs);

		// Act:
		final long start = System.currentTimeMillis();
		final DefaultHashCache copy = context.cache.copy();
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("shallow copy with %d entries needed %dms", count, (stop - start)));

		// Assert:
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(count));
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void shallowCopyToPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final DefaultHashCache copy = new DefaultHashCache();
		context.cache.putAll(context.pairs);

		// Act:
		final long start = System.currentTimeMillis();
		context.cache.shallowCopyTo(copy);
		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("shallow copy to with %d entries needed %dms", count, (stop - start)));

		// Assert:
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(count));
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void hashExistsPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final Hash hash = Utils.generateRandomHash();
		context.cache.putAll(context.pairs);

		// Warm-up:
		for (int i = 0; i < count; i++) {
			context.cache.hashExists(hash);
		}

		// Act:
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			context.cache.hashExists(hash);
		}

		long stop = System.currentTimeMillis();
		LOGGER.info(String.format("search for non existent hash %d times needed %dms", count, (stop - start)));

		// Act:
		start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			context.cache.hashExists(context.pairs.get(i).getHash());
		}

		stop = System.currentTimeMillis();
		LOGGER.info(String.format("search for existent hash %d times needed %dms", count, (stop - start)));

		// Assert:
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsPerformanceTest() {
		// Arrange:
		final int count = 250_000;
		final TestContext context = new TestContext(count);
		final List<HashMetaDataPair> pairs = context.createPairs(120);
		context.cache.putAll(pairs);

		// Warm-up:
		context.cache.anyHashExists(pairs.stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));

		// Act:
		final long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			context.cache.anyHashExists(pairs.stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));
		}

		final long stop = System.currentTimeMillis();
		LOGGER.info(String.format("%d batch searches for %d non existent hashes needed %dms", 1000, 120, (stop - start)));

		// Assert:
		MatcherAssert.assertThat(stop - start < 500, IsEqual.equalTo(true));
	}

	private static class TestContext {
		private final DefaultHashCache cache;
		private final List<HashMetaDataPair> pairs;

		private TestContext(final int count) {
			this.cache = new DefaultHashCache(count, 36);
			this.pairs = this.createPairs(count);
		}

		private List<HashMetaDataPair> createPairs(final int count) {
			final List<HashMetaDataPair> pairs = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				pairs.add(new HashMetaDataPair(Utils.generateRandomHash(), new HashMetaData(BlockHeight.ONE, Utils.generateRandomTimeStamp())));
			}

			return pairs;
		}
	}
}
