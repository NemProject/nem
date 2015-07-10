package org.nem.core.model.mosaic;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.ExceptionAssert;

import java.util.Arrays;

public class SmartTileTest {

	// region ctor

	@Test
	public void canCreateSmartTileFromValidParameters() {
		// Act:
		final SmartTile smartTile = new SmartTile(createMosaicId(), Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void cannotCreateSmartTileWithNullParameter() {
		// Assert:
		Arrays.asList("mosaicId", "quantity").forEach(SmartTileTest::assertCannotCreateWithNullParameter);
	}

	@Test
	public void cannotCreateSmartTileWithOutOfRangeQuantity() {
		// Assert:
		assertCannotCreateWithQuantity(-1);
		assertCannotCreateWithQuantity(MosaicProperties.MAX_QUANTITY + 1);
	}

	private static void assertCannotCreateWithNullParameter(final String parameterName) {
		ExceptionAssert.assertThrows(v -> new SmartTile(
				parameterName.equals("mosaicId") ? null : createMosaicId(),
				parameterName.equals("quantity") ? null : Quantity.fromValue(123)), IllegalArgumentException.class);
	}

	private static void assertCannotCreateWithQuantity(final long quantity) {
		ExceptionAssert.assertThrows(v -> new SmartTile(createMosaicId(), Quantity.fromValue(quantity)), IllegalArgumentException.class);
	}

	// endregion

	// add / subtract

	@Test
	public void canAddSmartTileWithSameMosaicId() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(createMosaicId(), Quantity.fromValue(234));

		// Act:
		final SmartTile smartTile = smartTile1.add(smartTile2);

		// Assert:
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(357)));
	}

	@Test
	public void cannotAddSmartTileWithDifferentMosaicIds() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(new MosaicId(new NamespaceId("foo.bar"), "qux"), Quantity.fromValue(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> smartTile1.add(smartTile2), IllegalArgumentException.class);
	}

	@Test
	public void cannotAddSmartTileWhenResultingQuantityIsOutOfRange() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(MosaicProperties.MAX_QUANTITY));
		final SmartTile smartTile2 = new SmartTile(createMosaicId(), Quantity.fromValue(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> smartTile1.add(smartTile2), IllegalArgumentException.class);
	}

	@Test
	public void canSubtractSmartTileWithSameMosaicId() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(createMosaicId(), Quantity.fromValue(234));

		// Act:
		final SmartTile smartTile = smartTile2.subtract(smartTile1);

		// Assert:
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.fromValue(111)));
	}

	@Test
	public void canSubtractSmartTileWhenResultingQuantityIsZero() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(createMosaicId(), Quantity.fromValue(123));

		// Act:
		final SmartTile smartTile = smartTile2.subtract(smartTile1);

		// Assert:
		Assert.assertThat(smartTile.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(smartTile.getQuantity(), IsEqual.equalTo(Quantity.ZERO));
	}

	@Test
	public void cannotSubtractSmartTileWithDifferentMosaicIds() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(new MosaicId(new NamespaceId("foo.bar"), "qux"), Quantity.fromValue(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> smartTile2.subtract(smartTile1), IllegalArgumentException.class);
	}

	@Test
	public void cannotSubtractSmartTileWhenResultingQuantityIsOutOfRange() {
		// Arrange:
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(123));
		final SmartTile smartTile2 = new SmartTile(createMosaicId(), Quantity.fromValue(234));

		// Assert:
		ExceptionAssert.assertThrows(v -> smartTile1.subtract(smartTile2), IllegalArgumentException.class);
	}

	// endregion

	private static MosaicId createMosaicId() {
		return new MosaicId(new NamespaceId("foo.bar"), "baz");
	}
}
