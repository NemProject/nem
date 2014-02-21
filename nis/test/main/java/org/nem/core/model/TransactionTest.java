package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.MockAccount;
import org.nem.core.test.MockAccountLookup;

import java.security.InvalidParameterException;

public class TransactionTest {

    //region New

    @Test
    public void ctorCanCreateTransactionForAccountWithSenderPrivateKey() throws Exception {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account sender = new Account(publicPrivateKeyPair);

        // Act:
        final MockTransaction transaction = new MockTransaction(sender, 6);

        // Assert:
        Assert.assertThat(transaction.getType(), IsEqual.equalTo(123));
        Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(759));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
        Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
        Assert.assertThat(transaction.getSignature(), IsEqual.equalTo(null));
        Assert.assertThat(transaction.getFee(), IsEqual.equalTo(0L));
    }

    @Test(expected = InvalidParameterException.class)
    public void ctorCannotCreateTransactionForAccountWithoutSenderPrivateKey() throws Exception {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final KeyPair publicOnlyKeyPair = new KeyPair(publicPrivateKeyPair.getPublicKey());

        // Act:
        new MockTransaction(new Account(publicOnlyKeyPair));
    }

    //endregion

    //region Serialization

    @Test
    public void transactionCanBeRoundTripped() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Account senderPublicKeyOnly = new Account(new KeyPair(sender.getPublicKey()));
        final MockTransaction originalTransaction = new MockTransaction(sender, 7);
        originalTransaction.setFee(130);
        final MockTransaction transaction = createRoundTrippedTransaction(originalTransaction, senderPublicKeyOnly);

        // Assert:
        Assert.assertThat(transaction.getType(), IsEqual.equalTo(123));
        Assert.assertThat(transaction.getVersion(), IsEqual.equalTo(759));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(7));
        Assert.assertThat(transaction.getSender(), IsEqual.equalTo(senderPublicKeyOnly));
        Assert.assertThat(transaction.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(transaction.getFee(), IsEqual.equalTo(130L));
    }

    @Test
    public void roundTrippedTransactionCanBeVerified() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final Account senderPublicKeyOnly = new Account(new KeyPair(sender.getPublicKey()));
        final MockTransaction transaction = createRoundTrippedTransaction(sender, 7, senderPublicKeyOnly);

        // Assert:
        Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
    }

    @Test(expected = SerializationException.class)
    public void serializeFailsIfSignatureIsNotPresent() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final MockTransaction transaction = new MockTransaction(sender);

        // Act:
        transaction.serialize(new DelegatingObjectSerializer(new JsonSerializer()));
    }

    //endregion

    //region Sign / Verify

    @Test
    public void signCreatesValidSignature() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final MockTransaction transaction = new MockTransaction(sender);

        // Act:
        transaction.sign();

        // Assert:
        Assert.assertThat(transaction.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(transaction.verify(), IsEqual.equalTo(true));
    }

    @Test
    public void changingFieldInvalidatesSignature() throws Exception {
        // Arrange:
        final Account sender = new Account(new KeyPair());
        final MockTransaction transaction = new MockTransaction(sender, 7);

        // Act:
        transaction.sign();
        transaction.setCustomField(12);

        // Assert:
        Assert.assertThat(transaction.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(transaction.verify(), IsEqual.equalTo(false));
    }

    @Test(expected = InvalidParameterException.class)
    public void cannotSignWithoutPrivateKey() throws Exception {
        // Arrange:
        final Account sender = new MockAccount("Gamma");
        final Account senderPublicKeyOnly = new Account(new KeyPair(sender.getPublicKey()));
        final MockTransaction transaction = createRoundTrippedTransaction(sender, 7, senderPublicKeyOnly);

        // Assert:
        transaction.sign();
    }

    //endregion

    //region Fees

    @Test
    public void feeIsMaximumOfMinimumFeeAndCurrentFee() throws Exception {
        // Assert:
        Assert.assertThat(getFee(15L, 50L), IsEqual.equalTo(50L));
        Assert.assertThat(getFee(130L, 50L), IsEqual.equalTo(130L));
    }

    private long getFee(long minimumFee, long fee) throws Exception {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account sender = new Account(publicPrivateKeyPair);

        // Act:
        final MockTransaction transaction = new MockTransaction(sender);
        transaction.setMinimumFee(minimumFee);
        transaction.setFee(fee);
        return transaction.getFee();
    }

    //endregion

    private MockTransaction createRoundTrippedTransaction(
        final Account originalSender,
        final int customField,
        final Account deserializedSender) throws Exception {
        // Act:
        final MockTransaction originalTransaction = new MockTransaction(originalSender, customField);
        return createRoundTrippedTransaction(originalTransaction, deserializedSender);
    }

    private MockTransaction createRoundTrippedTransaction(
        Transaction originalTransaction,
        final Account deserializedSender) throws Exception {
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
        return new MockTransaction(deserializer);
    }

    private class MockTransaction extends Transaction {

        private int customField;
        private long minimumFee;

        public MockTransaction(final Account sender) throws Exception {
            this(sender, 0);
        }

        public MockTransaction(final Account sender, final int customField) throws Exception {
            super(123, 759, sender);
            this.customField = customField;
        }

        public MockTransaction(final ObjectDeserializer deserializer) throws Exception {
            super(deserializer.readInt("type"), deserializer);
            this.customField = deserializer.readInt("customField");
        }

        public int getCustomField() { return this.customField; }
        public void setCustomField(final int customField) { this.customField = customField; }

        public void setMinimumFee(final long minimumFee) { this.minimumFee = minimumFee; }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        protected long getMinimumFee() {
            return this.minimumFee;
        }

        @Override
        protected void serializeImpl(ObjectSerializer serializer) throws Exception {
            serializer.writeInt("customField", this.customField);
        }
    }
}
