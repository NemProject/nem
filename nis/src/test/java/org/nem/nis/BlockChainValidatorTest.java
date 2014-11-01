package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

public class BlockChainValidatorTest {

	//region block chain size

	@Test
	public void blockChainMustBeNoGreaterThanBlockLimit() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, NisUtils.createBlockList(parentBlock, 20)), IsEqual.equalTo(true));
		Assert.assertThat(validator.isValid(parentBlock, NisUtils.createBlockList(parentBlock, 21)), IsEqual.equalTo(true));
		Assert.assertThat(validator.isValid(parentBlock, NisUtils.createBlockList(parentBlock, 22)), IsEqual.equalTo(false));
	}

	//endregion

	//region block checks

	@Test
	public void allBlocksInChainMustHaveCorrectParentBlockHash() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		final Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
		blocks.add(block);
		blocks.add(createBlock(Utils.generateRandomAccount(), block));
		NisUtils.signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	@Test
	public void invalidParentBlockHashInvalidatesChain() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		final Block block = createBlock(Utils.generateRandomAccount(), parentBlock);
		final Block dummyPrevious = createBlock(Utils.generateRandomAccount(), parentBlock);
		blocks.add(block);
		blocks.add(createBlock(Utils.generateRandomAccount(), block));
		blocks.get(blocks.size() - 1).setPrevious(dummyPrevious);
		NisUtils.signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHaveCorrectHeight() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		blocks.add(createBlock(Utils.generateRandomAccount(), parentBlock, 12));
		blocks.add(createBlock(Utils.generateRandomAccount(), blocks.get(0), 15));
		NisUtils.signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustHaveCorrectHeightInOrder() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = new ArrayList<>();
		blocks.add(createBlock(Utils.generateRandomAccount(), parentBlock, 12));
		blocks.add(createBlock(Utils.generateRandomAccount(), blocks.get(0), 14));
		blocks.add(createBlock(Utils.generateRandomAccount(), blocks.get(1), 13));
		NisUtils.signAllBlocks(blocks);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustVerify() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		blocks.get(1).setSignature(new Signature(Utils.generateRandomBytes(64)));

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void allBlocksInChainMustPassValidation() {
		// Arrange:
		final BlockValidator blockValidator = Mockito.mock(BlockValidator.class);
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.blockValidator = blockValidator;
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		Mockito.when(blockValidator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		Mockito.when(blockValidator.validate(Mockito.eq(blocks.get(1)))).thenReturn(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
		Mockito.verify(blockValidator, Mockito.times(2)).validate(Mockito.any());
	}

	@Test
	public void allBlocksInChainMustHit() {
		// Arrange:
		final MockBlockScorer scorer = new MockBlockScorer();
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.scorer = scorer;
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		scorer.setZeroTargetBlock(blocks.get(1));

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAllBlockChecksPass() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));
	}

	//endregion

	//region transaction checks

	@Test
	public void allTransactionsInChainMustVerify() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
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
		final TransactionValidator transactionValidator = Mockito.mock(TransactionValidator.class);
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.transactionValidator = transactionValidator;
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction(10));
		middleBlock.addTransaction(createValidSignedTransaction(11));
		middleBlock.addTransaction(createValidSignedTransaction(12));
		middleBlock.sign();

		Mockito.when(transactionValidator.validate(Mockito.any(), Mockito.any()))
				.thenReturn(ValidationResult.SUCCESS);
		Mockito.when(transactionValidator.validate(Mockito.eq(middleBlock.getTransactions().get(1)), Mockito.any()))
				.thenReturn(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
		Mockito.verify(transactionValidator, Mockito.times(2)).validate(Mockito.any(), Mockito.any());
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInABlockIsSignedByBlockHarvester() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createSignedTransactionWithGivenSender(middleBlock.getSigner()));
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// Assert:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfOneBlockContainsTheSameTransactionTwice() {
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		final MockTransaction tx = createValidSignedTransaction();
		middleBlock.addTransaction(tx);
		middleBlock.addTransaction(tx);
		middleBlock.sign();

		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsInvalidIfTwoBlocksContainTheSameTransaction() {
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final MockTransaction tx = createValidSignedTransaction();
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block1 = blocks.get(1);
		block1.addTransaction(tx);
		final Block block2 = blocks.get(2);
		block2.setPrevious(block1);
		block2.addTransaction(tx);
		NisUtils.signAllBlocks(blocks);

		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(false));
	}

	@Test
	public void chainIsValidIfAllTransactionChecksPass() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Account account = Utils.generateRandomAccount();
		final Block block = new Block(account, parentBlock, TimeInstant.ZERO);

		final Block middleBlock = new Block(account, block, TimeInstant.ZERO);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());

		final Block lastBlock = new Block(account, middleBlock, TimeInstant.ZERO);

		final List<Block> blocks = Arrays.asList(block, middleBlock, lastBlock);
		NisUtils.signAllBlocks(blocks);

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
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.executor = executor;
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final Block block1 = Mockito.spy(createBlock(Utils.generateRandomAccount(), parentBlock));
		final Block block2 = Mockito.spy(createBlock(Utils.generateRandomAccount(), block1));
		final List<Block> blocks = Arrays.asList(block1, block2);
		NisUtils.signAllBlocks(blocks);

		// Act:
		Assert.assertThat(validator.isValid(parentBlock, blocks), IsEqual.equalTo(true));

		// Assert:
		Mockito.verify(executor, Mockito.times(1)).accept(block1);
		Mockito.verify(executor, Mockito.times(1)).accept(block2);
	}

	//endregion

	//region validation context

	@Test
	public void validationContextConfirmedBlockHeightIsConstant() {
		// Act:
		final ArgumentCaptor<ValidationContext> contextCaptor = captureValidationContext(11);

		// Assert:
		for (int i = 0; i < contextCaptor.getAllValues().size(); ++i) {
			Assert.assertThat(
					contextCaptor.getAllValues().get(i).getConfirmedBlockHeight(),
					IsEqual.equalTo(new BlockHeight(11)));
		}
	}

	@Test
	public void validationContextCurrentBlockHeightIsIncrementingPerBlock() {
		// Act:
		final ArgumentCaptor<ValidationContext> contextCaptor = captureValidationContext(11);

		// Assert:
		for (int i = 0; i < contextCaptor.getAllValues().size(); ++i) {
			Assert.assertThat(
					contextCaptor.getAllValues().get(i).getBlockHeight(),
					IsEqual.equalTo(new BlockHeight(12 + i / 2)));
		}
	}

	private static ArgumentCaptor<ValidationContext> captureValidationContext(final long parentBlockHeight) {
		// Arrange:
		final TransactionValidator transactionValidator = Mockito.mock(TransactionValidator.class);
		Mockito.when(transactionValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.transactionValidator = transactionValidator;

		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), parentBlockHeight);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		Block previousBlock = null;
		for (final Block block : blocks) {
			if (null != previousBlock) {
				block.setPrevious(previousBlock);
			}

			block.addTransaction(createValidSignedTransaction());
			block.addTransaction(createValidSignedTransaction());
			block.sign();

			previousBlock = block;
		}

		// Act:
		final boolean result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));

		final ArgumentCaptor<ValidationContext> contextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
		Mockito.verify(transactionValidator, Mockito.times(6)).validate(Mockito.any(), contextCaptor.capture());
		return contextCaptor;
	}

	//endregion

	//region helper functions

	//region transactions

	private static Transaction createValidNonVerifiableTransaction() {
		final TimeInstant timeStamp = new TimeInstant(15);
		final MockTransaction transaction = new MockTransaction(/* custom field */ 12, timeStamp);
		transaction.setDeadline(timeStamp.addSeconds(1));
		transaction.setSignature(new Signature(Utils.generateRandomBytes(64)));
		return transaction;
	}

	private static MockTransaction createValidSignedTransaction() {
		return createValidSignedTransaction(12);
	}

	private static MockTransaction createValidSignedTransaction(final int customField) {
		final TimeInstant timeInstant = new TimeInstant(15);
		final MockTransaction transaction = new MockTransaction(customField, timeInstant);
		transaction.setDeadline(timeInstant.addSeconds(1));
		transaction.sign();
		return transaction;
	}

	private static Transaction createSignedTransactionWithGivenSender(final Account account) {
		final Transaction transaction = new MockTransaction(account);
		transaction.setDeadline(new TimeInstant(16));
		transaction.sign();
		return transaction;
	}

	//endregion

	//region blocks

	private static Block createParentBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Block createBlock(final Account account, final Block parentBlock, final long height) {
		final Block block = createParentBlock(account, height);
		block.setPrevious(parentBlock);
		return block;
	}

	private static Block createBlock(final Account account, final Block parentBlock) {
		return new Block(account, parentBlock, TimeInstant.ZERO);
	}

	//endregion

	private static class BlockChainValidatorFactory {
		public Consumer<Block> executor = block -> { };
		public BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public int maxChainSize = 21;
		public BlockValidator blockValidator = Mockito.mock(BlockValidator.class);
		public BatchTransactionValidator transactionValidator = Mockito.mock(TransactionValidator.class);

		public BlockChainValidatorFactory() {
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);

			Mockito.when(this.blockValidator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);

			Mockito.when(this.transactionValidator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		}

		public BlockChainValidator create() {
			return new BlockChainValidator(
					this.executor,
					this.scorer,
					this.maxChainSize,
					this.blockValidator,
					this.transactionValidator);
		}
	}

	private static BlockChainValidator createValidator() {
		return new BlockChainValidatorFactory().create();
	}

	//endregion
}
