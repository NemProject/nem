package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class ControllerUtilsTest {

	@Test
	public void byteArrayCanBeDeserialized() throws Exception {
		// Arrange:
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		byte[] serializedBytes = BinarySerializer.serializeToBytes(originalEntity);

		// Act:
		try (final BinaryDeserializer deserializer = ControllerUtils.getDeserializer(serializedBytes, null)) {
			final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

			// Assert:
			CustomAsserts.assertMockSerializableEntity(entity, 7, "foo", 3);
		}
	}

	@Test
	public void binaryDeserializerIsAssociatedWithAccountLookup() throws Exception {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
		byte[] serializedBytes = BinarySerializer.serializeToBytes(originalEntity);

		// Act:
		try (final BinaryDeserializer deserializer = ControllerUtils.getDeserializer(serializedBytes, accountLookup)) {
			deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

			// Assert:
			Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
		}
	}
}
