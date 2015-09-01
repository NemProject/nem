package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.*;

public class MosaicTest {

	// region ctor

	@Test
	public void canCreateMosaicFromValidParameters() {
		// Act:
		final Mosaic mosaic = new Mosaic(createMosaicId(), Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(mosaic.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(mosaic.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void cannotCreateMosaicWithNullParameter() {
		// Assert:
		Arrays.asList("mosaicId", "quantity").forEach(MosaicTest::assertCannotCreateWithNullParameter);
	}

	private static void assertCannotCreateWithNullParameter(final String parameterName) {
		ExceptionAssert.assertThrows(v -> new Mosaic(
						parameterName.equals("mosaicId") ? null : createMosaicId(),
						parameterName.equals("quantity") ? null : Quantity.fromValue(123)),
				IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripMosaic() {
		// Arrange:
		final Mosaic original = new Mosaic(createMosaicId(), Quantity.fromValue(123));

		// Act:
		final Mosaic mosaic = new Mosaic(Utils.roundtripSerializableEntity(original, null));

		// Assert:
		Assert.assertThat(mosaic.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(mosaic.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	// endregion

	//region toString

	@Test
	public void toStringReturnsExpectedString() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId(new NamespaceId("BoB.SilveR"), "BaR");
		final Mosaic mosaic = new Mosaic(mosaicId, Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(mosaic.toString(), IsEqual.equalTo("bob.silver * bar : 123"));
	}

	//endregion

	//region equals / hashCode

	private static final Map<String, Mosaic> DESC_TO_MOSAIC_MAP = new HashMap<String, Mosaic>() {
		{
			this.put("default", new Mosaic(createMosaicId(), Quantity.fromValue(123)));
			this.put("diff-mosaic-id", new Mosaic(new MosaicId(new NamespaceId("foo.bar"), "qux"), Quantity.fromValue(123)));
			this.put("diff-quantity", new Mosaic(createMosaicId(), Quantity.fromValue(132)));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Mosaic mosaic = new Mosaic(createMosaicId(), Quantity.fromValue(123));

		// Assert:
		for (final Map.Entry<String, Mosaic> entry : DESC_TO_MOSAIC_MAP.entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(mosaic)) : IsEqual.equalTo(mosaic));
		}
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new Mosaic(createMosaicId(), Quantity.fromValue(123)).hashCode();

		// Assert:
		for (final Map.Entry<String, Mosaic> entry : DESC_TO_MOSAIC_MAP.entrySet()) {
			Assert.assertThat(
					entry.getValue().hashCode(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(hashCode)) : IsEqual.equalTo(hashCode));
		}
	}

	private static boolean isDiffExpected(final String propertyName) {
		return !propertyName.equals("default");
	}

	//endregion

	private static MosaicId createMosaicId() {
		return new MosaicId(new NamespaceId("foo.bar"), "baz");
	}
}
