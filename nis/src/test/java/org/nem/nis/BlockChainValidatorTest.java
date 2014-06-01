package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
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
	public void chainIsValidIfAllTransactionChecksPass() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidator validator = createValidator(scorer);
		final Block parentBlock = createBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		final Account account = Utils.generateRandomAccount();
		final Block block = new Block(account, parentBlock, TimeInstant.ZERO);
		blocks.add(block);


		final Block middleBlock = new Block(account, block, TimeInstant.ZERO);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		blocks.add(middleBlock);

		final Block lastBlock = new Block(account, middleBlock, TimeInstant.ZERO);
		blocks.add(lastBlock);

		signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
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

	private static Transaction createInvalidSignedTransaction() {
		final Transaction transaction =  new MockTransaction();
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() - 1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createValidNonVerifiableTransaction() {
		final Transaction transaction =  new MockTransaction();
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() + 1));
		transaction.setSignature(new Signature(Utils.generateRandomBytes(64)));
		return transaction;
	}

	private static Transaction createValidSignedTransaction() {
		final Transaction transaction =  new MockTransaction();
		transaction.setDeadline(new TimeInstant(MockTransaction.TIMESTAMP.getRawTime() + 1));
		transaction.sign();
		return transaction;
	}

	private static BlockChainValidator createValidator(final BlockScorer scorer) {
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		// TODO: ugly
		Mockito.when(accountAnalyzer.findByAddress(Mockito.anyObject())).thenReturn(Utils.generateRandomAccount());
		return new BlockChainValidator(accountAnalyzer, scorer, 21);
	}

	private static BlockChainValidator createValidator() {
		return createValidator(new MockBlockScorer());
	}

	private static Block createBlock(final Account account, long height) {
		Block block = new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
		return block;
	}

	private static Block createBlock(final Account account, Block parentBlock) {
		Block block = new Block(account, parentBlock, TimeInstant.ZERO);
		return block;
	}

	//endregion
}
