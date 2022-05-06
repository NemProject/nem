package org.nem.nis.cache.delta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.*;

public class DefaultSkipListMapTest {

	// region ctor

	@Test
	public void canCreateSkipListMapImplWithNoParameters() {
		// Act:
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>();

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(0));
	}

	@Test
	public void canCreateSkipListMapImplWithParameters() {
		// Arrange:
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		innerMap.put(TimeInstant.ZERO, Collections.singleton(Utils.generateRandomHash()));

		// Act:
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(1));
	}

	// endregion

	// region clear

	@Test
	public void clearRemovesAllElementsFromMap() {
		// Arrange:
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final Set<Hash> hashes1 = new HashSet<>(
				Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash()));
		final Set<Hash> hashes2 = new HashSet<>(Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash()));
		innerMap.put(TimeInstant.ZERO, hashes1);
		innerMap.put(new TimeInstant(1), hashes2);
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// sanity check
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(5));

		// Act:
		map.clear();

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfKeyValuePairExistsInMap() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		innerMap.put(TimeInstant.ZERO, Collections.singleton(hash));
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// Act:
		final boolean isContained = map.contains(TimeInstant.ZERO, hash);

		// Assert:
		MatcherAssert.assertThat(isContained, IsEqual.equalTo(true));
	}

	@Test
	public void containsReturnsFalseIfKeyValuePairDoesNotExistsInMap() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		innerMap.put(TimeInstant.ZERO, Collections.singleton(hash));
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// Act:
		final boolean isContained1 = map.contains(TimeInstant.ZERO, Utils.generateRandomHash());
		final boolean isContained2 = map.contains(new TimeInstant(1), hash);

		// Assert:
		MatcherAssert.assertThat(isContained1, IsEqual.equalTo(false));
		MatcherAssert.assertThat(isContained2, IsEqual.equalTo(false));
	}

	// endregion

	// region put / putAll

	@Test
	public void canPutKeyValuePairIntoMapIfKeyIsUnknown() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// Act:
		map.put(TimeInstant.ZERO, hash);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(map.contains(TimeInstant.ZERO, hash), IsEqual.equalTo(true));
	}

	@Test
	public void canPutKeyValuePairIntoMapIfKeyIsKnown() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);
		map.put(TimeInstant.ZERO, Utils.generateRandomHash());

		// Act:
		map.put(TimeInstant.ZERO, hash);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(map.contains(TimeInstant.ZERO, hash), IsEqual.equalTo(true));
	}

	@Test
	public void canPutKeyWithMultipleValuesIntoMapIfKeyIsUnknown() {
		// Arrange:
		final Set<Hash> hashes = new HashSet<>(
				Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash()));
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);

		// Act:
		map.put(TimeInstant.ZERO, hashes);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(3));
		hashes.stream().forEach(h -> MatcherAssert.assertThat(map.contains(TimeInstant.ZERO, h), IsEqual.equalTo(true)));
	}

	@Test
	public void canPutKeyWithMultipleValuesIntoMapIfKeyIsKnown() {
		// Arrange:
		final Set<Hash> hashes = new HashSet<>(
				Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash()));
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);
		map.put(TimeInstant.ZERO, Utils.generateRandomHash());

		// Act:
		map.put(TimeInstant.ZERO, hashes);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(4));
		hashes.stream().forEach(h -> MatcherAssert.assertThat(map.contains(TimeInstant.ZERO, h), IsEqual.equalTo(true)));
	}

	@Test
	public void putAllAddsAllKeyValuePairsToMap() {
		// Arrange:
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> original = new DefaultSkipListMap<>(innerMap);
		final DefaultSkipListMap<TimeInstant, Hash> map = createMapWithThreeKeys();

		// Act:
		original.putAll(map);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(6));
		map.entrySet().forEach(
				e -> e.getValue().stream().forEach(h -> MatcherAssert.assertThat(original.contains(e.getKey(), h), IsEqual.equalTo(true))));
	}

	// endregion

	// region remove / removeAll

	@Test
	public void removeRemovesKeyValuePairFromMap() {
		// Arrange:
		final List<Hash> hashes = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash());
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);
		map.put(TimeInstant.ZERO, new HashSet<>(hashes));

		// Act:
		map.remove(TimeInstant.ZERO, hashes.get(0));
		map.remove(TimeInstant.ZERO, hashes.get(2));

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(1));
		IntStream.range(0, 3)
				.forEach(i -> MatcherAssert.assertThat(map.contains(TimeInstant.ZERO, hashes.get(i)), IsEqual.equalTo(1 == i % 2)));
	}

	@Test
	public void removeAllRemovesAllGivenKeyValuePairsFromMap() {
		// Arrange:
		final List<Hash> hashes1 = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash());
		final List<Hash> hashes2 = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash(), Utils.generateRandomHash());
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> original = new DefaultSkipListMap<>(innerMap);
		original.put(TimeInstant.ZERO, new HashSet<>(hashes1));
		original.put(new TimeInstant(1), new HashSet<>(hashes2));
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap2 = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap2);
		map.put(TimeInstant.ZERO, hashes1.get(0));
		map.put(TimeInstant.ZERO, hashes1.get(2));
		map.put(new TimeInstant(1), hashes2.get(1));

		// Act:
		original.removeAll(map);

		// Assert:
		MatcherAssert.assertThat(map.size(), IsEqual.equalTo(3));
		IntStream.range(0, 3)
				.forEach(i -> MatcherAssert.assertThat(original.contains(TimeInstant.ZERO, hashes1.get(i)), IsEqual.equalTo(1 == i % 2)));
		IntStream.range(0, 3)
				.forEach(i -> MatcherAssert.assertThat(original.contains(new TimeInstant(1), hashes2.get(i)), IsEqual.equalTo(0 == i % 2)));
	}

	// endregion

	// region getValuesBefore

	@Test
	public void getValuesBeforeReturnsAllValuesForKeysThatLieBeforeTheGivenKey() {
		// Arrange:
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> original = new DefaultSkipListMap<>(innerMap);
		final Hash hash = Utils.generateRandomHash();
		final List<Hash> hashes1 = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash());
		final List<Hash> hashes2 = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash());
		final List<Hash> hashes3 = new ArrayList<>(hashes1);
		hashes3.add(hash);

		original.put(TimeInstant.ZERO, hash);
		original.put(new TimeInstant(5), hashes1);
		original.put(new TimeInstant(6), hashes2);

		// Act:
		final Collection<Hash> foundHashes1 = original.getValuesBefore(new TimeInstant(5));
		final Collection<Hash> foundHashes2 = original.getValuesBefore(new TimeInstant(6));

		// Assert:
		MatcherAssert.assertThat(foundHashes1, IsEqual.equalTo(Collections.singletonList(hash)));
		MatcherAssert.assertThat(foundHashes2, IsEquivalent.equivalentTo(hashes3));
	}

	// endregion

	// region entrySet

	@Test
	public void entrySetReturnsSetOfAllEntries() {
		// Arrange:
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);
		final Hash hash = Utils.generateRandomHash();
		final List<Hash> hashes = Arrays.asList(Utils.generateRandomHash(), Utils.generateRandomHash());
		map.put(TimeInstant.ZERO, hash);
		map.put(new TimeInstant(1), hashes);

		// Act:
		final Set<Map.Entry<TimeInstant, Set<Hash>>> entrySet = map.entrySet();
		final List<Map.Entry<TimeInstant, Set<Hash>>> sortedEntries = entrySet.stream()
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(entrySet.size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(sortedEntries.get(0).getKey(), IsEqual.equalTo(TimeInstant.ZERO));
		MatcherAssert.assertThat(sortedEntries.get(0).getValue(), IsEqual.equalTo(Collections.singleton(hash)));
		MatcherAssert.assertThat(sortedEntries.get(1).getKey(), IsEqual.equalTo(new TimeInstant(1)));
		MatcherAssert.assertThat(sortedEntries.get(1).getValue(), IsEquivalent.equivalentTo(hashes));
	}

	// endregion

	private static DefaultSkipListMap<TimeInstant, Hash> createMapWithThreeKeys() {
		final ConcurrentSkipListMap<TimeInstant, Set<Hash>> innerMap = new ConcurrentSkipListMap<>();
		final DefaultSkipListMap<TimeInstant, Hash> map = new DefaultSkipListMap<>(innerMap);
		map.put(TimeInstant.ZERO, Utils.generateRandomHash());
		map.put(new TimeInstant(1), Utils.generateRandomHash());
		map.put(new TimeInstant(1), Utils.generateRandomHash());
		map.put(new TimeInstant(2), Utils.generateRandomHash());
		map.put(new TimeInstant(2), Utils.generateRandomHash());
		map.put(new TimeInstant(2), Utils.generateRandomHash());
		return map;
	}
}
