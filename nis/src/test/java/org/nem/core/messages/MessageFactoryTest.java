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
        JSONObject object = new JSONObject();
        object.put("type", 7);
        JsonDeserializer deserializer = new JsonDeserializer(object, null);

        // Act:
        MessageFactory.deserialize(deserializer);
    }

    @Test
    public void canDeserializePlainMessage() {
        // Arrange:
        PlainMessage originalMessage = new PlainMessage(new byte[] { });
        Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, new MockAccountLookup());

        // Act:
        Message message = MessageFactory.deserialize(deserializer);

        // Assert:
        Assert.assertThat(message, IsInstanceOf.instanceOf(PlainMessage.class));
        Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.PLAIN));
    }

    @Test
    public void canDeserializeSecureMessage() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();
        SecureMessage originalMessage = new SecureMessage(sender, recipient, new byte[] { });
        Deserializer deserializer = Utils.roundtripSerializableEntity(originalMessage, new MockAccountLookup());

        // Act:
        Message message = MessageFactory.deserialize(deserializer);

        // Assert:
        Assert.assertThat(message, IsInstanceOf.instanceOf(SecureMessage.class));
        Assert.assertThat(message.getType(), IsEqual.equalTo(MessageTypes.SECURE));
    }
}
