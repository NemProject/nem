package org.nem.nis.state;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.ExceptionAssert;

import java.util.stream.*;

public class SmartTileMapTest {

	// region ReadOnlySmartTileMap

	@Test
	public void mapIsInitiallyEmpty() {
		// Act:
		final SmartTileMap smartTileMap = new SmartTileMap();

		// Assert:
		Assert.assertThat(smartTileMap.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getReturnsExpectedSmartTileIfAvailableInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		Assert.assertThat(smartTileMap.size(), IsEqual.equalTo(5));
		IntStream.range(1, 6).forEach(i -> assertMapContainsSmartTile(smartTileMap, i));
	}

	@Test
	public void getReturnsNullIfSmartTileIsNotAvailableInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		Assert.assertThat(smartTileMap.size(), IsEqual.equalTo(5));
		IntStream.range(6, 10).forEach(i -> Assert.assertThat(smartTileMap.get(createMosaicId(i)), IsNull.nullValue()));
	}

	@Test
	public void containsReturnsTrueIfSmartTileIsAvailableInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		Assert.assertThat(smartTileMap.size(), IsEqual.equalTo(5));
		IntStream.range(1, 6).forEach(i -> Assert.assertThat(smartTileMap.contains(createMosaicId(i)), IsEqual.equalTo(true)));
	}

	@Test
	public void containsReturnsFalseIfSmartTileIsNotAvailableInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		Assert.assertThat(smartTileMap.size(), IsEqual.equalTo(5));
		IntStream.range(6, 10).forEach(i -> Assert.assertThat(smartTileMap.contains(createMosaicId(i)), IsEqual.equalTo(false)));
	}

	// endregion

	// region add

	@Test
	public void canAddSmartTileIfMapIsEmpty() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();

		// Act:
		final SmartTile smartTile = smartTileMap.add(createSmartTile(5));

		// Assert:
		Assert.assertThat(smartTileMap.get(smartTile.getMosaicId()), IsSame.sameInstance(smartTile));
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(5)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(5)));
	}

	@Test
	public void canAddSmartTileIfMapIsNotEmptyAndSmartTileIsNotContainedInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		final SmartTile smartTile = smartTileMap.add(createSmartTile(10));

		// Assert:
		Assert.assertThat(smartTileMap.get(smartTile.getMosaicId()), IsSame.sameInstance(smartTile));
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(10)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(10)));
	}

	@Test
	public void canAddSmartTileIfMapIsNotEmptyAndSmartTileIsContainedInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		final SmartTile smartTile = smartTileMap.add(createSmartTile(4));

		// Assert:
		Assert.assertThat(smartTileMap.get(smartTile.getMosaicId()), IsSame.sameInstance(smartTile));
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(4)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(8)));
	}

	@Test
	public void cannotAddSmartTileIfResultingQuantityIsOutOfRange() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> smartTileMap.add(new SmartTile(createMosaicId(4), Quantity.fromValue(MosaicProperties.MAX_QUANTITY))),
				IllegalArgumentException.class);
	}

	// endregion

	// region subtract

	@Test
	public void canSubtractSmartTileIfSmartTileWithSameMosaicIdIsAvailableInMapAndResultingQuantityIsPositive() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		final SmartTile smartTile = smartTileMap.subtract(new SmartTile(createMosaicId(4), Quantity.fromValue(2)));

		// Assert:
		Assert.assertThat(smartTileMap.get(smartTile.getMosaicId()), IsSame.sameInstance(smartTile));
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(4)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(2)));
	}

	@Test
	public void canSubtractSmartTileIfSmartTileWithSameMosaicIdIsAvailableInMapAndResultingQuantityIsZero() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		final SmartTile smartTile = smartTileMap.subtract(createSmartTile(4));

		// Assert:
		Assert.assertThat(smartTileMap.get(smartTile.getMosaicId()), IsSame.sameInstance(smartTile));
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(4)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void cannotSubtractSmartTileIfSmartTileWithSameMosaicIdIsNotAvailableInMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		ExceptionAssert.assertThrows(v -> smartTileMap.subtract(createSmartTile(6)), IllegalArgumentException.class);
	}

	@Test
	public void cannotSubtractSmartTileIfResultingQuantityIsOutOfRange() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Assert:
		ExceptionAssert.assertThrows(
				v -> smartTileMap.subtract(new SmartTile(createMosaicId(4), Quantity.fromValue(5))),
				IllegalArgumentException.class);
	}

	// endregion

	// region remove

	@Test
	public void canRemoveSmartTile() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		smartTileMap.remove(createMosaicId(2));
		smartTileMap.remove(createMosaicId(4));

		// Assert:
		IntStream.range(1, 6).forEach(i -> Assert.assertThat(smartTileMap.contains(createMosaicId(i)), IsEqual.equalTo(i % 2 == 1)));
	}

	// endregion

	// region copy

	@Test
	public void canCopyMap() {
		// Arrange:
		final SmartTileMap smartTileMap = new SmartTileMap();
		addToMap(smartTileMap, 5);

		// Act:
		final SmartTileMap copy = smartTileMap.copy();
		smartTileMap.add(createSmartTile(2));
		smartTileMap.add(createSmartTile(7));
		smartTileMap.subtract(createSmartTile(3));

		// Assert:
		Assert.assertThat(copy.size(), IsEqual.equalTo(5));
		IntStream.range(1, 6).forEach(i -> Assert.assertThat(smartTileMap.contains(createMosaicId(i)), IsEqual.equalTo(true)));
	}

	// endregion

	private void assertMapContainsSmartTile(final SmartTileMap map, final int i) {
		// Act:
		final SmartTile smartTile = map.get(createMosaicId(i));

		Assert.assertThat(smartTile, IsNull.notNullValue());
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId(i)));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(i)));
	}

	private static void addToMap(final SmartTileMap map, final int count) {
		LongStream.range(1, count + 1).forEach(i -> map.add(createSmartTile(i)));
	}

	private static SmartTile createSmartTile(final long i) {
		return new SmartTile(createMosaicId(i), Quantity.fromValue(i));
	}

	private static MosaicId createMosaicId(final long i) {
		final NamespaceId namespaceId = new NamespaceId(String.format("namespace%d", i));
		return new MosaicId(namespaceId, String.format("name%d", i));
	}
}
