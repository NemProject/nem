package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class AccountStatusTest {

	//region construction

	@Test
	public void accountStatusCanBeCreatedFromCorrectStatusString() {
		// Arrange:
		final AccountStatus statusUnlocked = AccountStatus.fromString("UNLOCKED");
		final AccountStatus statusLocked = AccountStatus.fromString("LOCKED");

		// Assert:
		Assert.assertThat(statusUnlocked, IsEqual.equalTo(AccountStatus.UNLOCKED));
		Assert.assertThat(statusLocked, IsEqual.equalTo(AccountStatus.LOCKED));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountStatusCannotBeCreatedFromIncorrectStatusString() {
		// Arrange:
		final AccountStatus statusUnlocked = AccountStatus.fromString("TEST");
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAccountStatus() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final AccountStatus status = AccountStatus.fromString("UNLOCKED");

		// Act:
		AccountStatus.writeTo(serializer, "status", status);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("status"), IsEqual.equalTo("UNLOCKED"));
	}

	@Test
	public void canRoundtripAccountStatus() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final AccountStatus originalStatus = AccountStatus.fromString("UNLOCKED");

		// Act:
		AccountStatus.writeTo(serializer, "status", originalStatus);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final AccountStatus status = AccountStatus.readFrom(deserializer, "status");

		// Assert:
		Assert.assertThat(status, IsEqual.equalTo(originalStatus));
	}

	//endregion
}
