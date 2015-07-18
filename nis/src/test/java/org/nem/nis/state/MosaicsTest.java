package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.stream.IntStream;

public class MosaicsTest {
	private static final String DEFAULT_NID = "vouchers";

	// region constructor

	@Test
	public void mosaicsAreInitiallyEmpty() {
		// Act:
		final Mosaics mosaics = this.createCache();

		// Assert:
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(0));
		Assert.assertThat(mosaics.getNamespaceId(), IsEqual.equalTo(new NamespaceId(DEFAULT_NID)));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedMosaic() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final Mosaic original = Utils.createMosaic(DEFAULT_NID, "gift vouchers");
		mosaics.add(original);

		// Act:
		final MosaicEntry entry = mosaics.get(new MosaicId(new NamespaceId(DEFAULT_NID), "gift vouchers"));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(original));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void getPreservesSupplyAcrossCalls() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final Mosaic original = Utils.createMosaic(DEFAULT_NID, "gift vouchers");
		mosaics.add(original);
		mosaics.get(new MosaicId(new NamespaceId(DEFAULT_NID), "gift vouchers")).increaseSupply(new Quantity(1337));

		// Act:
		final MosaicEntry entry = mosaics.get(new MosaicId(new NamespaceId(DEFAULT_NID), "gift vouchers"));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(original));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Quantity(1337)));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicWithMatchingNamespaceExistsInCache() {
		// Arrange:
		final String[] names = { "name1", "name2", "name3", "name4", };
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		IntStream.range(0, names.length)
				.forEach(i -> Assert.assertThat(
						mosaics.contains(Utils.createMosaic(DEFAULT_NID, names[i]).getId()),
						IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicWithDifferentNamespaceExistsInCache() {
		// Arrange:
		final String[] names = { "name1", "name2", "name3", "name4", };
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		IntStream.range(0, names.length)
				.forEach(i -> Assert.assertThat(
						mosaics.contains(Utils.createMosaic("coupons", names[i]).getId()),
						IsEqual.equalTo(false)));
	}

	@Test
	public void containsReturnsTrueOnlyWhenBothMosaicWithMatchingNamespaceAndNameExistsInCache() {
		// Arrange:
		final String[] names = { "name1", "name2", "name3", "name4", };
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, names);

		// Assert:
		Assert.assertThat(mosaics.contains(Utils.createMosaic(DEFAULT_NID, "name2").getId()), IsEqual.equalTo(true));
		Assert.assertThat(mosaics.contains(Utils.createMosaic(DEFAULT_NID, "name7").getId()), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(Utils.createMosaic("coupons", "name2").getId()), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(Utils.createMosaic("coupons", "name7").getId()), IsEqual.equalTo(false));
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
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		IntStream.range(0, 3).forEach(i -> Assert.assertThat(mosaics.contains(createMosaicId(i + 1)), IsEqual.equalTo(true)));
	}

	@Test
	public void cannotAddSameMosaicTwiceToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.add(createMosaic(2)), IllegalArgumentException.class);
	}

	@Test
	public void addReturnsAddedMosaicEntry() {
		// Arrange:
		final Mosaics mosaics = this.createCache();

		// Act:
		final MosaicEntry entry = mosaics.add(createMosaic(7));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(createMosaic(7)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void cannotAddMosaicWithMismatchedNamespaceToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.add(Utils.createMosaic("coupons", "2")), IllegalArgumentException.class);
	}

	// endregion

	// region remove

	@Test
	public void canRemoveExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 5);

		// Act:
		mosaics.remove(createMosaicId(2));
		mosaics.remove(createMosaicId(4));

		// Assert:
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		Assert.assertThat(mosaics.contains(createMosaicId(1)), IsEqual.equalTo(true));
		Assert.assertThat(mosaics.contains(createMosaicId(2)), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(createMosaicId(3)), IsEqual.equalTo(true));
		Assert.assertThat(mosaics.contains(createMosaicId(4)), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(createMosaicId(5)), IsEqual.equalTo(true));
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
		mosaics.add(createMosaic(7)).increaseSupply(new Quantity(123));

		// Act:
		final MosaicEntry entry = mosaics.remove(createMosaicId(7));

		// Assert:
		Assert.assertThat(entry.getMosaic(), IsEqual.equalTo(createMosaic(7)));
		Assert.assertThat(entry.getSupply(), IsEqual.equalTo(new Quantity(123)));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesAllEntries() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);

		// Act:
		final Mosaics copy = mosaics.copy();

		// Assert: initial copy
		Assert.assertThat(copy.getNamespaceId(), IsEqual.equalTo(new NamespaceId(DEFAULT_NID)));
		Assert.assertThat(copy.size(), IsEqual.equalTo(4));
		IntStream.range(0, 4).forEach(i -> Assert.assertThat(copy.contains(createMosaicId(i + 1)), IsEqual.equalTo(true)));
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
		Assert.assertThat(mosaics.contains(createMosaicId(3)), IsEqual.equalTo(false));
		Assert.assertThat(copy.contains(createMosaicId(3)), IsEqual.equalTo(true));
	}

	@Test
	public void copyMosaicSupplyChangeIsUnlinked() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 4);

		// Act: change a mosaic's supply
		final Mosaics copy = mosaics.copy();
		mosaics.get(createMosaicId(3)).increaseSupply(new Quantity(123));

		// Assert: the mosaic supply should be increased in the original but no the copy
		Assert.assertThat(mosaics.get(createMosaicId(3)).getSupply(), IsEqual.equalTo(new Quantity(123)));
		Assert.assertThat(copy.get(createMosaicId(3)).getSupply(), IsEqual.equalTo(Quantity.ZERO));
	}

	// endregion

	private static void addToCache(final Mosaics mosaics, final String[] names) {
		IntStream.range(0, names.length)
				.forEach(i -> mosaics.add(Utils.createMosaic(DEFAULT_NID, names[i])));
	}

	private static void addToCache(final Mosaics mosaics, final int count) {
		IntStream.range(0, count).forEach(i -> mosaics.add(createMosaic(i + 1)));
	}

	private static MosaicId createMosaicId(final int id) {
		return Utils.createMosaicId(new NamespaceId(DEFAULT_NID), id + 1);
	}

	private static Mosaic createMosaic(final int id) {
		return Utils.createMosaic(new NamespaceId(DEFAULT_NID), id + 1);
	}

	private Mosaics createCache() {
		return new Mosaics(new NamespaceId(DEFAULT_NID));
	}
}