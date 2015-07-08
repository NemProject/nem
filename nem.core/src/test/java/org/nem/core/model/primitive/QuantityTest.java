package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.BiFunction;

public class QuantityTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(Quantity.ZERO, IsEqual.equalTo(new Quantity(0)));
	}

	//endregion

	//region fromValue

	@Test
	public void canCreateQuantityFromValue() {
		// Act:
		final Quantity quantity = Quantity.fromValue(11);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(11L));
	}

	//endregion

	//region constructor

	public void cannotBeCreatedAroundNegativeQuantity() {
		// Act:
		ExceptionAssert.assertThrows(v -> new Quantity(-1), NegativeQuantityException.class);
		ExceptionAssert.assertThrows(v -> Quantity.fromValue(-1), NegativeQuantityException.class);
	}

	@Test
	public void canBeCreatedAroundZeroQuantity() {
		// Act:
		final Quantity quantity = new Quantity(0);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveQuantity() {
		// Act:
		final Quantity quantity = new Quantity(1);

		// Assert:
		Assert.assertThat(quantity.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region add / subtract

	@Test
	public void twoQuantitiesCanBeAdded() {
		// Arrange:
		final Quantity quantity1 = new Quantity(65);
		final Quantity quantity2 = new Quantity(111);

		// Act:
		final Quantity result1 = quantity1.add(quantity2);
		final Quantity result2 = quantity2.add(quantity1);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(new Quantity(176)));
		Assert.assertThat(result2, IsEqual.equalTo(new Quantity(176)));
	}

	@Test
	public void smallerQuantityCanBeSubtractedFromLargerQuantity() {
		// Arrange:
		final Quantity quantity1 = new Quantity(65);
		final Quantity quantity2 = new Quantity(111);

		// Act:
		final Quantity result = quantity2.subtract(quantity1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Quantity(46)));
	}

	@Test
	public void largerQuantityCannotBeSubtractedFromSmallerQuantity() {
		// Arrange:
		final Quantity quantity1 = new Quantity(65);
		final Quantity quantity2 = new Quantity(111);

		// Act:
		ExceptionAssert.assertThrows(v -> quantity1.subtract(quantity2), NegativeQuantityException.class);
	}

	//endregion

	//region multiply

	@Test
	public void QuantityCanBeMultipliedByScalar() {
		// Arrange:
		final Quantity quantity = new Quantity(65);

		// Act:
		final Quantity result = quantity.multiply(3);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Quantity(195)));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteQuantity() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Quantity quantity = new Quantity(0x7712411223456L);

		// Act:
		Quantity.writeTo(serializer, "quantity", quantity);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("quantity"), IsEqual.equalTo(0x7712411223456L));
	}

	@Test
	public void canRoundTripQuantity() {
		// Assert:
		assertCanRoundTripQuantity(Quantity::readFrom);
	}

	private static void assertCanRoundTripQuantity(final BiFunction<Deserializer, String, Quantity> readFrom) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Quantity original = new Quantity(0x7712411223456L);

		// Act:
		Quantity.writeTo(serializer, "quantity", original);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Quantity quantity = readFrom.apply(deserializer, "quantity");

		// Assert:
		Assert.assertThat(quantity, IsEqual.equalTo(original));
	}

	//endregion
}
