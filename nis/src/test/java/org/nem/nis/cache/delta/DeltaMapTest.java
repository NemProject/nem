package org.nem.nis.cache.delta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;

import java.util.*;

public abstract class DeltaMapTest<TMap extends DeltaMap<Integer, String> & CopyableDeltaMap<TMap>> {

	// region abstract functions

	/**
	 * Creates a delta map with the specified capacity.
	 *
	 * @param capacity The capacity.
	 * @return The map.
	 */
	protected abstract TMap createMapWithCapacity(final int capacity);

	/**
	 * Creates a delta map with the specified values.
	 *
	 * @param initialValues The initial values.
	 * @return The map.
	 */
	protected abstract TMap createMapWithValues(final Map<Integer, String> initialValues);

	// endregion

	// region constructor

	@Test
	public void canCreateMapWithInitialCapacity() {
		// Arrange:
		final DeltaMap<?, ?> map = this.createMapWithCapacity(11);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(0));
	}

	@Test
	@SuppressWarnings("serial")
	public void canCreateMapWithInitialValues() {
		// Arrange:
		final Map<Integer, String> initialValues = new HashMap<Integer, String>() {
			{
				this.put(7, "aaa");
				this.put(8, "bbb");
				this.put(5, "ccc");
			}
		};
		final DeltaMap<Integer, String> map = this.createMapWithValues(initialValues);

		// Assert:
		assertContents(map, Arrays.asList(createEntry(7, "aaa"), createEntry(8, "bbb"), createEntry(5, "ccc")));
	}

	// endregion

	private static Map.Entry<Integer, String> createEntry(final Integer key, final String value) {
		return new Map.Entry<Integer, String>() {
			@Override
			public Integer getKey() {
				return key;
			}

			@Override
			public String getValue() {
				return value;
			}

			@Override
			public String setValue(final String value) {
				return null;
			}
		};
	}

	private static void assertContents(final DeltaMap<Integer, String> map, final Collection<Map.Entry<Integer, String>> entries) {
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(entries.size()));
		entries.stream().forEach(e -> {
			MatcherAssert.assertThat(map.containsKey(e.getKey()), IsEqual.equalTo(true));
			MatcherAssert.assertThat(map.get(e.getKey()), IsEqual.equalTo(e.getValue()));
		});
	}
}
