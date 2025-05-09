package org.nem.core.connect;

import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

/**
 * Base class for tests of classes deriving from HttpDeserializerResponseStrategy that deserialize entities.
 */
public abstract class HttpDeserializerResponseStrategyContractTest {

	@Test
	public void coercedDeserializerIsCorrectlyCreatedAroundInput() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = this.coerceDeserializer(originalEntity, new MockAccountLookup());
		final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

		// Assert:
		MatcherAssert.assertThat(entity, IsEqual.equalTo(originalEntity));
	}

	@Test
	public void coercedDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);

		// Act:
		final Deserializer deserializer = this.coerceDeserializer(originalEntity, accountLookup);
		deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

		// Assert:
		MatcherAssert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	protected abstract Deserializer coerceDeserializer(final SerializableEntity originalEntity, final AccountLookup accountLookup)
			throws IOException;
}
