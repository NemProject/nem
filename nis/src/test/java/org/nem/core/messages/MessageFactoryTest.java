package org.nem.core.messages;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class MessageFactoryTest {

    @Test(expected = InvalidParameterException.class)
    public void cannotDeserializeUnknownMessage() {
        // Arrange:
        final JSONObject object = new JSONObject();
        object.put("type", 7);
        final JsonDeserializer deserializer = new JsonDeserializer(object, null);

        // Act:
        MessageFactory.deserialize(null, null, deserializer);
    }

    @Test
    public void canDeserializePlainMessage() {
        // Arrange:
        final PlainMessage originalMessage = new PlainMessage(new byte[] { 1, 2, 4 });
        final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, new MockAccountLookup());

        // Act:
        final Message message = MessageFactory.deserialize(null, null, deserializer);

        // Assert:
        Assert.assertThat(message, IsInstanceOf.instanceOf(PlainMessage.class));
        Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
        Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 4 }));
    }

    @Test
    public void canDeserializeSecureMessage() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();
        final SecureMessage originalMessage = new SecureMessage(sender, recipient, new byte[] { 1, 2, 4 });
        final Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, new MockAccountLookup());

        // Act:
        final Message message = MessageFactory.deserialize(sender, recipient, deserializer);

        // Assert:
        Assert.assertThat(message, IsInstanceOf.instanceOf(SecureMessage.class));
        Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
        Assert.assertThat(message.getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 2, 4 }));
    }
}
