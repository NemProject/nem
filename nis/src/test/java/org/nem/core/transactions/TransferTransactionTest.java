package org.nem.core.transactions;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccountLookup;

public class TransferTransactionTest {

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
		final Address recipient = Address.fromEncoded("NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		TransferTransaction transaction = new TransferTransaction(sender, amount, new byte[messageSize], recipient);

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
		final Address recipient = Address.fromEncoded("NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		TransferTransaction transaction = new TransferTransaction(sender, 1, new byte[messageSize], recipient);

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
		final Address recipient = Address.fromEncoded("NBKLYTH6OWWQCQ6OI66HJOPBGLXWVQG6V2UTQEUI");
		final TransferTransaction originalTransaction = new TransferTransaction(sender, 123, new byte[] { 12, 50, 21 }, recipient);
        final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, senderPublicKeyOnly);

        // Assert:
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
