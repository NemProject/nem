package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

public class AccountRemoteStatusTest {
	//region construction

	@Test
	public void accountRemoteStatusCanBeCreatedFromCorrectStatusString() {
		for (final AccountRemoteStatus accountRemoteStatus : AccountRemoteStatus.values())
		{
			assertCanCreate(accountRemoteStatus.toString(), accountRemoteStatus);
		}
	}

	private static void assertCanCreate(final String statusString, final AccountRemoteStatus accountRemoteStatus) {
		// Arrange:
		final AccountRemoteStatus status = AccountRemoteStatus.fromString(statusString);

		// Assert:
		Assert.assertThat(status, IsEqual.equalTo(accountRemoteStatus));
	}

	@Test(expected = IllegalArgumentException.class)
	public void accountRemoteStatusCannotBeCreatedFromIncorrectStatusString() {
		// Arrange:
		AccountRemoteStatus.fromString("TEST");
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAccountStatus() {
		for (final AccountRemoteStatus accountRemoteStatus : AccountRemoteStatus.values())
		{
			assertCanWrite(accountRemoteStatus.toString());
		}
	}

	private static void assertCanWrite(final String statusString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final AccountRemoteStatus status = AccountRemoteStatus.fromString(statusString);

		// Act:
		AccountRemoteStatus.writeTo(serializer, "status", status);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("status"), IsEqual.equalTo(statusString));
	}

	@Test
	public void canRoundtripAccountStatus() {
		for (final AccountRemoteStatus accountRemoteStatus : AccountRemoteStatus.values())
		{
			assertCanRoundtrip(accountRemoteStatus.toString());
		}
	}

	private static void assertCanRoundtrip(final String statusString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final AccountRemoteStatus originalStatus = AccountRemoteStatus.fromString(statusString);

		// Act:
		AccountRemoteStatus.writeTo(serializer, "status", originalStatus);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final AccountRemoteStatus status = AccountRemoteStatus.readFrom(deserializer, "status");

		// Assert:
		Assert.assertThat(status, IsEqual.equalTo(originalStatus));
	}
	//endregion
}
