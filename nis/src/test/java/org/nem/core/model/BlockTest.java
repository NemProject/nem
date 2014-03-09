package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.transactions.TransferTransaction;
import org.nem.nis.Genesis;

import java.util.List;

public class BlockTest {

	final static byte[] DUMMY_PREVIOUS_HASH = { 0,1,2,3,4,5,6,7,8,9, 0,1,2,3,4,5,6,7,8,9, 0,1,2,3,4,5,6,7,8,9, 1,2 };
    //region Constructors

    @Test
    public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();

        // Act:
        final Block block = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);

        // Assert:
        Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(block.getType(), IsEqual.equalTo(1));
        Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
        Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(0L));
		Assert.assertThat(block.getTimestamp(), IsEqual.equalTo(Genesis.INITIAL_TIME));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(Genesis.INITIAL_HEIGHT));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
    }

    //endregion

	//region Hashing

	@Test
	public void identicalBlocksHaveSameHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block1 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
		final Block block2 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);

		// Act:
		byte[] hash1 = block1.getHash();
		byte[] hash2 = block2.getHash();

		// Assert:
		Assert.assertThat(hash1, IsEqual.equalTo(hash2));
	}

	@Test
	public void differenBlocksHaveDifferentHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block1 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
		final Block block2 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME + 1, Genesis.INITIAL_HEIGHT);

		// Act:
		byte[] hash1 = block1.getHash();
		byte[] hash2 = block2.getHash();

		// Assert:
		Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
	}

	@Test
	public void signingDoesntChangeBlockHashes() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block1 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
		final Block block2 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);

		block1.sign();

		// Act:
		byte[] hash1 = block1.getHash();
		byte[] hash2 = block2.getHash();

		// Assert:
		Assert.assertThat(hash1, IsEqual.equalTo(hash2));
	}

	@Test
	public void addingTransactionChangesBlockHash() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block block1 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
		final Block block2 = new Block(signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME + 1, Genesis.INITIAL_HEIGHT);

		Transaction tx = createSignedTransactionWithAmount(100);
		block1.addTransaction(tx);

		// Act:
		byte[] hash1 = block1.getHash();
		byte[] hash2 = block2.getHash();

		// Assert:
		Assert.assertThat(hash1, IsNot.not(IsEqual.equalTo(hash2)));
		Assert.assertThat(block1.getTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat(block1.getTransactions().get(0), IsEqual.equalTo(tx));
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

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(2L));
		Assert.assertThat(block.getTimestamp(), IsEqual.equalTo(Genesis.INITIAL_TIME));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(Genesis.INITIAL_HEIGHT));

		final List<Transaction> transactions = block.getTransactions();
        Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

        final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
        Assert.assertThat(transaction1.getAmount(), IsEqual.equalTo(17L));

        final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
        Assert.assertThat(transaction2.getAmount(), IsEqual.equalTo(290L));
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
        final Block originalBlock = new Block(null == signer ? Utils.generateRandomAccount() : signer, DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
        TransferTransaction transaction1 = createSignedTransactionWithAmount(17);
        originalBlock.addTransaction(transaction1);

        TransferTransaction transaction2 = createSignedTransactionWithAmount(290);
        originalBlock.addTransaction(transaction2);
        originalBlock.sign();

        // Arrange:
        MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(originalBlock.getSigner()));
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction1.getSigner()));
        accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(transaction2.getSigner()));

        // Act:
        SerializableEntity entity = verifiable ? originalBlock : originalBlock.asNonVerifiable();
        VerifiableEntity.DeserializationOptions options = verifiable
            ? VerifiableEntity.DeserializationOptions.VERIFIABLE
            : VerifiableEntity.DeserializationOptions.NON_VERIFIABLE;

        Deserializer deserializer = Utils.roundtripSerializableEntity(entity, accountLookup);
        return new Block(deserializer.readInt("type"), options, deserializer);
    }

    private TransferTransaction createSignedTransactionWithAmount(long amount) {
        TransferTransaction transaction = new TransferTransaction(
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
        Block block = new Block(Utils.generateRandomAccount(), DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
        Transaction transaction = createTransactionWithFee(17);

        // Act:
        block.addTransaction(transaction);

        // Assert:
        Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
        Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(17L));
		Assert.assertThat(block.getTimestamp(), IsEqual.equalTo(Genesis.INITIAL_TIME));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(Genesis.INITIAL_HEIGHT));
    }


    //endregion

    //region Fee

    @Test
    public void blockFeeIsSumOfTransactionFees() {
        // Arrange:
        Block block = new Block(Utils.generateRandomAccount(), DUMMY_PREVIOUS_HASH, Genesis.INITIAL_TIME, Genesis.INITIAL_HEIGHT);
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
}
