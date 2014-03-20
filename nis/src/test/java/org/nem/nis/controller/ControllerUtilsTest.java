package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class ControllerUtilsTest {

    @Test
    public void serializeCreatesJsonStringTerminatingInNewline() {
        // Arrange:
        final MockSerializableEntity entity = new MockSerializableEntity(7, "foo", 3);

        // Act:
        final String jsonString = ControllerUtils.serialize(entity);

        // Assert:
        Assert.assertThat(jsonString.endsWith("\r\n"), IsEqual.equalTo(true));
    }

    @Test
    public void serializableEntityCanBeRoundTrippedAsJsonString() {
        // Arrange:
        final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
        final String jsonString = ControllerUtils.serialize(originalEntity);

        // Act:
        final Deserializer deserializer = ControllerUtils.getDeserializer(jsonString, null);
        final MockSerializableEntity entity = new MockSerializableEntity(deserializer);

        // Assert:
        CustomAsserts.assertMockSerializableEntity(entity, 7, "foo", 3);
    }

    @Test
    public void jsonDeserializerIsAssociatedWithAccountLookup() {
        // Arrange:
        final MockAccountLookup accountLookup = new MockAccountLookup();
        final MockSerializableEntity originalEntity = new MockSerializableEntity(7, "foo", 3);
        final String jsonString = ControllerUtils.serialize(originalEntity);

        // Act:
        final Deserializer deserializer = ControllerUtils.getDeserializer(jsonString, accountLookup);
        deserializer.getContext().findAccountByAddress(Address.fromEncoded("foo"));

        // Assert:
        Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
    }

    @Test(expected = InvalidParameterException.class)
    public void getDeserializerFailsIfInputStringIsNotJsonObject() {
        // Act:
        ControllerUtils.getDeserializer("7", null);
    }

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
