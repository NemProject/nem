package org.nem.core.model.mosaic;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;

import java.util.*;

public class MosaicTransferPairTest {

	// region ctor

	@Test
	public void canCreatePairFromValidParameters() {
		// Act:
		final MosaicTransferPair pair = new MosaicTransferPair(createMosaicId(), Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(pair.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void cannotCreatePairWithNullParameter() {
		// Assert:
		Arrays.asList("mosaicId", "quantity").forEach(MosaicTransferPairTest::assertCannotCreateWithNullParameter);
	}

	private static void assertCannotCreateWithNullParameter(final String parameterName) {
		ExceptionAssert.assertThrows(v -> new MosaicTransferPair(
						parameterName.equals("mosaicId") ? null : createMosaicId(),
						parameterName.equals("quantity") ? null : Quantity.fromValue(123)),
				IllegalArgumentException.class);
	}

	// endregion

	// region serialization

	@Test
	public void canRoundTripPair() {
		// Arrange:
		final MosaicTransferPair original = new MosaicTransferPair(createMosaicId(), Quantity.fromValue(123));

		// Act:
		final MosaicTransferPair pair = new MosaicTransferPair(Utils.roundtripSerializableEntity(original, null));

		// Assert:
		Assert.assertThat(pair.getMosaicId(), IsEqual.equalTo(createMosaicId()));
		Assert.assertThat(pair.getQuantity(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	// endregion

	//region toString

	@Test
	public void toStringReturnsExpectedString() {
		// Arrange:
		final MosaicId mosaicId = new MosaicId(new NamespaceId("BoB.SilveR"), "BaR");
		final MosaicTransferPair pair = new MosaicTransferPair(mosaicId, Quantity.fromValue(123));

		// Assert:
		Assert.assertThat(pair.toString(), IsEqual.equalTo("bob.silver * BaR : 123"));
	}

	//endregion

	//region equals / hashCode

	private static final Map<String, MosaicTransferPair> DESC_TO_PAIR_MAP = new HashMap<String, MosaicTransferPair>() {
		{
			this.put("default", new MosaicTransferPair(createMosaicId(), Quantity.fromValue(123)));
			this.put("diff-mosaic-id", new MosaicTransferPair(new MosaicId(new NamespaceId("foo.bar"), "qux"), Quantity.fromValue(123)));
			this.put("diff-quantity", new MosaicTransferPair(createMosaicId(), Quantity.fromValue(132)));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final MosaicTransferPair pair = new MosaicTransferPair(createMosaicId(), Quantity.fromValue(123));

		// Assert:
		for (final Map.Entry<String, MosaicTransferPair> entry : DESC_TO_PAIR_MAP.entrySet()) {
			Assert.assertThat(
					entry.getValue(),
					isDiffExpected(entry.getKey()) ? IsNot.not(IsEqual.equalTo(pair)) : IsEqual.equalTo(pair));
		}
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = new MosaicTransferPair(createMosaicId(), Quantity.fromValue(123)).hashCode();

		// Assert:
		for (final Map.Entry<String, MosaicTransferPair> entry : DESC_TO_PAIR_MAP.entrySet()) {
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
