package org.nem.nis.cache;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.*;

public class DefaultHashCacheCopyTest {
	private static final int RETENTION_TIME_SECONDS = 36 * 60 * 60;
	private static final int RETENTION_TIME_HOURS = 36;

	// region ReadOnlyHashCache

	// region size

	@Test
	public void sizeRespectsAddedAndRemoveHashes() {
		// Arrange:
		final Map<Hash, HashMetaData> original = createMap(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(10);
		final Map<Hash, HashMetaData> removeHashes = createMap(7);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				addedHashes,
				removeHashes,
				RETENTION_TIME_HOURS);

		// Act:
		final int size = copy.size();

		// Assert:
		Assert.assertThat(size, IsEqual.equalTo(5 + 10 - 7));
	}

	// endregion

	// region get

	@Test
	public void getReturnsMetaDataIfHashIsInOriginalMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(original, RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(
				copy.get(hashes.get(i)),
				IsEqual.equalTo(createMetaDataWithTimeStamp(i))));
	}

	@Test
	public void getReturnsMetaDataIfHashIsInAddedHashesMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(
				copy.get(hashes.get(i)),
				IsEqual.equalTo(createMetaDataWithTimeStamp(i))));
	}

	@Test
	public void getReturnsNullIfHashIsInOriginalMapAndInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.get(hashes.get(i)), IsNull.nullValue()));
	}

	@Test
	public void getReturnsNullIfHashIsNotInAnyMap() {
		// Arrange:
		final HashCache copy = new DefaultHashCacheCopy(
				createMap(10),
				createMap(10),
				createMap(10),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.get(Utils.generateRandomHash()), IsNull.nullValue()));
	}

	// endregion

	// region hashExists

	@Test
	public void hashExistsReturnsTrueIfHashIsInOriginalMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(original, RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void hashExistsReturnsTrueIfHashIsInAddedHashesMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void hashExistsReturnsFalseIfHashIsInOriginalMapAndInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(false)));
	}

	@Test
	public void hashExistsReturnsFalseIfHashIsNotInAnyMap() {
		// Arrange:
		final HashCache copy = new DefaultHashCacheCopy(
				createMap(10),
				createMap(10),
				createMap(10),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(Utils.generateRandomHash()), IsEqual.equalTo(false)));
	}

	// endregion

	// region anyHashExists

	@Test
	public void anyHashExistsReturnsTrueIfAnyHashIsInOriginalMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final List<Hash> testHashes = createHashes(5);
		testHashes.add(hashes.get(3));
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(original, RETENTION_TIME_HOURS);

		// Assert:
		Assert.assertThat(copy.anyHashExists(testHashes), IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsTrueIfAnyHashIsInAddedHashesMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final List<Hash> testHashes = createHashes(5);
		testHashes.add(hashes.get(3));
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		Assert.assertThat(copy.anyHashExists(testHashes), IsEqual.equalTo(true));
	}

	@Test
	public void anyHashExistsReturnsFalseIfAllHashesFoundInOriginalMapAreAlsoInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Assert:
		Assert.assertThat(copy.anyHashExists(hashes), IsEqual.equalTo(false));
	}

	@Test
	public void anyHashExistReturnsFalseIfNoHashIsFoundInAnyMap() {
		// Arrange:
		final HashCache copy = new DefaultHashCacheCopy(
				createMap(10),
				createMap(10),
				createMap(10),
				RETENTION_TIME_HOURS);

		// Assert:
		Assert.assertThat(copy.anyHashExists(createHashes(5)), IsEqual.equalTo(false));
	}

	// endregion

	// endregion

	// region HashCache

	// region put

	@Test
	public void putFailsIfHashIsInOriginalMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> ExceptionAssert.assertThrows(
				v -> copy.put(createPairWithHash(hashes.get(i))),
				IllegalArgumentException.class));
	}

	@Test
	public void putFailsIfHashIsInAddedHashesMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		IntStream.range(0, 5).forEach(i -> ExceptionAssert.assertThrows(
				v -> copy.put(createPairWithHash(hashes.get(i))),
				IllegalArgumentException.class));
	}

	@Test
	public void putPutsPairIntoAddedHashesMap() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Map<Hash, HashMetaData> addedHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.put(createPairWithHash(hash));

		// Assert:
		Assert.assertThat(copy.hashExists(hash), IsEqual.equalTo(true));
		Assert.assertThat(copy.size(), IsEqual.equalTo(1));
		Assert.assertThat(addedHashes.containsKey(hash), IsEqual.equalTo(true));
		Assert.assertThat(addedHashes.size(), IsEqual.equalTo(1));
	}

	@Test
	public void putRemovesHashFromRemovedHashesMapIfPresent() {
		// Arrange:
		final Hash hash = Utils.generateRandomHash();
		final Map<Hash, HashMetaData> removedHashes = new ConcurrentHashMap<>();
		removedHashes.put(hash, createMetaDataWithTimeStamp(0));
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.put(createPairWithHash(hash));

		// Assert:
		Assert.assertThat(copy.hashExists(hash), IsEqual.equalTo(true));
		Assert.assertThat(copy.size(), IsEqual.equalTo(1));
		Assert.assertThat(removedHashes.isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region putAll

	@Test
	public void putAllFailsIfAnyHashIsInOriginalMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final List<Hash> testHashes = createHashes(5);
		testHashes.add(hashes.get(3));
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> copy.putAll(createPairsWithHashes(testHashes)),
				IllegalArgumentException.class);
	}

	@Test
	public void putAllFailsIfAnyHashIsInAddedHashesMapAndNotInRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final List<Hash> testHashes = createHashes(5);
		testHashes.add(hashes.get(3));
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> copy.putAll(createPairsWithHashes(testHashes)),
				IllegalArgumentException.class);
	}

	@Test
	public void putAllPutsAllPairsIntoAddedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.putAll(createPairsWithHashes(hashes));

		// Assert:
		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(addedHashes.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
		Assert.assertThat(addedHashes.size(), IsEqual.equalTo(5));
	}

	@Test
	public void putAllRemovesHashesFromRemovedHashesMapIfPresent() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> removedHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.putAll(createPairsWithHashes(hashes));

		// Assert:
		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(true)));
		Assert.assertThat(removedHashes.isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region remove

	@Test
	public void removeDoesNotRemoveHashFromOriginalMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.remove(hashes.get(1));
		copy.remove(hashes.get(3));

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(0 == i % 2)));
		Assert.assertThat(original.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(original.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void removeRemovesHashFromAddedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.remove(hashes.get(1));
		copy.remove(hashes.get(3));

		// Assert:
		Assert.assertThat(addedHashes.size(), IsEqual.equalTo(3));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(addedHashes.containsKey(hashes.get(i)), IsEqual.equalTo(0 == i % 2)));
	}

	@Test
	public void removeAddsHashToRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removeHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removeHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.remove(hashes.get(1));
		copy.remove(hashes.get(3));

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(0 == i % 2)));
		Assert.assertThat(removeHashes.size(), IsEqual.equalTo(2));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(removeHashes.containsKey(hashes.get(i)), IsEqual.equalTo(1 == i % 2)));
	}

	// endregion

	// region removeAll

	@Test
	public void removeAllDoesNotRemoveHashesFromOriginalMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.removeAll(hashes);

		// Assert:
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(copy.hashExists(hashes.get(i)), IsEqual.equalTo(false)));
		Assert.assertThat(original.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(original.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void removeAllRemovesHashesFromAddedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.removeAll(hashes);

		// Assert:
		Assert.assertThat(addedHashes.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void removeAllAddsHashesToRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removeHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removeHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.removeAll(hashes);

		// Assert:
		Assert.assertThat(removeHashes.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(removeHashes.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	// endregion

	// region clear

	@Test
	public void clearDoesNotRemoveHashesMapFromOriginalMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final HashCache copy = createCopyWithOriginal(original);

		// Act:
		copy.clear();

		// Assert:
		Assert.assertThat(original.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(original.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void clearRemovesHashesFromAddedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.clear();

		// Assert:
		Assert.assertThat(addedHashes.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void clearAddsHashesFromOriginalMapToRemovedHashesMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removedHashes = new ConcurrentHashMap<>();
		final HashCache copy =  new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.clear();

		// Assert:
		Assert.assertThat(removedHashes.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(removedHashes.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	// endregion

	// region prune

	@Test
	public void pruneDoesNotRemoveHashesFromOriginalMap() {
		// Arrange:
		final Map<Hash, HashMetaData> original = createMapWithTimeStamps(0, 10, 20, 30, 40);
		final HashCache copy = createCopyWithOriginal(original);

		// Act:
		copy.prune(TimeInstant.ZERO);

		// Assert:
		Assert.assertThat(original.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(
				original.containsValue(createMetaDataWithTimeStamp(10 * i)),
				IsEqual.equalTo(true)));
	}

	@Test
	public void pruneRemovesHashesFromAddedHashesMap() {
		// Arrange:
		final Map<Hash, HashMetaData> addedHashes = createMapWithTimeStamps(0, 10, 20, 30, 40);
		final HashCache copy = new DefaultHashCacheCopy(
				new ConcurrentHashMap<>(),
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.prune(new TimeInstant(RETENTION_TIME_SECONDS + 25));

		// Assert:
		Assert.assertThat(addedHashes.size(), IsEqual.equalTo(2));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(
				addedHashes.containsValue(createMetaDataWithTimeStamp(10 * i)),
				IsEqual.equalTo(2 < i)));
	}

	@Test
	public void pruneAddsHashesFromOriginalMapToRemovedHashesMap() {
		// Arrange:
		final Map<Hash, HashMetaData> original = createMapWithTimeStamps(5, 15, 25, 35, 45);
		final Map<Hash, HashMetaData> addedHashes = createMapWithTimeStamps(0, 10, 20, 30, 40);
		final Map<Hash, HashMetaData> removedHashes = new ConcurrentHashMap<>();
		final HashCache copy = new DefaultHashCacheCopy(
				original,
				addedHashes,
				removedHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.prune(new TimeInstant(RETENTION_TIME_SECONDS + 25));

		// Assert:
		Assert.assertThat(removedHashes.size(), IsEqual.equalTo(2));
		IntStream.range(0, 10).forEach(i -> Assert.assertThat(
				removedHashes.containsValue(createMetaDataWithTimeStamp(5 * i)),
				IsEqual.equalTo(1 == i || 3 == i)));
	}

	// endregion

	// endregion

	// region commit

	@Test
	public void commitAddsAllHashesInAddedHashesMapToOriginalMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = new ConcurrentHashMap<>();
		final Map<Hash, HashMetaData> addedHashes = createMap(hashes);
		final CommittableCache copy = new DefaultHashCacheCopy(
				original,
				addedHashes,
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);

		// Act:
		copy.commit();

		// Assert:
		Assert.assertThat(original.size(), IsEqual.equalTo(5));
		IntStream.range(0, 5).forEach(i -> Assert.assertThat(original.containsKey(hashes.get(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void commitRemovesAllHashesInRemovedHashesMapFromOriginalMap() {
		// Arrange:
		final List<Hash> hashes = createHashes(5);
		final Map<Hash, HashMetaData> original = createMap(hashes);
		final Map<Hash, HashMetaData> removedHashes = createMap(hashes);
		final CommittableCache copy = new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				removedHashes,
				RETENTION_TIME_HOURS);

		// Act:
		copy.commit();

		// Assert:
		Assert.assertThat(original.isEmpty(), IsEqual.equalTo(true));
	}

	// endregion

	// region utilities

	private static DefaultHashCacheCopy createCopyWithOriginal(final Map<Hash, HashMetaData> original) {
		return new DefaultHashCacheCopy(
				original,
				new ConcurrentHashMap<>(),
				new ConcurrentHashMap<>(),
				RETENTION_TIME_HOURS);
	}

	private static HashMetaDataPair createPairWithHash(final Hash hash) {
		return new HashMetaDataPair(hash, createMetaDataWithTimeStamp(0));
	}

	private static List<HashMetaDataPair> createPairsWithHashes(final List<Hash> hashes) {
		return hashes.stream()
				.map(h -> new HashMetaDataPair(h, createMetaDataWithTimeStamp(0)))
				.collect(Collectors.toList());
	}

	private static HashMetaData createMetaDataWithTimeStamp(final int timeStamp) {
		return new HashMetaData(BlockHeight.ONE, new TimeInstant(timeStamp));
	}

	private static Map<Hash, HashMetaData> createMap(final int count) {
		final Map<Hash, HashMetaData> map = new ConcurrentHashMap<>();
		IntStream.range(0, count).forEach(i -> map.put(Utils.generateRandomHash(), createMetaDataWithTimeStamp(i)));
		return map;
	}

	private static Map<Hash, HashMetaData> createMap(final List<Hash> hashes) {
		final Map<Hash, HashMetaData> map = new ConcurrentHashMap<>();
		IntStream.range(0, hashes.size()).forEach(i -> map.put(hashes.get(i), createMetaDataWithTimeStamp(i)));
		return map;
	}

	private static Map<Hash, HashMetaData> createMapWithTimeStamps(final int... timeStamps) {
		final Map<Hash, HashMetaData> map = new ConcurrentHashMap<>();
		IntStream.range(0, timeStamps.length).forEach(i -> map.put(
				Utils.generateRandomHash(),
				createMetaDataWithTimeStamp(timeStamps[i])));
		return map;
	}

	protected static List<Hash> createHashes(final int count) {
		final List<Hash> hashes = new ArrayList<>();
		IntStream.range(0, count).forEach(i -> hashes.add(Utils.generateRandomHash()));

		return hashes;
	}

	// endregion
}
