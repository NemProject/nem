package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.secret.BlockChainConstants;
import org.nem.nis.test.MockBlockScorer;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

public class BlockChainValidatorTest {

	//region block chain size

	@Test
	public void blockChainMustBeNoGreaterThanBlockLimit() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, createBlockList(parentBlock, 20)), IsEqual.equalTo(true));
		Assert.assertThat(validator.isValid(parentBlock, createBlockList(parentBlock, 21)), IsEqual.equalTo(true));
		Assert.assertThat(validator.isValid(parentBlock, createBlockList(parentBlock, 22)), IsEqual.equalTo(false));
	}

	//endregion

	//region block checks

	@Test
	public void allBlocksInChainMustHaveCorrectParentBlockHash() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		final Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
		blocks.add(block);
		blocks.add(createBlock(Utils.generateRandomAccount(), block));
		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	@Test
	public void invalidParentBlockHashInvalidatesChain() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		final Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
		final Block dummyPrevious = createBlock(Utils.generateRandomAccount(), parentBlock);
		blocks.add(block);
		blocks.add(createBlock(Utils.generateRandomAccount(), block));
		blocks.get(blocks.size() - 1).setPrevious(dummyPrevious);
		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHaveCorrectHeight() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		blocks.add(createBlock(Utils.generateRandomAccount(), 12));
		blocks.add(createBlock(Utils.generateRandomAccount(), 15));
		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHaveCorrectHeightInOrder() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		blocks.add(createBlock(Utils.generateRandomAccount(), 12));
		blocks.add(createBlock(Utils.generateRandomAccount(), 14));
		blocks.add(createBlock(Utils.generateRandomAccount(), 13));
		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustVerify() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 3);

		blocks.get(1).setSignature(new Signature(Utils.generateRandomBytes(64)));

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHit() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 3);

		scorer.setZeroTargetBlock(blocks.get(1));

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHaveValidTimestamp() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 3);
		final Block block = createFutureBlock(blocks.get(2));
		blocks.add(block);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAllBlockChecksPass() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 3);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	//endregion

	//region transaction checks

	@Test
	public void allTransactionsInChainMustVerify() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 3);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidNonVerifiableTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allTransactionsInChainMustBeValid() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createInvalidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allTransactionsInChainMustHaveValidTimestamp() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createSignedFutureTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInABlockIsSignedByBlockHarvester() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createSignedTransactionWithGivenSender(middleBlock.getSigner()));
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}


	@Test
	public void chainIsValidIfTransactionAlreadyExistBeforeMarkerBlock() {
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidatorTrue(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), BlockMarkerConstants.FATAL_TX_BUG_HEIGHT - 3);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	@Test
	public void chainIsInvalidIfTransactionAlreadyExistAfterMarkerThread() {
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidatorTrue(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), BlockMarkerConstants.FATAL_TX_BUG_HEIGHT - 2);
		parentBlock.sign();

		final List<Block> blocks = createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAllTransactionChecksPass() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Account account = Utils.generateRandomAccount();
		final Block block = new Block(account, parentBlock, TimeInstant.ZERO);

		final Block middleBlock = new Block(account, block, TimeInstant.ZERO);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());

		final Block lastBlock = new Block(account, middleBlock, TimeInstant.ZERO);

		final List<Block> blocks = Arrays.asList(block, middleBlock, lastBlock);
		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}
	//endregion

	//region block execution

	@Test
	@SuppressWarnings("unchecked")
	public void validatorCallsExecuteOnEachBlock() {
		// Arrange:
		final Consumer<Block> executor = (Consumer<Block>)Mockito.mock(Consumer.class);
		final BlockChainValidator validator = createValidator(executor);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Block block1 = Mockito.spy(createBlock(Utils.generateRandomAccount(), parentBlock));
		final Block block2 = Mockito.spy(createBlock(Utils.generateRandomAccount(), block1));
		final List<Block> blocks = Arrays.asList(block1, block2);
		signAllBlocks(blocks);

		// Act:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));

		// Assert:
		Mockito.verify(executor, Mockito.times(1)).accept(block1);
		Mockito.verify(executor, Mockito.times(1)).accept(block2);
	}

	//endregion

	//region helper functions

	private static List<Block> createBlockList(Block parent, final int numBlocks) {
		final List<Block> blocks = new ArrayList<>();
		final Account account = Utils.generateRandomAccount();
		for (int i = 0; i < numBlocks; ++i) {
			final Block block = new Block(account, parent, TimeInstant.ZERO);
			blocks.add(block);
			parent = block;
		}

		signAllBlocks(blocks);
		return blocks;
	}

	private static void signAllBlocks(final List<Block> blocks) {
		for (final Block block : blocks) {
			block.sign();
		}
	}

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static Transaction createInvalidSignedTransaction() {
		final Transaction transaction = new MockTransaction();
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() - 1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createValidNonVerifiableTransaction() {
		final TimeInstant timeStamp = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(15);
		final MockTransaction transaction = new MockTransaction(/* custom field */ 12, timeStamp);
		transaction.setDeadline(timeStamp.addSeconds(1));
		transaction.setSignature(new Signature(Utils.generateRandomBytes(64)));
		return transaction;
	}

	private static MockTransaction createValidSignedTransaction() {
		final TimeInstant timeStamp = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(15);
		final MockTransaction transaction = new MockTransaction(/* custom field */ 12, timeStamp);
		transaction.setDeadline(timeStamp.addSeconds(1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedFutureTransaction() {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new MockTransaction(0, currentTime.addMinutes(2));
		transaction.setDeadline(currentTime.addHours(2));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedTransactionWithGivenSender(final Account account) {
		final Transaction transaction = new MockTransaction(account);
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() + 1));
		transaction.sign();
		return transaction;
	}

	private static BlockScorer createMockBlockScorer() {
		final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		Mockito.when(scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
		Mockito.when(scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);
		return scorer;
	}

	private static BlockChainValidator createValidator(final BlockScorer scorer) {
		return new BlockChainValidator(block -> { }, scorer, 21, o -> false);
	}

	private static BlockChainValidator createValidator(final Consumer<Block> blockExecutor) {
		return new BlockChainValidator(blockExecutor, createMockBlockScorer(), 21, o -> false);
	}

	private static BlockChainValidator createValidatorTrue(final BlockScorer scorer) {
		return new BlockChainValidator(block -> { }, scorer, 21, o -> true);
	}

	private static BlockChainValidator createValidator() {
		return createValidator(new MockBlockScorer());
	}

	private static Block createBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Block createBlock(final Account account, final Block parentBlock) {
		return new Block(account, parentBlock, TimeInstant.ZERO);
	}

	//endregion
}
