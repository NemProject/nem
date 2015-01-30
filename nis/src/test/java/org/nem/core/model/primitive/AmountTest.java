package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.util.function.BiFunction;

public class AmountTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(Amount.ZERO, IsEqual.equalTo(new Amount(0)));
	}

	//endregion

	//region factories

	@Test
	public void canCreateAmountFromNem() {
		// Act:
		final Amount amount = Amount.fromNem(11);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(11000000L));
		Assert.assertThat(amount.getNumNem(), IsEqual.equalTo(11L));
	}

	@Test
	public void canCreateAmountFromMicroNem() {
		// Act:
		final Amount amount = Amount.fromMicroNem(11);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(11L));
		Assert.assertThat(amount.getNumNem(), IsEqual.equalTo(0L));
	}

	//endregion

	//region constructor

	@Test(expected = NegativeBalanceException.class)
	public void cannotBeCreatedAroundNegativeAmount() {
		// Act:
		new Amount(-1);
	}

	@Test
	public void canBeCreatedAroundZeroAmount() {
		// Act:
		final Amount amount = new Amount(0);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveAmount() {
		// Act:
		final Amount amount = new Amount(1);

		// Assert:
		Assert.assertThat(amount.getNumMicroNem(), IsEqual.equalTo(1L));
	}

	//endregion

	//region getNumNem

	@Test
	public void getNumNemRoundsDownToTheNearestWholeNem() {
		// Assert:
		Assert.assertThat(Amount.fromMicroNem(11000000L).getNumNem(), IsEqual.equalTo(11L));
		Assert.assertThat(Amount.fromMicroNem(11000001L).getNumNem(), IsEqual.equalTo(11L));
		Assert.assertThat(Amount.fromMicroNem(11999999L).getNumNem(), IsEqual.equalTo(11L));
	}

	//endregion

	//region add / subtract

	@Test
	public void twoAmountsCanBeAddedTogether() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		final Amount result1 = amount1.add(amount2);
		final Amount result2 = amount2.add(amount1);

		// Assert:
		Assert.assertThat(result1, IsEqual.equalTo(new Amount(176)));
		Assert.assertThat(result2, IsEqual.equalTo(new Amount(176)));
	}

	@Test
	public void smallerAmountCanBeSubtractedFromLargerAmount() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		final Amount result = amount2.subtract(amount1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Amount(46)));
	}

	@Test(expected = NegativeBalanceException.class)
	public void largerAmountCannotBeSubtractedFromSmallerAmount() {
		// Arrange:
		final Amount amount1 = new Amount(65);
		final Amount amount2 = new Amount(111);

		// Act:
		amount1.subtract(amount2);
	}

	//endregion

	//region multiply

	@Test
	public void amountCanBeMultipliedByScalar() {
		// Arrange:
		final Amount amount = new Amount(65);

		// Act:
		final Amount result = amount.multiply(3);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new Amount(195)));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAmount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Amount amount = new Amount(0x7712411223456L);

		// Act:
		Amount.writeTo(serializer, "Amount", amount);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Amount"), IsEqual.equalTo(0x7712411223456L));
	}

	@Test
	public void canRoundtripAmount() {
		// Assert:
		assertCanRoundtripAmount(Amount::readFrom);
	}

	@Test
	public void canRoundtripAmountUsingReadFromOptional() {
		// Assert:
		assertCanRoundtripAmount(Amount::readFromOptional);
	}

	private static void assertCanRoundtripAmount(final BiFunction<Deserializer, String, Amount> readFrom) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Amount originalAmount = new Amount(0x7712411223456L);

		// Act:
		Amount.writeTo(serializer, "Amount", originalAmount);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Amount amount = readFrom.apply(deserializer, "Amount");

		// Assert:
		Assert.assertThat(amount, IsEqual.equalTo(originalAmount));
	}

	@Test
	public void canReadNullUsingReadFromOptional() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Amount amount = Amount.readFromOptional(deserializer, "Amount");

		// Assert:
		Assert.assertThat(amount, IsNull.nullValue());
	}

	//endregion
}
