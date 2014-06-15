package org.nem.nis.controller.acceptance;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.nis.test.LocalHostConnector;

public class PushControllerTest {

	private static final String PUSH_TRANSACTION_PATH = "push/transaction";

	@Test
	public void transferIncorrectPush() {
		// Arrange:
		final LocalHostConnector connector = new LocalHostConnector();

		final JSONObject obj = new JSONObject();
		obj.put("type", 123456);

		// Act:
		final ErrorResponseDeserializerUnion result = connector.post(PUSH_TRANSACTION_PATH, obj);

		// Assert:
		Assert.assertThat(result.getStatus(), IsEqual.equalTo(400));
	}
}
