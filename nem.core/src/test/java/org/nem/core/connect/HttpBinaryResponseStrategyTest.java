package org.nem.core.connect;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.ConnectUtils;

import java.io.IOException;

public class HttpBinaryResponseStrategyTest extends HttpDeserializerResponseStrategyContractTest {

	@Test
	public void getSupportedContentTypeReturnsCorrectContentType() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpBinaryResponseStrategy(null);

		// Assert:
		Assert.assertThat(strategy.getSupportedContentType(), IsEqual.equalTo("application/binary"));
	}

	@Override
	protected Deserializer coerceDeserializer(
			final SerializableEntity originalEntity,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final byte[] serializedBytes = BinarySerializer.serializeToBytes(originalEntity);

		// Act:
		return coerceDeserializer(serializedBytes, accountLookup);
	}

	private static Deserializer coerceDeserializer(
			final byte[] serializedBytes,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final HttpDeserializerResponseStrategy strategy = new HttpBinaryResponseStrategy(context);

		// Act:
		return ConnectUtils.coerceDeserializer(serializedBytes, strategy);
	}
}