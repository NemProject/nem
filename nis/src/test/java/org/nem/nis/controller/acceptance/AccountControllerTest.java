package org.nem.nis.controller.acceptance;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.nis.test.LocalHostConnector;

public class AccountControllerTest {

	private static final String ACCOUNT_UNLOCK_PATH = "account/unlock";

	@Test
	public void unlockSuccessReturnsOk() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();
		final PrivateKey key = Utils.generateRandomAccount().getKeyPair().getPrivateKey();
		final JsonSerializer serializer = new JsonSerializer();
		key.serialize(serializer);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(ACCOUNT_UNLOCK_PATH, serializer.getObject());

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(200));
		Assert.assertThat(result.hasBody(), IsEqual.equalTo(false));
	}
}
