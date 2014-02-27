package org.nem.core.transactions;

import net.minidev.json.*;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class TransactionFactoryTest {

    @Test(expected = InvalidParameterException.class)
    public void cannotDeserializeUnknownTransaction() {
        // Arrange:
        JSONObject object = new JSONObject();
        object.put("type", 7);
        JsonDeserializer deserializer = new JsonDeserializer(object, null);

        // Act:
        TransactionFactory.Deserialize(deserializer);
    }

    @Test
    public void canDeserializeTransferTransaction() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();

        Transaction originalTransaction = new TransferTransaction(sender, recipient, 100, null);
        originalTransaction.sign();

        JsonSerializer serializer = new JsonSerializer();
        originalTransaction.serialize(serializer);

        JsonDeserializer deserializer = new JsonDeserializer(
            serializer.getObject(),
            new DeserializationContext(new MockAccountLookup()));

        // Act:
        Transaction transaction = TransactionFactory.Deserialize(deserializer);

        // Assert:
        Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
    }
}
