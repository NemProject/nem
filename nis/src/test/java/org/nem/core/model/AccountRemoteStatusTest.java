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
		// TODO 20141005 minor, you might want to have a map with these values and loop over them or loop over AccountRemoteStatus.values()
		// > (just to ensure all the tests will always test all the values)
		assertCanCreate("REMOTE", AccountRemoteStatus.REMOTE);
        assertCanCreate("INACTIVE", AccountRemoteStatus.INACTIVE);
        assertCanCreate("ACTIVATED", AccountRemoteStatus.ACTIVATED);
        assertCanCreate("ACTIVE", AccountRemoteStatus.ACTIVE);
        assertCanCreate("DEACTIVATED", AccountRemoteStatus.DEACTIVATED);
    }

    private static void assertCanCreate(final String statusString, final AccountRemoteStatus accountRemoteStatus) {
        // Arrange:
        final AccountRemoteStatus status =  AccountRemoteStatus.fromString(statusString);

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
        assertCanWrite("REMOTE");
        assertCanWrite("INACTIVE");
        assertCanWrite("ACTIVATED");
        assertCanWrite("ACTIVE");
        assertCanWrite("DEACTIVATED");
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
        assertCanRoundtrip("REMOTE");
        assertCanRoundtrip("INACTIVE");
        assertCanRoundtrip("ACTIVATED");
        assertCanRoundtrip("ACTIVE");
        assertCanRoundtrip("DEACTIVATED");
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
