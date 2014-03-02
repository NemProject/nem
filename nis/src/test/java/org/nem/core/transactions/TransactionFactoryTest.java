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
        TransactionFactory.deserialize(deserializer);
    }

    @Test
    public void canDeserializeTransferTransaction() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
        Transaction originalTransaction = new TransferTransaction(sender, recipient, 100, null);
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());

        // Act:
        Transaction transaction = TransactionFactory.deserialize(deserializer);

        // Assert:
        Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
        Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
    }
}
