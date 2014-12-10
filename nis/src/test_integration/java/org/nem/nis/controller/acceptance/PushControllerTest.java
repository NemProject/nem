package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.node.NodeIdentity;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.MockTransaction;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.test.LocalHostConnector;

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
		jsonIdentity.put("public-key", identity.getAddress().getPublicKey().toString());
		jsonIdentity.put("name", "test");

		final Signature signature = identity.sign(HashUtils.calculateHash(transaction).getRaw());

		final JSONObject obj = new JSONObject();
		obj.put("entity", jsonEntity);
		obj.put("signature", HexEncoder.getString(signature.getBytes()));
		obj.put("identity", jsonIdentity);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(PUSH_TRANSACTION_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}
}
