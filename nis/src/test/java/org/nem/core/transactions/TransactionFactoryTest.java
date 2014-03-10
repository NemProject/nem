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
        TransactionFactory.VERIFIABLE.deserialize(deserializer);
    }

    @Test
    public void canDeserializeVerifiableTransferTransaction() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
        Transaction originalTransaction = new TransferTransaction(0, sender, recipient, 100, null);
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, new MockAccountLookup());

        // Act:
        Transaction transaction = TransactionFactory.VERIFIABLE.deserialize(deserializer);

        // Assert:
        Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
        Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
        Assert.assertThat(transaction.getSignature(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void canDeserializeNonVerifiableTransferTransaction() {
        // Arrange:
        final Account sender = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();
        Transaction originalTransaction = new TransferTransaction(0, sender, recipient, 100, null);
        Deserializer deserializer = Utils.roundtripSerializableEntity(
            originalTransaction.asNonVerifiable(),
            new MockAccountLookup());

        // Act:
        Transaction transaction = TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);

        // Assert:
        Assert.assertThat(transaction, IsInstanceOf.instanceOf(TransferTransaction.class));
        Assert.assertThat(transaction.getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
        Assert.assertThat(transaction.getSignature(), IsEqual.equalTo(null));
    }
}
