package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.*;

import java.util.stream.IntStream;

public class MosaicsTest {

	// region constructor

	@Test
	public void mosaicsAreInitiallyEmpty() {
		// Act:
		final Mosaics mosaics = this.createCache();

		// Assert:
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region get

	@Test
	public void getReturnsExpectedMosaic() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		final Mosaic original = Utils.createMosaic("vouchers", "gift vouchers");
		mosaics.add(original);

		// Act:
		final Mosaic mosaic = mosaics.get(new MosaicId(new NamespaceId("vouchers"), "gift vouchers"));

		// Assert:
		Assert.assertThat(mosaic, IsEqual.equalTo(original));
	}

	// endregion

	// region contains

	@Test
	public void containsReturnsTrueIfMosaicExistsInCache() {
		// Arrange:
		final String[] namespaceIds = { "id1", "id1", "id2", "id3", };
		final String[] names = { "name1", "name2", "name1", "name3", };
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, namespaceIds, names);

		// Assert:
		IntStream.range(0, namespaceIds.length)
				.forEach(i -> Assert.assertThat(
						mosaics.contains(new MosaicId(new NamespaceId(namespaceIds[i]), names[i])),
						IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfMosaicDoesNotExistInCache() {
		// Arrange:
		final String[] namespaceIds = { "id1", "id1", "id2", "id3", };
		final String[] names = { "name1", "name2", "name1", "name3", };
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, namespaceIds, names);

		// Assert:
		Assert.assertThat(mosaics.contains(new MosaicId(new NamespaceId("id1"), "name3")), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(new MosaicId(new NamespaceId("id2"), "name2")), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(new MosaicId(new NamespaceId("id3"), "name1")), IsEqual.equalTo(false));
	}

	// endregion

	// region add/remove

	@Test
	public void canAddDifferentMosaicsToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();

		// Act:
		addToCache(mosaics, 3);

		// Assert:
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		IntStream.range(0, 3).forEach(i -> Assert.assertThat(mosaics.contains(Utils.createMosaicId(i + 1)),	IsEqual.equalTo(true)));
	}

	@Test
	public void cannotAddSameMosaicTwiceToCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.add(Utils.createMosaic(2)), IllegalArgumentException.class);
	}

	@Test
	public void canRemoveExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 5);

		// Act:
		mosaics.remove(Utils.createMosaic(2));
		mosaics.remove(Utils.createMosaic(4));

		// Assert:
		Assert.assertThat(mosaics.size(), IsEqual.equalTo(3));
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(1)), IsEqual.equalTo(true));
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(2)), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(3)), IsEqual.equalTo(true));
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(4)), IsEqual.equalTo(false));
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(5)), IsEqual.equalTo(true));
	}

	@Test
	public void cannotRemoveNonExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.remove(Utils.createMosaic(7)), IllegalArgumentException.class);
	}

	@Test
	public void cannotRemovePreviouslyExistingNonExistingMosaicFromCache() {
		// Arrange:
		final Mosaics mosaics = this.createCache();
		addToCache(mosaics, 3);
		mosaics.remove(Utils.createMosaic(2));

		// Assert:
		ExceptionAssert.assertThrows(v -> mosaics.remove(Utils.createMosaic(2)), IllegalArgumentException.class);
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
		Assert.assertThat(copy.size(), IsEqual.equalTo(4));
		IntStream.range(0, 4).forEach(i -> Assert.assertThat(copy.contains(Utils.createMosaicId(i + 1)), IsEqual.equalTo(true)));

		// Act: remove a mosaic
		mosaics.remove(Utils.createMosaic(3));

		// Assert: the mosaic should always be removed from the original but not removed from the copy
		Assert.assertThat(mosaics.contains(Utils.createMosaicId(3)), IsEqual.equalTo(false));
		Assert.assertThat(copy.contains(Utils.createMosaicId(3)), IsEqual.equalTo(true));
	}

	// endregion

	private static void addToCache(final Mosaics mosaics, final String[] namespaceIds, final String[] names) {
		IntStream.range(0, namespaceIds.length)
				.forEach(i -> mosaics.add(Utils.createMosaic(namespaceIds[i], names[i])));
	}

	private static void addToCache(final Mosaics mosaics, final int count) {
		IntStream.range(0, count).forEach(i -> mosaics.add(Utils.createMosaic(i + 1)));
	}

	private Mosaics createCache() {
		return new Mosaics();
	}
}