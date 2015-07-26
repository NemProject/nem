package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.util.function.BiFunction;

public class SupplyTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(Supply.ZERO, IsEqual.equalTo(new Supply(0)));
	}

	//endregion

	//region fromValue

	@Test
	public void canCreateSupplyFromValue() {
		// Act:
		final Supply supply = Supply.fromValue(11);

		// Assert:
		Assert.assertThat(supply.getRaw(), IsEqual.equalTo(11L));
	}

	//endregion

	//region constructor

	@Test
	public void cannotBeCreatedAroundNegativeSupply() {
		// Act:
		ExceptionAssert.assertThrows(v -> new Supply(-1), NegativeQuantityException.class);
		ExceptionAssert.assertThrows(v -> Supply.fromValue(-1), NegativeQuantityException.class);
	}

	@Test
	public void canBeCreatedAroundZeroSupply() {
		// Act:
		final Supply supply = new Supply(0);

		// Assert:
		Assert.assertThat(supply.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveSupply() {
		// Act:
		final Supply supply = new Supply(1);

		// Assert:
		Assert.assertThat(supply.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region add / subtract

	@Test
	public void twoQuantitiesCanBeAdded() {
		// Arrange:
		final Supply supply1 = new Supply(65);
		final Supply supply2 = new Supply(111);

		// Act:
		final Supply result1 = supply1.add(supply2);
		final Supply result2 = supply2.add(supply1);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(new Supply(176)));
		Assert.assertThat(result2, IsEqual.equalTo(new Supply(176)));
	}

	@Test
	public void smallerSupplyCanBeSubtractedFromLargerSupply() {
		// Arrange:
		final Supply supply1 = new Supply(65);
		final Supply supply2 = new Supply(111);

		// Act:
		final Supply result = supply2.subtract(supply1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Supply(46)));
	}

	@Test
	public void largerSupplyCannotBeSubtractedFromSmallerSupply() {
		// Arrange:
		final Supply supply1 = new Supply(65);
		final Supply supply2 = new Supply(111);

		// Act:
		ExceptionAssert.assertThrows(v -> supply1.subtract(supply2), NegativeQuantityException.class);
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteSupply() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Supply supply = new Supply(0x7712411223456L);

		// Act:
		Supply.writeTo(serializer, "supply", supply);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("supply"), IsEqual.equalTo(0x7712411223456L));
	}

	@Test
	public void canRoundTripSupply() {
		// Assert:
		assertCanRoundTripSupply(Supply::readFrom);
	}

	private static void assertCanRoundTripSupply(final BiFunction<Deserializer, String, Supply> readFrom) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Supply original = new Supply(0x7712411223456L);

		// Act:
		Supply.writeTo(serializer, "supply", original);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Supply supply = readFrom.apply(deserializer, "supply");

		// Assert:
		Assert.assertThat(supply, IsEqual.equalTo(original));
	}

	//endregion
}
