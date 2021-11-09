package org.nem.nis.cache.delta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.util.*;

// TODO 20151124 J-J,B: should probably finish these tests
public class SkipListDeltaMapTest {

	// region ctor

	@Test
	public void canCreateEmptyMap() {
		// Act:
		final SkipListDeltaMap<TimeInstant, Hash> map = new SkipListDeltaMap<>();

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canCreateMapWithInitialValues() {
		// Arrange:
		final Map<TimeInstant, Set<Hash>> initialEntries = createStandardInnerMapEntries();
		final DefaultSkipListMap<TimeInstant, Hash> innerMap = createInnerMapWithValues(initialEntries);

		// Act:
		final SkipListDeltaMap<TimeInstant, Hash> map = new SkipListDeltaMap<>(innerMap);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(6));
		assertContents(map,
				Arrays.asList(createEntry(new TimeInstant(7), initialEntries.get(new TimeInstant(7))),
						createEntry(new TimeInstant(8), initialEntries.get(new TimeInstant(8))),
						createEntry(new TimeInstant(5), initialEntries.get(new TimeInstant(5)))));
	}

	// endregion

	// region clear

	@Test
	public void clearRemovesAllKeyValuePairs() {
		// Arrange:
		final Map<TimeInstant, Set<Hash>> initialEntries = createStandardInnerMapEntries();
		final SkipListDeltaMap<TimeInstant, Hash> map = new SkipListDeltaMap<>(createInnerMapWithValues(initialEntries));

		// Act:
		map.clear();

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(0));
	}

	// endregion

	private static Map.Entry<TimeInstant, Set<Hash>> createEntry(final TimeInstant key, final Set<Hash> value) {
		return new Map.Entry<TimeInstant, Set<Hash>>() {
			@Override
			public TimeInstant getKey() {
				return key;
			}

			@Override
			public Set<Hash> getValue() {
				return value;
			}

			@Override
			public Set<Hash> setValue(final Set<Hash> value) {
				return null;
			}
		};
	}

	@SuppressWarnings("serial")
	private static Map<TimeInstant, Set<Hash>> createStandardInnerMapEntries() {
		return new HashMap<TimeInstant, Set<Hash>>() {
			{
				this.put(new TimeInstant(7), new HashSet<>(Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash())));
				this.put(new TimeInstant(8), Collections.singleton(Utils.generateRandomHash()));
				this.put(new TimeInstant(5),
						new HashSet<>(Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash())));
			}
		};
	}

	private static void assertContents(final SkipListDeltaMap<TimeInstant, Hash> map,
			final Collection<Map.Entry<TimeInstant, Set<Hash>>> entries) {
		final int count = entries.stream().mapToInt(e -> e.getValue().size()).sum();
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(count));
		entries.stream().forEach(
				e -> e.getValue().stream().forEach(h -> MatcherAssert.assertThat(map.contains(e.getKey(), h), IsEqual.equalTo(true))));
	}

	private static DefaultSkipListMap<TimeInstant, Hash> createInnerMapWithValues(final Map<TimeInstant, Set<Hash>> entries) {
		final DefaultSkipListMap<TimeInstant, Hash> innerMap = new DefaultSkipListMap<>();
		entries.entrySet().forEach(e -> innerMap.put(e.getKey(), e.getValue()));
		return innerMap;
	}
}
