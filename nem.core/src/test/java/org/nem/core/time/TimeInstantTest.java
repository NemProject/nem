package org.nem.core.time;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class TimeInstantTest {

	//region constants

	@Test
	public void constantsAreInitializedCorrectly() {
		// Assert:
		Assert.assertThat(TimeInstant.ZERO, IsEqual.equalTo(new TimeInstant(0)));
	}

	//endregion

	//region constructor

	@Test(expected = IllegalArgumentException.class)
	public void cannotBeCreatedAroundNegativeTime() {
		// Act:
		new TimeInstant(-1);
	}

	@Test
	public void canBeCreatedAroundZeroTime() {
		// Act:
		final TimeInstant instant = new TimeInstant(0);

		// Assert:
		Assert.assertThat(instant.getRawTime(), IsEqual.equalTo(0));
	}

	@Test
	public void canBeCreatedAroundPositiveTime() {
		// Act:
		final TimeInstant instant = new TimeInstant(1);

		// Assert:
		Assert.assertThat(instant.getRawTime(), IsEqual.equalTo(1));
	}

	//endregion

	//region addX

	@Test
	public void addSecondsCreatesNewInstant() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);

		// Act:
		final TimeInstant instant2 = instant1.addSeconds(2);

		// Assert:
		Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(9));
	}

	@Test
	public void addMinutesCreatesNewInstant() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);

		// Act:
		final TimeInstant instant2 = instant1.addMinutes(3);

		// Assert:
		Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(187));
	}

	@Test
	public void addHoursCreatesNewInstant() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);

		// Act:
		final TimeInstant instant2 = instant1.addHours(4);

		// Assert:
		Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(4 * 60 * 60 + 7));
	}

	@Test
	public void addDaysCreatesNewInstant() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);

		// Act:
		final TimeInstant instant2 = instant1.addDays(5);

		// Assert:
		Assert.assertThat(instant2.getRawTime(), IsEqual.equalTo(5 * 24 * 60 * 60 + 7));
	}

	//endregion

	//region subtract

	@Test
	public void subtractCanSubtractEqualInstances() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);
		final TimeInstant instant2 = new TimeInstant(7);

		// Assert:
		Assert.assertThat(instant1.subtract(instant2), IsEqual.equalTo(0));
		Assert.assertThat(instant2.subtract(instant1), IsEqual.equalTo(0));
	}

	@Test
	public void subtractCanSubtractUnequalInstances() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);
		final TimeInstant instant2 = new TimeInstant(11);

		// Assert:
		Assert.assertThat(instant1.subtract(instant2), IsEqual.equalTo(-4));
		Assert.assertThat(instant2.subtract(instant1), IsEqual.equalTo(4));
	}

	//endregion

	//region compareTo

	@Test
	public void compareToCanCompareEqualInstances() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);
		final TimeInstant instant2 = new TimeInstant(7);

		// Assert:
		Assert.assertThat(instant1.compareTo(instant2), IsEqual.equalTo(0));
		Assert.assertThat(instant2.compareTo(instant1), IsEqual.equalTo(0));
	}

	@Test
	public void compareToCanCompareUnequalInstances() {
		// Arrange:
		final TimeInstant instant1 = new TimeInstant(7);
		final TimeInstant instant2 = new TimeInstant(8);

		// Assert:
		Assert.assertThat(instant1.compareTo(instant2), IsEqual.equalTo(-1));
		Assert.assertThat(instant2.compareTo(instant1), IsEqual.equalTo(1));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final TimeInstant instant = new TimeInstant(7);

		// Assert:
		Assert.assertThat(new TimeInstant(7), IsEqual.equalTo(instant));
		Assert.assertThat(new TimeInstant(6), IsNot.not(IsEqual.equalTo(instant)));
		Assert.assertThat(new TimeInstant(8), IsNot.not(IsEqual.equalTo(instant)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(instant)));
		Assert.assertThat(7, IsNot.not(IsEqual.equalTo((Object)instant)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final TimeInstant instant = new TimeInstant(7);
		final int hashCode = instant.hashCode();

		// Assert:
		Assert.assertThat(new TimeInstant(7).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new TimeInstant(6).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new TimeInstant(8).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsRawTime() {
		// Arrange:
		final TimeInstant instant = new TimeInstant(22561);

		// Assert:
		Assert.assertThat(instant.toString(), IsEqual.equalTo("22561"));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteTimeInstant() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TimeInstant instant = new TimeInstant(77124);

		// Act:
		TimeInstant.writeTo(serializer, "TimeInstant", instant);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("TimeInstant"), IsEqual.equalTo(77124));
	}

	@Test
	public void canRoundtripTimeInstant() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TimeInstant originalInstant = new TimeInstant(77124);

		// Act:
		TimeInstant.writeTo(serializer, "TimeInstant", originalInstant);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final TimeInstant instant = TimeInstant.readFrom(deserializer, "TimeInstant");

		// Assert:
		Assert.assertThat(instant, IsEqual.equalTo(originalInstant));
	}

	//endregion
}
