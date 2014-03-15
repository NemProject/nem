package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;

import java.util.*;

public class BlockTest {

	final static byte[] DUMMY_PREVIOUS_HASH = { 0,1,2,3,4,5,6,7,8,9, 0,1,2,3,4,5,6,7,8,9, 0,1,2,3,4,5,6,7,8,9, 1,2 };
    //region Constructors

    @Test
    public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();

        // Act:
        final Block block = createBlock(signer);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
        Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(3L));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
    }

    //endregion

    //region Serialization

    @Test
    public void blockCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();

        // Act:
        final Block block = createBlockForRoundTripTests(true, signer);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(new Amount(2L)));
        Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(3L));

		final List<Transaction> transactions = block.getTransactions();
        Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

        final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
        Assert.assertThat(transaction1.getAmount(), IsEqual.equalTo(new Amount(17L)));

        final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
        Assert.assertThat(transaction2.getAmount(), IsEqual.equalTo(new Amount(290L)));
    }

    @Test
    public void blockAndTransactionsCanBeVerifiedAfterVerifiableRoundTrip() {
        // Act:
        final Block block = createBlockForRoundTripTests(true, null);

        // Assert:
        Assert.assertThat(block.verify(), IsEqual.equalTo(true));

        final List<Transaction> transactions = block.getTransactions();
        Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

        final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
        Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

        final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
        Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
    }

    @Test
     public void transactionsCanBeVerifiedAfterNonVerifiableRoundTrip() {
        // Act:
        final Block block = createBlockForRoundTripTests(false, null);

        // Assert:
        Assert.assertThat(block.getSignature(), IsEqual.equalTo(null));

        final List<Transaction> transactions = block.getTransactions();
        Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

        final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
        Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

        final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
        Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
    }

    private Block createBlockForRoundTripTests(boolean verifiable, final Account signer) {
        // Arrange:
        final Block originalBlock = createBlock(null == signer ? Utils.generateRandomAccount() : signer);
        final TransferTransaction transaction1 = createSignedTransactionWithAmount(17);
        originalBlock.addTransaction(transaction1);

        final TransferTransaction transaction2 = createSignedTransactionWithAmount(290);
        originalBlock.addTransaction(transaction2);
        originalBlock.sign();

        // Arrange:
        final MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(originalBlock.getSigner()));
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction1.getSigner()));
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction2.getSigner()));

        // Act:
        final SerializableEntity entity = verifiable ? originalBlock : originalBlock.asNonVerifiable();
        final VerifiableEntity.DeserializationOptions options = verifiable
            ? VerifiableEntity.DeserializationOptions.VERIFIABLE
            : VerifiableEntity.DeserializationOptions.NON_VERIFIABLE;

        final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, accountLookup);
        return new Block(deserializer.readInt("type"), options, deserializer);
    }

    private TransferTransaction createSignedTransactionWithAmount(long amount) {
        final TransferTransaction transaction = new TransferTransaction(
            TimeInstant.ZERO,
            Utils.generateRandomAccount(),
            Utils.generateRandomAccount(),
            new Amount(amount),
            null);
        transaction.sign();
        return transaction;
    }

    //endregion

    //region Transaction

    @Test
    public void singleTransactionCanBeAddedToBlock() {
        // Arrange:
        final Block block = createBlock();
        final Transaction transaction = createTransactionWithFee(17);

        // Act:
        block.addTransaction(transaction);

        // Assert:
        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
        Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));
    }

    @Test
    public void multipleTransactionsCanBeAddedToBlock() {
        // Arrange:
        final Block block = createBlock();
        final List<Transaction> transactions = new ArrayList<>();
        transactions.add(createTransactionWithFee(17));
        transactions.add(createTransactionWithFee(11));

        // Act:
        block.addTransactions(transactions);

        // Assert:
        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(2));
        Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transactions.get(0)));
        Assert.assertThat((block.getTransactions().get(1)), IsEqual.equalTo(transactions.get(1)));
    }

    //endregion

    //region Fee

    @Test
    public void blockFeeIsSumOfTransactionFees() {
        // Arrange:
        final Block block = createBlock();
        block.addTransaction(createTransactionWithFee(17));
        block.addTransaction(createTransactionWithFee(11));
        block.addTransactions(Arrays.asList(createTransactionWithFee(3), createTransactionWithFee(50)));
        block.addTransaction(createTransactionWithFee(22));

        // Assert:
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(new Amount(103L)));
    }

    //endregion

    private static Transaction createTransactionWithFee(final long fee) {
        // Arrange:
        Account sender = Utils.generateRandomAccount();
        MockTransaction transaction = new MockTransaction(sender);
        transaction.setFee(new Amount(fee));
        return transaction;
    }

    private static Block createBlock(final Account forger) {
        // Arrange:
        return new Block(forger, DUMMY_PREVIOUS_HASH, new TimeInstant(7), 3);
    }

    private static Block createBlock() {
        // Arrange:
        return createBlock(Utils.generateRandomAccount());
    }
}
