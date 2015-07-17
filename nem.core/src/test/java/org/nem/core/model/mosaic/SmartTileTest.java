package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
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
		assertCannotCreateWithQuantity(MosaicConstants.MAX_QUANTITY + 1);
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
		final SmartTile smartTile1 = new SmartTile(createMosaicId(), Quantity.fromValue(MosaicConstants.MAX_QUANTITY));
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

	//region toString

	@Test
	public void toStringReturnsExpectedString() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId(new NamespaceId("BoB.SilveR"), "BaR");
		final SmartTile smartTile = new SmartTile(mosaicId, Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(smartTile.toString(), IsEqual.equalTo("bob.silver * BaR : 123"));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicId mosaicId = createMosaicId();
		final MosaicId diffMosaicId = new MosaicId(new NamespaceId("foo.bar"), "qux");
		final SmartTile smartTile = new SmartTile(mosaicId, Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(new SmartTile(createMosaicId(), Quantity.fromValue(123)), IsEqual.equalTo(smartTile));
		Assert.assertThat(new SmartTile(diffMosaicId, Quantity.fromValue(123)), IsNot.not(IsEqual.equalTo(smartTile)));
		Assert.assertThat(new SmartTile(createMosaicId(), Quantity.fromValue(234)), IsNot.not(IsEqual.equalTo(smartTile)));
		Assert.assertThat(new Object(), IsNot.not(IsEqual.equalTo(smartTile)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(mosaicId)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final MosaicId mosaicId = createMosaicId();
		final MosaicId diffMosaicId = new MosaicId(new NamespaceId("foo.bar"), "qux");
		final int hashCode = new SmartTile(mosaicId, Quantity.fromValue(123)).hashCode();

		// Assert:
		Assert.assertThat(new SmartTile(createMosaicId(), Quantity.fromValue(123)).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new SmartTile(diffMosaicId, Quantity.fromValue(123)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new SmartTile(createMosaicId(), Quantity.fromValue(234)).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	private static MosaicId createMosaicId() {
		return new MosaicId(new NamespaceId("foo.bar"), "baz");
	}
}
