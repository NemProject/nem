package org.nem.core.serialization;

import org.hamcrest.core.*;
import org.json.JSONObject;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.test.*;

import java.math.BigInteger;

/**
 * Notice that this the write* tests are validating that the values were actually written to the
 * underlying serializer (JSON in this case) and are thus dependent on the storage details of the
 * JsonSerializer. Although the tests have this extra dependency, they give us a way to validate
 * that a single property value is written for each object.
 */
public class DelegatingObjectSerializerTest {

    //region Write

    @Test
    public void canWriteInt() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeInt("int", 0x09513510);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getInt("int"), IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canWriteLong() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeLong("long", 0xF239A033CE951350L);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getLong("long"), IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canWriteBigInteger() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeBigInteger("BigInteger", new BigInteger("958A7561F014", 16));

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("BigInteger"), IsEqual.equalTo("AJWKdWHwFA=="));
    }

    @Test
    public void canWriteBytes() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21, 0x5A };
        serializer.writeBytes("bytes", bytes);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("bytes"), IsEqual.equalTo("UP8AfCFa"));
    }

    @Test
    public void canWriteString() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeString("String", "BEta");

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("String"), IsEqual.equalTo("BEta"));
    }

    @Test
    public void canWriteAddress() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        Address address = Address.fromEncoded("MockAcc");

        // Act:
        serializer.writeAddress("Address", address);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("Address"), IsEqual.equalTo(address.getEncoded()));
    }

    @Test
    public void canWriteAccount() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        Address address = Address.fromEncoded("MockAcc");

        // Act:
        serializer.writeAccount("Account", new MockAccount(address));

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("Account"), IsEqual.equalTo(address.getEncoded()));
    }

    @Test
    public void canWriteSignature() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final Signature signature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));
        serializer.writeSignature("Signature", signature);

        // Assert:
        final JSONObject object = jsonSerializer.getObject();
        Assert.assertThat(object.length(), IsEqual.equalTo(1));
        Assert.assertThat(object.getString("Signature"), IsEqual.equalTo("AQAAAHoDAAAAAKTw"));
    }

    //endregion

    //region Roundtrip

    @Test
    public void canRoundtripInt() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeInt("int", 0x09513510);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final int i = deserializer.readInt("int");

        // Assert:
        Assert.assertThat(i, IsEqual.equalTo(0x09513510));
    }

    @Test
    public void canRoundtripLong() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeLong("long", 0xF239A033CE951350L);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final long l = deserializer.readLong("long");

        // Assert:
        Assert.assertThat(l, IsEqual.equalTo(0xF239A033CE951350L));
    }

    @Test
    public void canRoundtripBigInteger() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final BigInteger i = new BigInteger("958A7561F014", 16);
        serializer.writeBigInteger("BigInteger", i);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final BigInteger readBigInteger = deserializer.readBigInteger("BigInteger");

        // Assert:
        Assert.assertThat(readBigInteger, IsEqual.equalTo(i));
    }

    @Test
    public void canRoundtripBytes() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final byte[] bytes = new byte[] { 0x50, (byte)0xFF, 0x00, 0x7C, 0x21 };
        serializer.writeBytes("bytes", bytes);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final byte[] readBytes = deserializer.readBytes("bytes");

        // Assert:
        Assert.assertThat(readBytes, IsEqual.equalTo(bytes));
    }

    @Test
    public void canRoundtripString() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeString("String", "BEta GaMMa");

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final String s = deserializer.readString("String");

        // Assert:
        Assert.assertThat(s, IsEqual.equalTo("BEta GaMMa"));
    }

    @Test
    public void canRoundtripAddress() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        serializer.writeAddress("Address", Address.fromEncoded("MockAcc"));

        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            new MockAccountLookup());
        final Address address = deserializer.readAddress("Address");

        // Assert:
        Assert.assertThat(address.getEncoded(), IsEqual.equalTo(address.getEncoded()));
    }

    @Test
    public void canRoundtripAccount() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        MockAccountLookup accountLookup = new MockAccountLookup();
        Address address = Address.fromEncoded("MockAcc");

        // Act:
        serializer.writeAccount("Account", new MockAccount(address));

        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            accountLookup);
        final Account account = deserializer.readAccount("Account");

        // Assert:
        Assert.assertThat(account.getAddress().getEncoded(), IsEqual.equalTo(address.getEncoded()));
        Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
    }

    @Test
    public void canRoundtripSignature() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);

        // Act:
        final Signature signature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));
        serializer.writeSignature("Signature", signature);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());
        final Signature readSignature = deserializer.readSignature("Signature");

        // Assert:
        Assert.assertThat(readSignature, IsEqual.equalTo(signature));
    }

    //endregion

    @Test
    public void canRoundtripMultipleValues() {
        // Arrange:
        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        Address address = Address.fromEncoded("Beta");

        // Act:
        serializer.writeInt("alpha", 0x09513510);
        serializer.writeLong("zeta", 0xF239A033CE951350L);
        serializer.writeAccount("nu", new MockAccount(address));
        serializer.writeBytes("beta", new byte[]{2, 4, 6});
        serializer.writeInt("gamma", 7);
        serializer.writeString("epsilon", "FooBar");
        serializer.writeLong("sigma", 8);

        ObjectDeserializer deserializer = createObjectDeserializer(jsonSerializer.getObject());

        // Assert:
        Assert.assertThat(deserializer.readInt("alpha"), IsEqual.equalTo(0x09513510));
        Assert.assertThat(deserializer.readLong("zeta"), IsEqual.equalTo(0xF239A033CE951350L));
        Assert.assertThat(deserializer.readAccount("nu").getAddress().getEncoded(), IsEqual.equalTo(address.getEncoded()));
        Assert.assertThat(deserializer.readBytes("beta"), IsEqual.equalTo(new byte[] { 2, 4, 6 }));
        Assert.assertThat(deserializer.readInt("gamma"), IsEqual.equalTo(7));
        Assert.assertThat(deserializer.readString("epsilon"), IsEqual.equalTo("FooBar"));
        Assert.assertThat(deserializer.readLong("sigma"), IsEqual.equalTo(8L));
    }

    private ObjectDeserializer createObjectDeserializer(final JSONObject object) {
        return new DelegatingObjectDeserializer(new JsonDeserializer(object), new MockAccountLookup());
    }
}
