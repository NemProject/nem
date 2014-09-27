package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

public class BlockTest {

	//region Constructors

	@Test
	public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = BlockUtils.createBlock(signer);

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(BlockUtils.DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));

		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(BlockUtils.DUMMY_GENERATION_HASH));
		Assert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void ctorCanCreateBlockAroundPreviousBlock() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block previousBlock = BlockUtils.createBlock(signer);

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

		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		final Hash expectedGenerationHash = HashUtils.nextHash(
				previousBlock.getGenerationHash(),
				signer.getKeyPair().getPublicKey());
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
		Assert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	//endregion

	//region Setters

	@Test
	public void previousBlockCanSet() {
		// Arrange:
		final Block previousBlock = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());

		// Act:
		block.setPrevious(previousBlock);

		// Assert:
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		final Hash expectedGenerationHash = HashUtils.nextHash(
				previousBlock.getGenerationHash(),
				block.getSigner().getKeyPair().getPublicKey());
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	@Test
	public void previousGenerationHashCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Hash previousGenerationHash = Utils.generateRandomHash();

		// Act:
		block.setPreviousGenerationHash(previousGenerationHash);

		// Assert:
		final Hash expectedGenerationHash = HashUtils.nextHash(
				previousGenerationHash,
				block.getSigner().getKeyPair().getPublicKey());
		Assert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	@Test
	public void blockDifficultyCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final BlockDifficulty blockDifficulty = new BlockDifficulty(44_444_444_444L);

		// Act:
		block.setDifficulty(blockDifficulty);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(blockDifficulty));
	}

	@Test
	public void blockLessorCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Account account = Utils.generateRandomAccount();

		// Act:
		block.setLessor(account);

		// Assert:
		Assert.assertThat(block.getLessor(), IsEqual.equalTo(account));
	}
	//endregion

	//region Serialization

	@Test
	public void blockCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = this.createBlockForRoundTripTests(true, signer);

		// Assert:
		Assert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(block.getType(), IsEqual.equalTo(1));
		Assert.assertThat(block.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		// (t1 has a fee of 1 and t2 has a fee of 2)
		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.fromNem(3L)));
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(BlockUtils.DUMMY_PREVIOUS_HASH));
		Assert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.getAmount(), IsEqual.equalTo(Amount.fromNem(17)));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.getAmount(), IsEqual.equalTo(Amount.fromNem(290)));
	}

	@Test
	public void blockDifficultyIsNotRoundTripped() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
	}

	@Test
	public void blockLessorIsNotRoundTripped() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Assert:
		Assert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void changingDifficultyDoesNotInvalidateBlock() {
		// Arrange:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Act
		final boolean originalVerifyResult = block.verify();
		block.setDifficulty(new BlockDifficulty(55_444_333_222_111L));

		// Assert:
		Assert.assertThat(block.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(55_444_333_222_111L)));
		Assert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		Assert.assertThat(block.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void changingPreviousBlockInvalidatesBlock() {
		// Arrange:
		final Block previousBlock = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Act
		final boolean originalVerifyResult = block.verify();
		block.setPrevious(previousBlock);

		// Assert:
		Assert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		Assert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		Assert.assertThat(block.verify(), IsEqual.equalTo(false));
	}

	@Test
	public void blockAndTransactionsCanBeVerifiedAfterVerifiableRoundTrip() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

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
		final Block block = this.createBlockForRoundTripTests(false, null);

		// Assert:
		Assert.assertThat(block.getSignature(), IsNull.nullValue());

		final List<Transaction> transactions = block.getTransactions();
		Assert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction)transactions.get(0);
		Assert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

		final TransferTransaction transaction2 = (TransferTransaction)transactions.get(1);
		Assert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
	}

	private Block createBlockForRoundTripTests(final boolean verifiable, final Account signer) {
		// Arrange:
		final Block originalBlock = BlockUtils.createBlock(null == signer ? Utils.generateRandomAccount() : signer);
		originalBlock.setLessor(Utils.generateRandomAccount());
		final TransferTransaction transaction1 = this.createSignedTransactionWithAmount(17);
		originalBlock.addTransaction(transaction1);

		final TransferTransaction transaction2 = this.createSignedTransactionWithAmount(290);
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

	private TransferTransaction createSignedTransactionWithAmount(final long amount) {
		final TransferTransaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(amount),
				null);
		transaction.sign();
		return transaction;
	}

	//endregion

	//region Transaction

	@Test
	public void singleTransactionCanBeAddedToBlock() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		final Transaction transaction = BlockUtils.createTransactionWithFee(17);

		// Act:
		block.addTransaction(transaction);

		// Assert:
		Assert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
		Assert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));
	}

	@Test
	public void multipleTransactionsCanBeAddedToBlock() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(BlockUtils.createTransactionWithFee(17));
		transactions.add(BlockUtils.createTransactionWithFee(11));

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
		final Block block = BlockUtils.createBlock();
		block.addTransaction(BlockUtils.createTransactionWithFee(17));
		block.addTransaction(BlockUtils.createTransactionWithFee(11));
		block.addTransactions(Arrays.asList(
				(Transaction)BlockUtils.createTransactionWithFee(3),
				BlockUtils.createTransactionWithFee(50)));
		block.addTransaction(BlockUtils.createTransactionWithFee(22));

		// Assert:
		Assert.assertThat(block.getTotalFee(), IsEqual.equalTo(new Amount(103L)));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		block.addTransaction(BlockUtils.createTransactionWithFee(1));
		block.addTransaction(BlockUtils.createTransactionWithFee(7));

		// Assert:
		Assert.assertThat(block.toString(), IsEqual.equalTo("height: 3, #tx: 2"));
	}

	//endregion
}