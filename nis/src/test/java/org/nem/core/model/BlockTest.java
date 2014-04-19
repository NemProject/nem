package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class BlockTest {

	final static Hash DUMMY_PREVIOUS_HASH = new Hash(new byte[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
			1, 2 });

	final static Hash DUMMY_GENERATION_HASH = new Hash(new byte[] {
			9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
			9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
			9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
			8, 7 });

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
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
	}

	@Test
	public void ctorCanCreateBlockAroundPreviousBlock() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block previousBlock = createBlock(signer);

		// Act:
		final Block block = new Block(signer, previousBlock, new TimeInstant(11));

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(11)));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(4)));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));
	}

	//endregion

	//region Setters

	@Test
	public void blockDifficultyCanBeSet() {
		// Arrange:
		final Block block = createBlock(Utils.generateRandomAccount());
		final BlockDifficulty blockDifficulty = new BlockDifficulty(44_444_444_444L);

		// Act:
		block.setDifficulty(blockDifficulty);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(blockDifficulty));
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
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.getAmount(), IsEqual.equalTo(new Amount(17L)));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.getAmount(), IsEqual.equalTo(new Amount(290L)));
	}

	@Test
	public void blockDifficultyIsNotRoundTripped() {
		// Act:
		final Block block = createBlockForRoundTripTests(true, null);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
	}

	@Test
	public void changingDifficultyDoesNotInvalidateBlock() {
		// Arrange:
		final Block block = createBlockForRoundTripTests(true, null);

		// Act
		boolean result = block.verify();
		block.setDifficulty(new BlockDifficulty(55_444_333_222_111L));

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(55_444_333_222_111L)));
		Assert.assertThat(result, IsEqual.equalTo(true));
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
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
		originalBlock.setDifficulty(new BlockDifficulty(22_222_222_222L));
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

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final Block block = createBlock();
		block.addTransaction(createTransactionWithFee(1));
		block.addTransaction(createTransactionWithFee(7));

		// Assert:
		Assert.assertThat(block.toString(), IsEqual.equalTo("height: 3, #tx: 2"));
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
		Block block = new Block(forger, DUMMY_PREVIOUS_HASH, new TimeInstant(7), new BlockHeight(3));
		block.setGenerationHash(DUMMY_GENERATION_HASH);
		return block;
	}

	private static Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}
}
