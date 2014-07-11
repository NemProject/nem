package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.MockTransaction;
import org.nem.core.utils.Base64Encoder;
import org.nem.nis.test.LocalHostConnector;
import org.nem.peer.node.NodeIdentity;

public class PushControllerTest {

	private static final String PUSH_TRANSACTION_PATH = "push/transaction";

	@Test
	// TODO: this test is failing now
	public void transferIncorrectPush() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final Transaction transaction = new MockTransaction();
		transaction.sign();
		final JSONObject jsonEntity = JsonSerializer.serializeToJson(transaction);
		jsonEntity.put("type", 123456);

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final JSONObject jsonIdentity = new JSONObject();
		jsonIdentity.put("public-key", Base64Encoder.getString(identity.getKeyPair().getPublicKey().getRaw()));
		jsonIdentity.put("name", "test");

		final Signature signature = identity.sign(HashUtils.calculateHash(transaction).getRaw());

		final JSONObject obj = new JSONObject();
		obj.put("entity", jsonEntity);
		obj.put("signature", Base64Encoder.getString(signature.getBytes()));
		obj.put("identity", jsonIdentity);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(PUSH_TRANSACTION_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}
}
