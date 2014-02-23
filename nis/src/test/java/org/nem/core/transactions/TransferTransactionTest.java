package org.nem.core.transactions;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class TransferTransactionTest {

    //region Constructor

    @Test(expected = InvalidParameterException.class)
    public void recipientIsRequired() {
        // Arrange:
        final Account sender = new Account(new KeyPair());

        // Act:
        new TransferTransaction(sender, null, 123, new byte[] { 12, 50, 21 });
    }

    @Test
    public void ctorCanCreateTransactionWithMessage() {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Address recipient = Utils.generateRandomAddress();

        // Act:
        TransferTransaction transaction = new TransferTransaction(sender, recipient, 123, new byte[] { 12, 50, 21 });

        // Assert:
        Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
    }

    @Test
    public void ctorCanCreateTransactionWithoutMessage() {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Address recipient = Utils.generateRandomAddress();

        // Act:
        TransferTransaction transaction = new TransferTransaction(sender, recipient, 123, null);

        // Assert:
        Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { }));
    }

    //endregion

    //region Fee

    @Test
    public void feeIsCalculatedCorrectlyForTransactionWithoutMessage() {
        // Assert:
        Assert.assertThat(calculateFee(12000, 0), IsEqual.equalTo(12L));
        Assert.assertThat(calculateFee(12001, 0), IsEqual.equalTo(13L));
        Assert.assertThat(calculateFee(13000, 0), IsEqual.equalTo(13L));
    }

    @Test
    public void feeIsCalculatedCorrectlyForTransactionWithMessage() {
        // Assert:
        Assert.assertThat(calculateFee(12000, 1), IsEqual.equalTo(13L));
        Assert.assertThat(calculateFee(12000, 199), IsEqual.equalTo(13L));
        Assert.assertThat(calculateFee(13000, 200), IsEqual.equalTo(14L));
    }

    private long calculateFee(final long amount, final int messageSize){
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Address recipient = Utils.generateRandomAddress();
		TransferTransaction transaction = new TransferTransaction(sender, recipient, amount, new byte[messageSize]);

        // Act:
        return transaction.getFee();
    }

    //endregion

    //region Valid

    @Test
    public void largeMessagesAreInvalid() {
        // Assert:
        Assert.assertThat(isMessageSizeValid(0), IsEqual.equalTo(true));
        Assert.assertThat(isMessageSizeValid(999), IsEqual.equalTo(true));
        Assert.assertThat(isMessageSizeValid(1000), IsEqual.equalTo(true));
        Assert.assertThat(isMessageSizeValid(1001), IsEqual.equalTo(false));
    }

    private boolean isMessageSizeValid(final int messageSize){
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Address recipient = Utils.generateRandomAddress();
		TransferTransaction transaction = new TransferTransaction(sender, recipient, 1, new byte[messageSize]);

        // Act:
        return transaction.isValid();
    }

    //endregion

    //region Serialization

    @Test
    public void transactionCanBeRoundTripped() {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Account senderPublicKeyOnly = new Account(new KeyPair(sender.getPublicKey()));
        final Address recipient = Utils.generateRandomAddress();
		final TransferTransaction originalTransaction = new TransferTransaction(sender, recipient, 123, new byte[] { 12, 50, 21 });
        final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, senderPublicKeyOnly);

        // Assert:
        Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
    }

    //endregion

    private TransferTransaction createRoundTrippedTransaction(
        Transaction originalTransaction,
        final Account deserializedSender) {
        // Arrange:
        originalTransaction.sign();

        // Act:
        JsonSerializer jsonSerializer = new JsonSerializer(true);
        ObjectSerializer serializer = new DelegatingObjectSerializer(jsonSerializer);
        originalTransaction.serialize(serializer);

        final MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(deserializedSender);
        ObjectDeserializer deserializer = new DelegatingObjectDeserializer(
            new JsonDeserializer(jsonSerializer.getObject()),
            accountLookup);

        deserializer.readInt("type");
        return new TransferTransaction(deserializer);
    }
}
