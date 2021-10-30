package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Supply;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.IntStream;

public class MosaicsTest {
	private static final String DEFAULT_NID = "vouchers";

	// region constructor

	@Test
	public void mosaicsAreInitiallyEmpty() {
		// Act:
		final Mosaics mosaics = this.createCache();

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(mosaics.getNamespaceId(), IsEqual.equalTo(new NamespaceId(DEFAULT_NID)));
	}

	// endregion

	// region deepSize

	@Test
	public void deepSizeRespectsHistorySizes() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);
		mosaics.add(createMosaicDefinition(2, 123));
		mosaics.add(createMosaicDefinition(2, 234));
		mosaics.add(createMosaicDefinition(3, 345));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(7));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedMosaic() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final MosaicDefinition original = Utils.createMosaicDefinition(DEFAULT_NID, "gift vouchers");
		mosaics.add(original);

		// Act:
		final MosaicEntry entry = mosaics.get(Utils.createMosaicId(DEFAULT_NID, "gift vouchers"));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(original));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(Supply.ZERO));
	}

	@Test
	public void getPreservesSupplyAcrossCalls() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final MosaicDefinition original = Utils.createMosaicDefinition(DEFAULT_NID, "gift vouchers");
		mosaics.add(original);
		mosaics.get(Utils.createMosaicId(DEFAULT_NID, "gift vouchers")).increaseSupply(new Supply(1337));

		// Act:
		final MosaicEntry entry = mosaics.get(Utils.createMosaicId(DEFAULT_NID, "gift vouchers"));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(original));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(new Supply(1337)));
	}

	@Test
	public void getReturnsNullIfMosaicDoesNotExistInCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		mosaics.add(Utils.createMosaicDefinition(DEFAULT_NID, "gift vouchers"));

		// Act:
		final MosaicEntry entry = mosaics.get(Utils.createMosaicId(DEFAULT_NID, "gift cards"));

		// Assert:
		MatcherAssert.assertThat(entry, IsNull.nullValue());
	}

	// endregion

	// region getMosaicIds

	@Test
	public void getMosaicIdsReturnsEmptyCollectionIfNoMosaicsArePresent() {
		// Arrange:
		final Mosaics mosaics = this.createCache();

		// Act:
		final Collection<MosaicId> mosaicIds = mosaics.getMosaicIds();

		// Assert:
		MatcherAssert.assertThat(mosaicIds.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getMosaicIdsReturnsAllMosaicIds() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final Collection<MosaicId> expectedMosaicIds = new HashSet<>();
		for (int i = 0; i < 10; ++i) {
			final MosaicDefinition definition = Utils.createMosaicDefinition(DEFAULT_NID, "my mosaic " + (i + 1));
			mosaics.add(definition);
			expectedMosaicIds.add(definition.getId());
		}

		// Act:
		final Collection<MosaicId> mosaicIds = mosaics.getMosaicIds();

		// Assert:
		MatcherAssert.assertThat(mosaicIds, IsEquivalent.equivalentTo(expectedMosaicIds));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicWithMatchingNamespaceExistsInCache() {
		// Arrange:
		final String[] names = {
				"name1", "name2", "name3", "name4",
		};
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		IntStream.range(0, names.length).forEach(i -> MatcherAssert
				.assertThat(mosaics.contains(Utils.createMosaicDefinition(DEFAULT_NID, names[i]).getId()), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicWithDifferentNamespaceExistsInCache() {
		// Arrange:
		final String[] names = {
				"name1", "name2", "name3", "name4",
		};
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		IntStream.range(0, names.length).forEach(i -> MatcherAssert
				.assertThat(mosaics.contains(Utils.createMosaicDefinition("coupons", names[i]).getId()), IsEqual.equalTo(false)));
	}

	@Test
	public void containsReturnsTrueOnlyWhenBothMosaicWithMatchingNamespaceAndNameExistsInCache() {
		// Arrange:
		final String[] names = {
				"name1", "name2", "name3", "name4",
		};
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		MatcherAssert.assertThat(mosaics.contains(Utils.createMosaicDefinition(DEFAULT_NID, "name2").getId()), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mosaics.contains(Utils.createMosaicDefinition(DEFAULT_NID, "name7").getId()), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mosaics.contains(Utils.createMosaicDefinition("coupons", "name2").getId()), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mosaics.contains(Utils.createMosaicDefinition("coupons", "name7").getId()), IsEqual.equalTo(false));
	}

	// endregion

	// region add

	@Test
	public void canAddDifferentMosaicsToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();

		// Act:
		addToCache(mosaics, 3);

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(3));
		IntStream.range(0, 3).forEach(i -> MatcherAssert.assertThat(mosaics.contains(createMosaicId(i + 1)), IsEqual.equalTo(true)));
	}

	@Test
	public void canAddSameMosaicTwiceToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Act:
		final MosaicEntry entry = mosaics.add(createMosaicDefinition(2, 123));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(Supply.fromValue(123)));
	}

	@Test
	public void addReturnsAddedMosaicEntryWhenMosaicHistoryIsEmpty() {
		// Arrange:
		final Mosaics mosaics = this.createCache();

		// Act:
		final MosaicEntry entry = mosaics.add(createMosaicDefinition(7));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(createMosaicDefinition(7)));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(Supply.ZERO));
	}

	@Test
	public void addReturnsAddedMosaicEntryWhenMosaicHistoryIsNonEmpty() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		mosaics.add(createMosaicDefinition(7));

		// Act:
		final MosaicEntry entry = mosaics.add(createMosaicDefinition(7, 567));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(2));
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(createMosaicDefinition(7)));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(Supply.fromValue(567)));
	}

	@Test
	public void cannotAddMosaicWithMismatchedNamespaceToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Act:
		ExceptionAssert.assertThrows(v -> mosaics.add(Utils.createMosaicDefinition("coupons", "2")), IllegalArgumentException.class);

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(3));
	}

	// endregion

	// region remove

	@Test
	public void removeExistingMosaicRemovesMosaicIdFromCacheIfHistoryDepthIsOne() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 5);

		// sanity check
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(mosaics.size()));

		// Act:
		mosaics.remove(createMosaicId(2));
		mosaics.remove(createMosaicId(4));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(1)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(2)), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(3)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(4)), IsEqual.equalTo(false));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(5)), IsEqual.equalTo(true));
	}

	@Test
	public void removeExistingMosaicDoesNotRemoveMosaicIdFromCacheIfHistoryDepthIsLargerThanOne() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 5);
		mosaics.add(createMosaicDefinition(2, 123));

		// sanity check
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(1 + mosaics.size()));
		MatcherAssert.assertThat(mosaics.get(createMosaicId(2)).getSupply(), IsEqual.equalTo(Supply.fromValue(123)));

		// Act:
		mosaics.remove(createMosaicId(2));

		// Assert:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(2)), IsEqual.equalTo(true));
		MatcherAssert.assertThat(mosaics.get(createMosaicId(2)).getSupply(), IsEqual.equalTo(Supply.ZERO));
	}

	@Test
	public void removeExistingMosaicRemovesLastEntryInHistory() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		IntStream.range(0, 5).forEach(i -> mosaics.add(createMosaicDefinition(2, 3 * i)));

		// Sanity:
		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(5));

		IntStream.range(0, 5).forEach(i -> {
			// Act:
			final MosaicEntry entry = mosaics.remove(createMosaicId(2));

			// Assert:
			MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(Supply.fromValue(3 * (4 - i))));
		});

		MatcherAssert.assertThat(mosaics.size(), IsEqual.equalTo(0));
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(0));
	}

	@Test
	public void cannotRemoveNonExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.remove(createMosaicId(7)), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemovePreviouslyExistingNonExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);
		mosaics.remove(createMosaicId(2));

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.remove(createMosaicId(2)), IllegalArgumentException.class);
	}

	@Test
	public void removeReturnsRemovedMosaicEntry() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		mosaics.add(createMosaicDefinition(7)).increaseSupply(new Supply(123));

		// Act:
		final MosaicEntry entry = mosaics.remove(createMosaicId(7));

		// Assert:
		MatcherAssert.assertThat(entry.getMosaicDefinition(), IsEqual.equalTo(createMosaicDefinition(7)));
		MatcherAssert.assertThat(entry.getSupply(), IsEqual.equalTo(new Supply(123)));
	}

	@Test
	public void removeReturnsRemovedHistoricalMosaicEntry() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 5);
		mosaics.add(createMosaicDefinition(2, 123));

		// Act:
		final MosaicEntry removedEntry = mosaics.remove(createMosaicId(2));

		// Assert:
		MatcherAssert.assertThat(removedEntry.getMosaicDefinition(), IsEqual.equalTo(createMosaicDefinition(2)));
		MatcherAssert.assertThat(removedEntry.getSupply(), IsEqual.equalTo(Supply.fromValue(123)));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllEntries() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);
		mosaics.add(createMosaicDefinition(2, 123));

		// Act:
		final Mosaics copy = mosaics.copy();

		// Assert: initial copy
		MatcherAssert.assertThat(copy.getNamespaceId(), IsEqual.equalTo(new NamespaceId(DEFAULT_NID)));
		MatcherAssert.assertThat(copy.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(copy.deepSize(), IsEqual.equalTo(5));
		IntStream.range(0, 4).forEach(i -> MatcherAssert.assertThat(copy.contains(createMosaicId(i + 1)), IsEqual.equalTo(true)));
		MatcherAssert.assertThat(copy.get(createMosaicId(2)).getSupply(), IsEqual.equalTo(Supply.fromValue(123)));
	}

	@Test
	public void copyMosaicRemovalIsUnlinked() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);

		// Act: remove a mosaic
		final Mosaics copy = mosaics.copy();
		mosaics.remove(createMosaicId(3));

		// Assert: the mosaic should be removed from the original but not removed from the copy
		MatcherAssert.assertThat(mosaics.contains(createMosaicId(3)), IsEqual.equalTo(false));
		MatcherAssert.assertThat(copy.contains(createMosaicId(3)), IsEqual.equalTo(true));
	}

	@Test
	public void copyHistoricalMosaicRemovalIsUnlinked() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);
		mosaics.add(createMosaicDefinition(2, 123));

		// Act: remove a mosaic history entry
		final Mosaics copy = mosaics.copy();
		mosaics.remove(createMosaicId(2));

		// Assert: the mosaic history entry should be removed from the original but not removed from the copy
		MatcherAssert.assertThat(mosaics.deepSize(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(mosaics.get(createMosaicId(2)).getSupply(), IsEqual.equalTo(Supply.ZERO));

		MatcherAssert.assertThat(copy.deepSize(), IsEqual.equalTo(5));
		MatcherAssert.assertThat(copy.get(createMosaicId(2)).getSupply(), IsEqual.equalTo(new Supply(123)));
	}

	@Test
	public void copyMosaicSupplyChangeIsUnlinked() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);

		// Act: change a mosaic's supply
		final Mosaics copy = mosaics.copy();
		mosaics.get(createMosaicId(3)).increaseSupply(new Supply(123));

		// Assert: the mosaic supply should be increased in the original but no the copy
		MatcherAssert.assertThat(mosaics.get(createMosaicId(3)).getSupply(), IsEqual.equalTo(new Supply(123)));
		MatcherAssert.assertThat(copy.get(createMosaicId(3)).getSupply(), IsEqual.equalTo(Supply.ZERO));
	}

	// endregion

	private static void addToCache(final Mosaics mosaics, final String[] names) {
		IntStream.range(0, names.length).forEach(i -> mosaics.add(Utils.createMosaicDefinition(DEFAULT_NID, names[i])));
	}

	private static void addToCache(final Mosaics mosaics, final int count) {
		IntStream.range(0, count).forEach(i -> mosaics.add(createMosaicDefinition(i + 1)));
	}

	private static MosaicId createMosaicId(final int id) {
		return Utils.createMosaicId(new NamespaceId(DEFAULT_NID), id + 1);
	}

	private static MosaicDefinition createMosaicDefinition(final int id) {
		return Utils.createMosaicDefinition(new NamespaceId(DEFAULT_NID), id + 1);
	}

	private static MosaicDefinition createMosaicDefinition(final int id, final long supply) {
		return Utils.createMosaicDefinition(new NamespaceId(DEFAULT_NID), id + 1, createMosaicPropertiesWithSupply(supply));
	}

	private static MosaicProperties createMosaicPropertiesWithSupply(final long supply) {
		final Properties properties = new Properties();
		properties.put("initialSupply", String.valueOf(supply));
		return new DefaultMosaicProperties(properties);
	}

	private Mosaics createCache() {
		return new Mosaics(new NamespaceId(DEFAULT_NID));
	}
}
