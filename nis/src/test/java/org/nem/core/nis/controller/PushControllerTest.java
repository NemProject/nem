package org.nem.core.nis.controller;

import net.minidev.json.JSONObject;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.serialization.JsonDeserializer;
import org.nem.core.test.MockPeerConnector;

import java.net.MalformedURLException;

public class PushControllerTest {

	@Test
	public void transferIncorrectPush() throws MalformedURLException {
		// Arrange:
		MockPeerConnector pc = new MockPeerConnector();

		JSONObject obj = new JSONObject();
		obj.put("type", 123456);

		// Act:
		JsonDeserializer res = pc.pushTransaction(obj);

		// Assert:
		Assert.assertThat(res.readInt("error"), IsNull.notNullValue());
	}

}
