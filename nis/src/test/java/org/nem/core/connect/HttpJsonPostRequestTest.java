package org.nem.core.connect;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.MockSerializableEntity;

import java.io.UnsupportedEncodingException;

public class HttpJsonPostRequestTest {

	@Test
	public void serializableEntityPayloadIsCorrect() throws UnsupportedEncodingException {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(7, "a", 3);

		// Act:
		final HttpPostRequest request = new HttpJsonPostRequest(entity);

		// Assert:
		Assert.assertThat(
				request.getPayload(),
				IsEqual.equalTo(JsonSerializer.serializeToBytes(entity)));
	}

	@Test
	public void jsonObjectPayloadIsCorrect() throws UnsupportedEncodingException {
		// Arrange:
		final JSONObject jsonEntity = new JSONObject();
		jsonEntity.put("a", 7);
		jsonEntity.put("b", "a");

		// Act:
		final HttpPostRequest request = new HttpJsonPostRequest(jsonEntity);

		// Assert:
		Assert.assertThat(
				request.getPayload(),
				IsEqual.equalTo(jsonEntity.toString().getBytes("UTF-8")));
	}

	@Test
	public void contentTypeIsApplicationJson() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(7, "a", 3);

		// Act:
		final HttpPostRequest request = new HttpJsonPostRequest(entity);

		// Assert:
		Assert.assertThat(
				request.getContentType(),
				IsEqual.equalTo("application/json"));
	}
}