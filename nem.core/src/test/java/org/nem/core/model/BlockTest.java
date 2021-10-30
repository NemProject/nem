package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.*;

public class BlockTest {

	// region Constructors

	@Test
	public void ctorCanCreateBlockForAccountWithSignerPrivateKey() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = BlockUtils.createBlock(signer);

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(BlockUtils.DUMMY_PREVIOUS_HASH));
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));

		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		MatcherAssert.assertThat(block.getGenerationHash(), IsEqual.equalTo(BlockUtils.DUMMY_GENERATION_HASH));
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void ctorCreatesNemesisBlockTypeAroundBlockHeightOne() {
		// Act:
		final Block block = BlockUtils.createBlockWithHeight(BlockHeight.ONE);

		// Assert:
		MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(-1));
		MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(BlockHeight.ONE));
	}

	@Test
	public void ctorCreatesRegularBlockTypeAroundOtherBlockHeights() {
		// Act:
		final Block block = BlockUtils.createBlockWithHeight(new BlockHeight(2));

		// Assert:
		MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(2)));
	}

	@Test
	public void ctorCanCreateBlockAroundPreviousBlock() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Block previousBlock = BlockUtils.createBlock(signer);

		// Act:
		final Block block = new Block(signer, previousBlock, new TimeInstant(11));

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(11)));

		MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.ZERO));
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(4)));
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(0));

		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
		final Hash expectedGenerationHash = HashUtils.nextHash(previousBlock.getGenerationHash(), signer.getAddress().getPublicKey());
		MatcherAssert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	// endregion

	// region Setters

	@Test
	public void previousBlockCanSet() {
		// Arrange:
		final Block previousBlock = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());

		// Act:
		block.setPrevious(previousBlock);

		// Assert:
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		final Hash expectedGenerationHash = HashUtils.nextHash(previousBlock.getGenerationHash(),
				block.getSigner().getAddress().getPublicKey());
		MatcherAssert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	@Test
	public void previousGenerationHashCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Hash previousGenerationHash = Utils.generateRandomHash();

		// Act:
		block.setPreviousGenerationHash(previousGenerationHash);

		// Assert:
		final Hash expectedGenerationHash = HashUtils.nextHash(previousGenerationHash, block.getSigner().getAddress().getPublicKey());
		MatcherAssert.assertThat(block.getGenerationHash(), IsEqual.equalTo(expectedGenerationHash));
	}

	@Test
	public void blockDifficultyCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final BlockDifficulty blockDifficulty = new BlockDifficulty(44_444_444_444L);

		// Act:
		block.setDifficulty(blockDifficulty);

		// Assert:
		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(blockDifficulty));
	}

	@Test
	public void blockLessorCanBeSet() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());
		final Account account = Utils.generateRandomAccount();

		// Act:
		block.setLessor(account);

		// Assert:
		MatcherAssert.assertThat(block.getLessor(), IsEqual.equalTo(account));
	}

	@Test
	public void settingLessorToSignerDoesNotSetLessor() {
		// Arrange:
		final Block block = BlockUtils.createBlock(Utils.generateRandomAccount());

		// Act:
		block.setLessor(block.getSigner());

		// Assert:
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	// endregion

	// region Serialization

	@Test
	public void blockCanBeRoundTripped() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final Block block = this.createBlockForRoundTripTests(true, signer);

		// Assert:
		MatcherAssert.assertThat(block.getSigner(), IsEqual.equalTo(signer));
		MatcherAssert.assertThat(block.getType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(block.getVersion(), IsEqual.equalTo(VerifiableEntityUtils.VERSION_ONE));
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(7)));

		// (t1 has a fee of 2 and t2 has a fee of 140)
		MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.fromNem(2 + 140)));
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(BlockUtils.DUMMY_PREVIOUS_HASH));
		MatcherAssert.assertThat(block.getHeight(), IsEqual.equalTo(new BlockHeight(3)));

		final List<Transaction> transactions = block.getTransactions();
		MatcherAssert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction) transactions.get(0);
		MatcherAssert.assertThat(transaction1.getAmount(), IsEqual.equalTo(Amount.fromNem(17)));

		final TransferTransaction transaction2 = (TransferTransaction) transactions.get(1);
		MatcherAssert.assertThat(transaction2.getAmount(), IsEqual.equalTo(Amount.fromNem(1_000_000)));
	}

	@Test
	public void blockDifficultyIsNotRoundTripped() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Assert:
		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(BlockDifficulty.INITIAL_DIFFICULTY));
	}

	@Test
	public void blockLessorIsNotRoundTripped() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Assert:
		MatcherAssert.assertThat(block.getLessor(), IsNull.nullValue());
	}

	@Test
	public void changingDifficultyDoesNotInvalidateBlock() {
		// Arrange:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Act
		final boolean originalVerifyResult = block.verify();
		block.setDifficulty(new BlockDifficulty(55_444_333_222_111L));

		// Assert:
		MatcherAssert.assertThat(block.getDifficulty(), IsEqual.equalTo(new BlockDifficulty(55_444_333_222_111L)));
		MatcherAssert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		MatcherAssert.assertThat(block.verify(), IsEqual.equalTo(true));
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
		MatcherAssert.assertThat(block.getPreviousBlockHash(), IsEqual.equalTo(HashUtils.calculateHash(previousBlock)));
		MatcherAssert.assertThat(originalVerifyResult, IsEqual.equalTo(true));
		MatcherAssert.assertThat(block.verify(), IsEqual.equalTo(false));
	}

	@Test
	public void blockAndTransactionsCanBeVerifiedAfterVerifiableRoundTrip() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(true, null);

		// Assert:
		MatcherAssert.assertThat(block.verify(), IsEqual.equalTo(true));

		final List<Transaction> transactions = block.getTransactions();
		MatcherAssert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction) transactions.get(0);
		MatcherAssert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

		final TransferTransaction transaction2 = (TransferTransaction) transactions.get(1);
		MatcherAssert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionsCanBeVerifiedAfterNonVerifiableRoundTrip() {
		// Act:
		final Block block = this.createBlockForRoundTripTests(false, null);

		// Assert:
		MatcherAssert.assertThat(block.getSignature(), IsNull.nullValue());

		final List<Transaction> transactions = block.getTransactions();
		MatcherAssert.assertThat(transactions.size(), IsEqual.equalTo(2));

		final TransferTransaction transaction1 = (TransferTransaction) transactions.get(0);
		MatcherAssert.assertThat(transaction1.verify(), IsEqual.equalTo(true));

		final TransferTransaction transaction2 = (TransferTransaction) transactions.get(1);
		MatcherAssert.assertThat(transaction2.verify(), IsEqual.equalTo(true));
	}

	private Block createBlockForRoundTripTests(final boolean verifiable, final Account signer) {
		// Arrange:
		final Block originalBlock = BlockUtils.createBlock(null == signer ? Utils.generateRandomAccount() : signer);
		originalBlock.setLessor(Utils.generateRandomAccount());
		final TransferTransaction transaction1 = this.createSignedTransactionWithAmount(17);
		originalBlock.addTransaction(transaction1);

		final TransferTransaction transaction2 = this.createSignedTransactionWithAmount(1_000_000);
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
		final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
				Utils.generateRandomAccount(), Amount.fromNem(amount), null);
		transaction.sign();
		return transaction;
	}

	// endregion

	// region Transaction

	@Test
	public void singleTransactionCanBeAddedToBlock() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		final Transaction transaction = BlockUtils.createTransactionWithFee(17);

		// Act:
		block.addTransaction(transaction);

		// Assert:
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transaction));
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
		MatcherAssert.assertThat(block.getTransactions().size(), IsEqual.equalTo(2));
		MatcherAssert.assertThat((block.getTransactions().get(0)), IsEqual.equalTo(transactions.get(0)));
		MatcherAssert.assertThat((block.getTransactions().get(1)), IsEqual.equalTo(transactions.get(1)));
	}

	// endregion

	// region Fee

	@Test
	public void blockFeeIsSumOfTransactionFees() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		block.addTransaction(BlockUtils.createTransactionWithFee(17));
		block.addTransaction(BlockUtils.createTransactionWithFee(11));
		block.addTransactions(Arrays.asList((Transaction) BlockUtils.createTransactionWithFee(3), BlockUtils.createTransactionWithFee(50)));
		block.addTransaction(BlockUtils.createTransactionWithFee(22));

		// Assert:
		MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(new Amount(103L)));
	}

	@Test
	public void blockWithMultisigTransactionReturnsCorrectFee() {
		// Arrange:
		final Block block = BlockUtils.createBlock();

		final Transaction innerTransaction = BlockUtils.createTransactionWithFee(1_000000);
		final SimpleMultisigContext context = new SimpleMultisigContext(innerTransaction);
		final MultisigSignatureTransaction sig1 = context.createSignature();
		sig1.setFee(Amount.fromNem(3));
		final MultisigSignatureTransaction sig2 = context.createSignature();
		sig2.setFee(Amount.fromNem(5));
		final MultisigTransaction transaction = context.createMultisig();
		transaction.addSignature(sig1);
		transaction.addSignature(sig2);
		transaction.setFee(Amount.fromNem(130));

		// Act:
		block.addTransaction(transaction);

		// Assert:
		MatcherAssert.assertThat(block.getTotalFee(), IsEqual.equalTo(Amount.fromNem(1 + 3 + 5 + 130)));
	}

	// endregion

	// region equals / hashCode

	private static final Account DEFAULT_ACCOUNT = Utils.generateRandomAccount();

	@SuppressWarnings("serial")
	private static final Map<String, Block> DESC_TO_BLOCK_MAP = new HashMap<String, Block>() {
		{
			final Block defaultBlock = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(3));
			defaultBlock.sign();
			this.put("default", defaultBlock);

			final Block diffSignatureBlock = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(3));
			diffSignatureBlock.setSignature(new Signature(BigInteger.ONE, BigInteger.ONE));
			this.put("diff-signature", diffSignatureBlock);

			final Block diffHeightBlock = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(4));
			diffHeightBlock.sign();
			this.put("diff-height", diffHeightBlock);

			final Block diffHashBlock = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(6), new BlockHeight(3));
			diffHeightBlock.sign();
			this.put("diff-hash", diffHashBlock);

			final Block nullSignatureBlock = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(3));
			this.put("null-signature", nullSignatureBlock);
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Block block = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(3));
		block.sign();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("default"), IsEqual.equalTo(block));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-signature"), IsNot.not(IsEqual.equalTo(block)));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-height"), IsNot.not(IsEqual.equalTo(block)));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-hash"), IsNot.not(IsEqual.equalTo(block)));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("null-signature"), IsNot.not(IsEqual.equalTo(block)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(block)));
		MatcherAssert.assertThat(DEFAULT_ACCOUNT, IsNot.not(IsEqual.equalTo((Object) block)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Block block = new Block(DEFAULT_ACCOUNT, Hash.ZERO, Hash.ZERO, new TimeInstant(7), new BlockHeight(3));
		block.sign();
		final int hashCode = block.hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-signature").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-height").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("diff-hash").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_BLOCK_MAP.get("null-signature").hashCode(), IsEqual.equalTo(hashCode));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateRepresentation() {
		// Arrange:
		final Block block = BlockUtils.createBlock();
		block.addTransaction(BlockUtils.createTransactionWithFee(1));
		block.addTransaction(BlockUtils.createTransactionWithFee(7));

		// Assert:
		MatcherAssert.assertThat(block.toString(), IsEqual.equalTo("height: 3, #tx: 2"));
	}

	// endregion
}
