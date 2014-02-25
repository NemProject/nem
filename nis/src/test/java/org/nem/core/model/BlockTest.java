package org.nem.core.model;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.crypto.KeyPair;
import org.nem.core.serialization.ObjectDeserializer;
import org.nem.core.test.*;

public class BlockTest {

    //region Constructors

    @Test
    public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();

        // Act:
        final Block block = new Block(signer);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(0L));
    }

    @Test
    public void blockCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = new Account(new KeyPair(signer.getPublicKey()));
        final Block originalBlock = new Block(signer);
        final Block block = createRoundTrippedTransaction(originalBlock, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(0L));
        // TODO: add transactions to this
    }

    //endregion

    //region Fee

    @Test
    public void blockFeeIsSumOfTransactionFees() {
        // Arrange:
        Block block = new Block(Utils.generateRandomAccount());
        block.addTransaction(createTransactionWithFee(17));
        block.addTransaction(createTransactionWithFee(11));
        block.addTransaction(createTransactionWithFee(22));

        // Assert:
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(50L));
    }

    //endregion

    private static Transaction createTransactionWithFee(final long fee) {
        // Arrange:
        Account sender = Utils.generateRandomAccount();
        MockTransaction transaction = new MockTransaction(sender);
        transaction.setFee(fee);
        return transaction;
    }

    private Block createRoundTrippedTransaction(
        Block originalBlock,
        final Account deserializedSigner) {
        // Act:
        ObjectDeserializer deserializer = Utils.RoundtripVerifiableEntity(originalBlock, deserializedSigner);
        return new Block(deserializer.readInt("type"), deserializer);
    }
}
