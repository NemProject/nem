package org.nem.nis.cache;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.IsEquivalent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public class CacheContentsTest {

	@Test
	public void iteratorExposesCopyOfSourceCollection() {
		// Assert:
		assertCopyOfSourceCollection(contents -> StreamSupport.stream(contents.spliterator(), false).collect(Collectors.toList()));
	}

	@Test
	public void asCollectionExposesCopyOfSourceCollection() {
		// Assert:
		assertCopyOfSourceCollection(CacheContents::asCollection);
	}

	@Test
	public void streamExposesCopyOfSourceCollection() {
		// Assert:
		assertCopyOfSourceCollection(contents -> contents.stream().collect(Collectors.toList()));
	}

	private static void assertCopyOfSourceCollection(final Function<CacheContents<Integer>, Collection<Integer>> toCollection) {
		// Arrange:
		final List<Integer> original = new ArrayList<>(Arrays.asList(6, 2, 5));

		// Act:
		final CacheContents<Integer> contents = new CacheContents<>(original);
		original.clear();
		final Collection<Integer> cacheCollection = toCollection.apply(contents);

		// Assert:
		MatcherAssert.assertThat(original.isEmpty(), IsEqual.equalTo(true));

		// Assert:
		MatcherAssert.assertThat(cacheCollection.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cacheCollection, IsEquivalent.equivalentTo(Arrays.asList(6, 2, 5)));
	}
}
