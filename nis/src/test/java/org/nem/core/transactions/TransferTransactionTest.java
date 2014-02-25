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
        final Account signer = Utils.generateRandomAccount();

        // Act:
        new TransferTransaction(signer, null, 123, new byte[] { 12, 50, 21 });
    }

    @Test
    public void ctorCanCreateTransactionWithMessage() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();

        // Act:
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 123, new byte[] { 12, 50, 21 });

        // Assert:
        Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
    }

    @Test
    public void ctorCanCreateTransactionWithoutMessage() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();

        // Act:
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 123, null);

        // Assert:
        Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { }));
    }

    @Test
    public void transactionCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccountWithoutPrivateKey();
        final TransferTransaction originalTransaction = new TransferTransaction(signer, recipient, 123, new byte[] { 12, 50, 21 });
        final TransferTransaction transaction = createRoundTrippedTransaction(originalTransaction, new AccountLookup() {
            public Account findByAddress(final Address address) {
                return address.equals(signer.getAddress()) ? signer : recipient;
            }
        });

        // Assert:
        Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(transaction.getRecipient(), IsEqual.equalTo(recipient));
        Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(123L));
        Assert.assertThat(transaction.getMessage(), IsEqual.equalTo(new byte[] { 12, 50, 21 }));
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
        final Account signer = Utils.generateRandomAccount();
        final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = new TransferTransaction(signer, recipient, amount, new byte[messageSize]);

        // Act:
        return transaction.getFee();
    }

    //endregion

    //region Valid

    @Test
    public void transactionsUpToSignerBalanceAreValid() {
        // Assert:
        Assert.assertThat(isTransactionAmountValid(100, 10, 1), IsEqual.equalTo(true));
        Assert.assertThat(isTransactionAmountValid(1000, 990, 10), IsEqual.equalTo(true));
        Assert.assertThat(isTransactionAmountValid(1000, 50, 950), IsEqual.equalTo(true));
    }

    @Test
    public void transactionsExceedingSignerBalanceAreInvalid() {
        // Assert:
        Assert.assertThat(isTransactionAmountValid(1000, 990, 11), IsEqual.equalTo(false));
        Assert.assertThat(isTransactionAmountValid(1000, 51, 950), IsEqual.equalTo(false));
        Assert.assertThat(isTransactionAmountValid(1000, 1001, 11), IsEqual.equalTo(false));
        Assert.assertThat(isTransactionAmountValid(1000, 51, 1001), IsEqual.equalTo(false));
    }

    private boolean isTransactionAmountValid(final int senderBalance, final int amount, final int fee) {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(senderBalance);
        final Account recipient = Utils.generateRandomAccount();
        TransferTransaction transaction = new TransferTransaction(signer, recipient, amount, null);
        transaction.setFee(fee);

        // Act:
        return transaction.isValid();
    }

    @Test
    public void smallMessagesAreValid() {
        // Assert:
        Assert.assertThat(isMessageSizeValid(0), IsEqual.equalTo(true));
        Assert.assertThat(isMessageSizeValid(999), IsEqual.equalTo(true));
        Assert.assertThat(isMessageSizeValid(1000), IsEqual.equalTo(true));
    }

    @Test
    public void largeMessagesAreInvalid() {
        // Assert:
        Assert.assertThat(isMessageSizeValid(1001), IsEqual.equalTo(false));
    }

    private boolean isMessageSizeValid(final int messageSize) {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(1000);
        final Account recipient = Utils.generateRandomAccount();
		TransferTransaction transaction = new TransferTransaction(signer, recipient, 1, new byte[messageSize]);

        // Act:
        return transaction.isValid();
    }

    //endregion

    //region Execute

    @Test
    public void executeTransfersAmountAndFeeFromSigner() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(1000);
        final Account recipient = Utils.generateRandomAccount();
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 99, null);
        transaction.setFee(10);

        // Act:
        transaction.execute();

        // Assert:
        Assert.assertThat(signer.getBalance(), IsEqual.equalTo(891L));
    }

    @Test
    public void executeTransfersAmountToSigner() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(1000);
        final Account recipient = Utils.generateRandomAccount();
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 99, null);
        transaction.setFee(10);

        // Act:
        transaction.execute();

        // Assert:
        Assert.assertThat(recipient.getBalance(), IsEqual.equalTo(99L));
    }

    @Test
    public void executeDoesNotAppendEmptyMessageToAccount() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(1000);
        final Account recipient = Utils.generateRandomAccount();
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 99, null);
        transaction.setFee(10);

        // Act:
        transaction.execute();

        // Assert:
        Assert.assertThat(recipient.getMessages().size(), IsEqual.equalTo(0));
    }

    @Test
    public void executeAppendsNonEmptyMessageToAccount() {
        // Arrange:
        final byte[] message = new byte[] { 0x12, 0x33, 0x0A };
        final Account signer = Utils.generateRandomAccount();
        signer.incrementBalance(1000);
        final Account recipient = Utils.generateRandomAccount();
        TransferTransaction transaction = new TransferTransaction(signer, recipient, 99, message);
        transaction.setFee(10);

        // Act:
        transaction.execute();

        // Assert:
        Assert.assertThat(recipient.getMessages().size(), IsEqual.equalTo(1));
        Assert.assertThat(recipient.getMessages().get(0).getEncryptedMessage(), IsEqual.equalTo(message));
    }

    //endregion

    private TransferTransaction createRoundTrippedTransaction(
        final Transaction originalTransaction,
        final AccountLookup accountLookup) {
        // Act:
        ObjectDeserializer deserializer = Utils.RoundtripVerifiableEntity(originalTransaction, accountLookup);
        deserializer.readInt("type");
        return new TransferTransaction(deserializer);
    }
}