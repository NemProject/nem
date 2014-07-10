package org.nem.core.connect;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.io.IOException;

public class HttpJsonResponseStrategyTest {

	@Test
	public void coercedDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = coerceDeserializer(originalEntity, new MockAccountLookup());
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		Assert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void coercedDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = coerceDeserializer(originalEntity, accountLookup);
		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test(expected = FatalPeerException.class)
	public void coerceThrowsFatalPeerExceptionIfPeerReturnsUnexpectedDataWhenDeserializerIsExpected() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		coerceDeserializer(new byte[] { }, accountLookup);
	}

	private static Deserializer coerceDeserializer(
			final SerializableEntity originalEntity,
			final AccountLookup accountLookup) throws IOException {
		// Arrange:
		final byte[] serializedBytes = JsonSerializer.serializeToJson(originalEntity).toJSONString().getBytes();

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
