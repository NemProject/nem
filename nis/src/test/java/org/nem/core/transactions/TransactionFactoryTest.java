package org.nem.core.transactions;

import org.hamcrest.core.IsInstanceOf;
import org.json.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;

import java.security.InvalidParameterException;

public class TransactionFactoryTest {

    @Test(expected = InvalidParameterException.class)
    public void cannotDeserializeUnknownTransaction() throws Exception {
        // Arrange:
        JSONObject object = new JSONObject();
        object.put("type", 7);
        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(new JsonDeserializer(object), new MockAccountLookup());

        // Act:
        TransactionFactory.Deserialize(deserializer);
    }

    @Test
    public void canDeserializeTransferTransaction() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        Transaction originalTransaction = new TransferTransaction(sender, 100, null);
        originalTransaction.sign();

        JsonSerializer jsonSerializer = new JsonSerializer();
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        originalTransaction.serialize(serializer);

        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            new MockAccountLookup());

        // Act:
        Transaction transaction = TransactionFactory.Deserialize(deserializer);

        // Assert:
        Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
    }
}
