package org.nem.core.serialization;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;

/**
 * Notice that this the write* tests are validating that the values were actually written to the
 * underlying serializer (JSON in this case) and are thus dependent on the storage details of the
 * JsonSerializer. Although the tests have this extra dependency, they give us a way to validate
 * that a single property value is written for each object.
 */
public class SerializationUtilsTest {

    //region Write

    @Test
    public void canWriteAddress() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();
        Address address = Address.fromEncoded("MockAcc");

        // Act:
        SerializationUtils.writeAddress(serializer, "Address", address);

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.size(), IsEqual.equalTo(1));
        Assert.assertThat((String)object.get("Address"), IsEqual.equalTo(address.getEncoded()));
    }

    @Test
    public void canWriteAccount() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();
        Address address = Address.fromEncoded("MockAcc");

        // Act:
        SerializationUtils.writeAccount(serializer, "Account", new MockAccount(address));

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.size(), IsEqual.equalTo(1));
        Assert.assertThat((String)object.get("Account"), IsEqual.equalTo(address.getEncoded()));
    }

    @Test
    public void canWriteSignature() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();
        final Signature signature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

        // Act:
        SerializationUtils.writeSignature(serializer, "Signature", signature);

        // Assert:
        final JSONObject object = serializer.getObject();
        Assert.assertThat(object.size(), IsEqual.equalTo(1));
        Assert.assertThat((String)object.get("Signature"), IsEqual.equalTo(Base64Encoder.getString(signature.getBytes())));
    }

    //endregion

    //region Roundtrip

    @Test
    public void canRoundtripAddress() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();

        // Act:
        SerializationUtils.writeAddress(serializer, "Address", Address.fromEncoded("MockAcc"));

        JsonDeserializer deserializer = createDeserializer(serializer.getObject());
        final Address address = SerializationUtils.readAddress(deserializer, "Address");

        // Assert:
        Assert.assertThat(address, IsEqual.equalTo(address));
    }

    @Test
    public void canRoundtripAccount() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();
        Address address = Address.fromEncoded("MockAcc");
        MockAccountLookup accountLookup = new MockAccountLookup();

        // Act:
        SerializationUtils.writeAccount(serializer, "Account", new MockAccount(address));

        JsonDeserializer deserializer = new JsonDeserializer(
            serializer.getObject(),
            new DeserializationContext(accountLookup));
        final Account account = SerializationUtils.readAccount(deserializer, "Account");

        // Assert:
        Assert.assertThat(account.getAddress(), IsEqual.equalTo(address));
        Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
    }

    @Test
    public void canRoundtripSignature() {
        // Arrange:
        JsonSerializer serializer = new JsonSerializer();
        final Signature originalSignature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

        // Act:
        SerializationUtils.writeSignature(serializer, "Signature", originalSignature);

        JsonDeserializer deserializer = createDeserializer(serializer.getObject());
        final Signature signature = SerializationUtils.readSignature(deserializer, "Signature");

        // Assert:
        Assert.assertThat(signature, IsEqual.equalTo(originalSignature));
    }

    //endregion

    private JsonDeserializer createDeserializer(final JSONObject object) {
        return new JsonDeserializer(
            object,
            new DeserializationContext(new MockAccountLookup()));
    }
}
