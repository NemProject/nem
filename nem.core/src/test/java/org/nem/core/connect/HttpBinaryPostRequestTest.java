package org.nem.core.connect;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.test.MockSerializableEntity;

public class HttpBinaryPostRequestTest {

	@Test
	public void serializableEntityPayloadIsCorrect() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(7, "a", 3);

		// Act:
		final HttpPostRequest request = new HttpBinaryPostRequest(entity);

		// Assert:
		Assert.assertThat(
				request.getPayload(),
				IsEqual.equalTo(BinarySerializer.serializeToBytes(entity)));
	}

	@Test
	public void contentTypeIsApplicationBinary() {
		// Arrange:
		final MockSerializableEntity entity = new MockSerializableEntity(7, "a", 3);

		// Act:
		final HttpPostRequest request = new HttpBinaryPostRequest(entity);

		// Assert:
		Assert.assertThat(
				request.getContentType(),
				IsEqual.equalTo("application/binary"));
	}
}