package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.mosaic.SmartTile;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

import java.util.stream.IntStream;

public class SmartTilesTest {

	// region constructor

	@Test
	public void smartTilesAreInitiallyEmpty() {
		// Act:
		final SmartTiles tiles = this.createCache();

		// Assert:
		Assert.assertThat(tiles.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region getCurrentSupply

	@Test
	public void getCurrentSupplyReturnsExpectedSupply() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 123));
		tiles.increaseSupply(createSmartTile(12, 234));

		final Quantity quantity = tiles.getCurrentSupply(Utils.createMosaicId(12));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(357)));
	}

	@Test
	public void getCurrentSupplyReturnsQuantityZeroIfSmartTileTypeIsUnknown() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 123));
		tiles.increaseSupply(createSmartTile(12, 234));

		final Quantity quantity = tiles.getCurrentSupply(Utils.createMosaicId(15));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.ZERO));
	}

	// endregion

	// region increaseSupply

	@Test
	public void canIncreaseSupplyIfSmartTileTypeIsUnknown() {
		// Arrange:
		final SmartTiles tiles = this.createCache();

		// Act:
		final Quantity quantity = tiles.increaseSupply(createSmartTile(12, 123));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(123)));
		Assert.assertThat(tiles.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void canIncreaseSupplyIfSmartTileTypeIsKnown() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 123));

		// Act:
		final Quantity quantity = tiles.increaseSupply(createSmartTile(12, 234));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(357)));
		Assert.assertThat(tiles.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(357)));
	}

	// endregion

	// region decreaseSupply

	@Test
	public void canDecreaseSupplyIfCurrentSupplyIsLargeEnough() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 234));

		// Act:
		final Quantity quantity = tiles.decreaseSupply(createSmartTile(12, 123));

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(Quantity.fromValue(111)));
		Assert.assertThat(tiles.getCurrentSupply(Utils.createMosaicId(12)), IsEqual.equalTo(Quantity.fromValue(111)));
	}

	@Test
 	public void cannotDecreaseSupplyIfSmartTileIsUnknown() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 123));

		// Assert:
		ExceptionAssert.assertThrows(v -> tiles.decreaseSupply(createSmartTile(15, 1)), NegativeQuantityException.class);
	}

	@Test
	public void cannotDecreaseSupplyIfCurrentSupplyIsTooSmall() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		tiles.increaseSupply(createSmartTile(12, 123));

		// Assert:
		ExceptionAssert.assertThrows(v -> tiles.decreaseSupply(createSmartTile(12, 124)), NegativeQuantityException.class);
	}

	// endregion

	// region / copy

	@Test
	public void copyCopiesAllEntries() {
		// Arrange:
		final SmartTiles tiles = this.createCache();
		addToCache(tiles, 4);

		// Act:
		final SmartTiles copy = tiles.copy();

		// Assert: initial copy
		Assert.assertThat(copy.size(), IsEqual.equalTo(4));
		IntStream.range(0, 4)
				.forEach(i -> Assert.assertThat(
						copy.getCurrentSupply(Utils.createMosaicId(i + 1)),
						IsEqual.equalTo(Quantity.fromValue(i + 1))));

		// Act: change supply
		tiles.increaseSupply(createSmartTile(3, 123));

		// Assert: the quantity should have changed the original but not in the copy
		Assert.assertThat(tiles.getCurrentSupply(Utils.createMosaicId(3)), IsEqual.equalTo(Quantity.fromValue(126)));
		Assert.assertThat(copy.getCurrentSupply(Utils.createMosaicId(3)), IsEqual.equalTo(Quantity.fromValue(3)));
	}

	// endregion

	private static void addToCache(final SmartTiles tiles, final int count) {
		IntStream.range(0, count)
				.forEach(i -> tiles.increaseSupply(new SmartTile(Utils.createMosaicId(i + 1), Quantity.fromValue(i + 1))));
	}

	private static SmartTile createSmartTile(final int id, final long quantity) {
		return new SmartTile(Utils.createMosaicId(id), Quantity.fromValue(quantity));
	}

	private SmartTiles createCache() {
		return new SmartTiles();
	}
}