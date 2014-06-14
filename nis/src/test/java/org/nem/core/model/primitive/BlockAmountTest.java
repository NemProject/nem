package org.nem.core.model.primitive;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class BlockAmountTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(BlockAmount.ZERO, IsEqual.equalTo(new BlockAmount(0)));
	}

	//endregion

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeAmount() {
		// Act:
		new BlockAmount(-1);
	}

	@Test
	public void canBeCreatedAroundZeroAmount() {
		// Act:
		final BlockAmount amount = new BlockAmount(0);

		// Assert:
		Assert.assertThat(amount.getRaw(), IsEqual.equalTo(0L));
	}

	@Test
	public void canBeCreatedAroundPositiveAmount() {
		// Act:
		final BlockAmount amount = new BlockAmount(1);

		// Assert:
		Assert.assertThat(amount.getRaw(), IsEqual.equalTo(1L));
	}

	//endregion

	//region inc/dec

	@Test
	public void incrementChangesAmountByOne() {
		// Arrange:
		final BlockAmount amount = new BlockAmount(0x1233L);

		// Act:
		final BlockAmount result = amount.increment();

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(amount)));
		Assert.assertThat(result, IsEqual.equalTo(new BlockAmount(0x1234L)));
	}

	@Test
	public void decrementChangesAmountByOne() {
		// Arrange:
		final BlockAmount amount = new BlockAmount(0x1235L);

		// Act:
		final BlockAmount result = amount.decrement();

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(amount)));
		Assert.assertThat(result, IsEqual.equalTo(new BlockAmount(0x1234L)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void decrementZeroAmountThrowsException() {
		// Arrange:
		final BlockAmount amount = BlockAmount.ZERO;

		// Act:
		amount.decrement();
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteBlockAmount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockAmount amount = new BlockAmount(0x8712411223456L);

		// Act:
		BlockAmount.writeTo(serializer, "xyzAmount", amount);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("xyzAmount"), IsEqual.equalTo(0x8712411223456L));
	}

	@Test
	public void canRoundtripBlockAmount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final BlockAmount originalAmount = new BlockAmount(0x8712411223456L);

		// Act:
		BlockAmount.writeTo(serializer, "xyzAmount", originalAmount);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final BlockAmount amount = BlockAmount.readFrom(deserializer, "xyzAmount");

		// Assert:
		Assert.assertThat(amount, IsEqual.equalTo(originalAmount));
	}

	//endregion
}