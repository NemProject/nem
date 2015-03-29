package org.nem.core.connect;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.io.IOException;

public class HttpJsonResponseStrategyTest extends HttpDeserializerResponseStrategyContractTest {

	@Test
	public void getSupportedContentTypeReturnsCorrectContentType() throws Exception {
		// Arrange:
		final HttpDeserializerResponseStrategy strategy = new HttpJsonResponseStrategy(null);

		// Assert:
		Assert.assertThat(strategy.getSupportedContentType(), IsEqual.equalTo("application/json"));
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfPeerReturnsUnexpectedDataWhenDeserializerIsExpected() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		coerceDeserializer(new byte[] {}, accountLookup);
	}

	@Override
	protected Deserializer coerceDeserializer(
			final SerializableEntity originalEntity,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final byte[] serializedBytes = JsonSerializer.serializeToBytes(originalEntity);

		// Act:
		return coerceDeserializer(serializedBytes, accountLookup);
	}

	private static Deserializer coerceDeserializer(
			final byte[] serializedBytes,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final DeserializationContext context = new DeserializationContext(accountLookup);
		final HttpDeserializerResponseStrategy strategy = new HttpJsonResponseStrategy(context);

		// Act:
		return ConnectUtils.coerceDeserializer(serializedBytes, strategy);
	}
}
