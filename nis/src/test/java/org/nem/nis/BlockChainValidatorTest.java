package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.PoiImportanceGenerator;
import org.nem.nis.test.MockBlockScorer;

import java.util.*;

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
		Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
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
		Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
		Block dummyPrevious = createBlock(Utils.generateRandomAccount(), parentBlock);
		blocks.add(block);
		blocks.add(createBlock(Utils.generateRandomAccount(), block));
		blocks.get(blocks.size()-1).setPrevious(dummyPrevious);
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
		Block block = createFutureBlock(blocks.get(2));
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
	public void validatorCallsExecuteOnEachBlock() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Block block1 = Mockito.spy(createBlock(Utils.generateRandomAccount(), parentBlock));
		final Block block2 = Mockito.spy(createBlock(Utils.generateRandomAccount(), block1));
		final List<Block> blocks = Arrays.asList(block1, block2);
		signAllBlocks(blocks);

		// Act:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));

		// Assert:
		Mockito.verify(block1, Mockito.times(1)).execute();
		Mockito.verify(block2, Mockito.times(1)).execute();
	}

	@Test
	public void validatorUpdatesAccountHeights() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(Mockito.mock(PoiImportanceGenerator.class));
		final Account recipient1 = accountAnalyzer.addAccountToCache(Utils.generateRandomAddress());
		final Account recipient2 = accountAnalyzer.addAccountToCache(Utils.generateRandomAddress());
		final Account recipient3 = accountAnalyzer.addAccountToCache(Utils.generateRandomAddress());
		final Account recipient4 = accountAnalyzer.addAccountToCache(Utils.generateRandomAddress());

		final BlockChainValidator validator = createValidator(accountAnalyzer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Block block1 = createSignedBlockWithTransfers(parentBlock, recipient1);
		final Block block2 = createSignedBlockWithTransfers(block1, recipient2, recipient3);
		final Block block3 = createSignedBlockWithTransfers(block2, recipient4, recipient1);
		final List<Block> blocks = Arrays.asList(block1, block2, block3);

		// Act:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));

		// Assert:
		Assert.assertThat(recipient1.getHeight(), IsEqual.equalTo(new BlockHeight(12)));
		Assert.assertThat(recipient2.getHeight(), IsEqual.equalTo(new BlockHeight(13)));
		Assert.assertThat(recipient3.getHeight(), IsEqual.equalTo(new BlockHeight(13)));
		Assert.assertThat(recipient4.getHeight(), IsEqual.equalTo(new BlockHeight(14)));
	}

	private static Block createSignedBlockWithTransfers(final Block parentBlock, final Account... recipients) {
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, TimeInstant.ZERO);

		final Account sender = Utils.generateRandomAccount();
		final Amount senderAmount = Amount.fromNem(100 * recipients.length);
		sender.incrementBalance(senderAmount);
		sender.getWeightedBalances().addReceive(BlockHeight.ONE, senderAmount);

		for (final Account recipient : recipients) {
			final MockTransaction transaction = createValidSignedTransaction();
			transaction.setTransferAction(observer -> observer.notifyTransfer(sender, recipient, Amount.fromNem(90)));
			block.addTransaction(transaction);
		}

		block.sign();
		return block;
	}

	//endregion

	//region helper functions

	private static List<Block> createBlockList(Block parent, int numBlocks) {
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
		for (final Block block : blocks)
			block.sign();
	}

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static Transaction createInvalidSignedTransaction() {
		final Transaction transaction =  new MockTransaction();
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
		final Transaction transaction =  new MockTransaction(0, currentTime.addMinutes(2));
		transaction.setDeadline(currentTime.addHours(2));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedTransactionWithGivenSender(final Account account) {
		final Transaction transaction =  new MockTransaction(account);
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() + 1));
		transaction.sign();
		return transaction;
	}

	private static BlockChainValidator createValidator(final BlockScorer scorer) {
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		Mockito.when(accountAnalyzer.findByAddress(Mockito.anyObject())).thenReturn(Utils.generateRandomAccount());
		return new BlockChainValidator(accountAnalyzer, scorer, 21);
	}

	private static BlockChainValidator createValidator(final AccountAnalyzer accountAnalyzer) {
		return new BlockChainValidator(accountAnalyzer, new MockBlockScorer(), 21);
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
