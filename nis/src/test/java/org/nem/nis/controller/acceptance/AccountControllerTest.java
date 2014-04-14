package org.nem.nis.controller.acceptance;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.nis.test.MockPeerConnector;

import java.net.MalformedURLException;

public class AccountControllerTest {

	@Test
	public void unlockSuccessReturnsOk() throws MalformedURLException {

		// TODO: for this to pass, HttpMethodClient needs to be updated to handle empty response bodies

		// Arrange:
		final MockPeerConnector pc = new MockPeerConnector();
		final PrivateKey key = Utils.generateRandomAccount().getKeyPair().getPrivateKey();
		final JsonSerializer serializer = new JsonSerializer();
		key.serialize(serializer);

		// Act:
		final Deserializer deserializer = pc.accountUnlock(serializer.getObject());

		// Assert:
		Assert.assertThat(deserializer.readInt("status"), IsEqual.equalTo(null));
	}
}
