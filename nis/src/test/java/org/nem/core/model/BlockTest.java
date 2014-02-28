package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.transactions.TransferTransaction;

import java.util.List;

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
        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
    }

    @Test
    public void blockCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final Block originalBlock = new Block(signer);
        originalBlock.addTransaction(createSignedTransactionWithAmount(17));
        originalBlock.addTransaction(createSignedTransactionWithAmount(290));
        originalBlock.sign();

        final Block block = createRoundTrippedTransaction(originalBlock, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(2L));

        final List<Transaction> transactions = block.getTransactions();
        Assert.assertThat(transactions.size(), IsEqual.equalTo(2));
        Assert.assertThat(((TransferTransaction)transactions.get(0)).getAmount(), IsEqual.equalTo(17L));
        Assert.assertThat(((TransferTransaction)transactions.get(1)).getAmount(), IsEqual.equalTo(290L));
    }

    private Transaction createSignedTransactionWithAmount(long amount) {
        Transaction transaction = new TransferTransaction(
            Utils.generateRandomAccount(),
            Utils.generateRandomAccount(),
            amount,
            null);
        transaction.sign();
        return transaction;
    }

    //endregion

    //region Transaction

    @Test
    public void transactionsCanBeAddedToBlock() {
        // Arrange:
        Block block = new Block(Utils.generateRandomAccount());
        Transaction transaction = createTransactionWithFee(17);

        // Act:
        block.addTransaction(transaction);

        // Assert:
        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
        Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));
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
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalBlock, deserializedSigner);
        return new Block(deserializer.readInt("type"), deserializer);
    }
}
