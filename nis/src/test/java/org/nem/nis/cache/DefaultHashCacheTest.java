package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.HashMetaDataPair;
import org.nem.core.test.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultHashCacheTest extends HashCacheTest<DefaultHashCache> {

	@Override
	protected DefaultHashCache createWritableCache() {
		return new DefaultHashCache().copy();
	}

	@Override
	protected DefaultHashCache createReadOnlyCacheWithRetentionTime(final int retentionTime) {
		return new DefaultHashCache(50, retentionTime);
	}

	// region deepCopy

	// TODO 20150930 BR -> BR: move to HashCacheTest once deepCopy is added to CopyableCache
	@Test
	public void deepCopyCreatesIndependentCache() {
		// Arrange:
		final List<HashMetaDataPair> pairs = Arrays.asList(123, 234, 345, 456).stream()
				.map(timeStamp -> new HashMetaDataPair(Utils.generateRandomHash(), createMetaDataWithTimeStamp(timeStamp)))
				.collect(Collectors.toList());
		final DefaultHashCache cache = this.createReadOnlyCacheWithRetentionTime(123);
		final DefaultHashCache copy = cache.copy();
		copy.putAll(pairs);
		copy.commit();

		// Act:
		final DefaultHashCache deepCopy = cache.deepCopy();
		final DefaultHashCache tmp = deepCopy.copy();
		tmp.removeAll(pairs.stream().map(HashMetaDataPair::getHash).collect(Collectors.toList()));
		tmp.commit();

		// Assert:
		MatcherAssert.assertThat(cache.size(), IsEqual.equalTo(4));
		pairs.forEach(p -> MatcherAssert.assertThat(cache.hashExists(p.getHash()), IsEqual.equalTo(true)));
	}

	// endregion

}
