package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.chain.BlockProcessor;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

public class BlockChainValidatorTest {

	// region block chain size

	@Test
	public void blockChainSizeCanBeLessThanBlockLimit() {
		// Assert:
		assertBlockChainSizeValidationResult(20, ValidationResult.SUCCESS);
	}

	@Test
	public void blockChainSizeCanBeEqualToBlockLimit() {
		// Assert:
		assertBlockChainSizeValidationResult(21, ValidationResult.SUCCESS);
	}

	@Test
	public void blockChainSizeCannotBeGreaterThanBlockLimit() {
		// Assert:
		assertBlockChainSizeValidationResult(22, ValidationResult.FAILURE_MAX_CHAIN_SIZE_EXCEEDED);
	}

	private static void assertBlockChainSizeValidationResult(final int size, final ValidationResult expectedResult) {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, NisUtils.createBlockList(parentBlock, size));

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	// region block checks

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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
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

		// resign all blocks but don't call NisUtils.signAllBlocks because this test is explicitly
		// testing an invalid previous block
		blocks.forEach(VerifiableEntity::sign);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_BLOCK_UNVERIFIABLE));
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_BLOCK_UNEXPECTED_HEIGHT));
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_BLOCK_UNEXPECTED_HEIGHT));
	}

	@Test
	public void allBlocksInChainMustVerify() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		blocks.get(1).setSignature(new Signature(Utils.generateRandomBytes(64)));

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_BLOCK_UNVERIFIABLE));
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_BLOCK_NOT_HIT));
	}

	@Test
	public void chainIsValidIfAllBlockChecksPass() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region transaction checks

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

		// - repair the chain after changes to the middle block
		blocks.set(2, new Block(Utils.generateRandomAccount(), middleBlock, TimeInstant.ZERO));
		blocks.get(2).sign();

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_UNVERIFIABLE));
	}

	@Test
	public void transactionsAtTreasuryReissuanceForkHeightDoNotNeedToVerify() {
		// Arrange:
		final BlockChainValidator validator = createValidator(
				new ForkConfiguration(new BlockHeight(100), new ArrayList<Hash>(), new ArrayList<Hash>()));
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 98);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.addTransaction(createValidNonVerifiableTransaction());
		middleBlock.addTransaction(createValidSignedTransaction());
		middleBlock.sign();

		// - repair the chain after changes to the middle block
		blocks.set(2, new Block(Utils.generateRandomAccount(), middleBlock, TimeInstant.ZERO));
		blocks.get(2).sign();

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void allTransactionsInChainMustBeValidAndPassSingleValidation() {
		// Arrange:
		final SingleTransactionValidator transactionValidator = Mockito.mock(SingleTransactionValidator.class);
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		factory.transactionValidator = transactionValidator;
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = createBlocksForTransactionValidationTests(parentBlock);

		Mockito.when(transactionValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		Mockito.when(transactionValidator.validate(Mockito.eq(blocks.get(1).getTransactions().get(1)), Mockito.any()))
				.thenReturn(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert: (validation should short circuit after the first failure even though there are three transactions)
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_FUTURE_DEADLINE));
		Mockito.verify(transactionValidator, Mockito.times(2)).validate(Mockito.any(), Mockito.any());
	}

	private static List<Block> createBlocksForTransactionValidationTests(final Block parentBlock) {
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		middleBlock.addTransaction(createValidSignedTransaction(10));
		middleBlock.addTransaction(createValidSignedTransaction(11));
		middleBlock.addTransaction(createValidSignedTransaction(12));
		middleBlock.sign();
		return blocks;
	}

	@Test
	public void chainIsInvalidIfOneBlockContainsTheSameTransactionTwice() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block middleBlock = blocks.get(1);
		final MockTransaction tx = createValidSignedTransaction();
		middleBlock.addTransaction(tx);
		middleBlock.addTransaction(tx);
		middleBlock.sign();

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN));
	}

	@Test
	public void chainIsInvalidIfTwoBlocksContainTheSameTransaction() {
		// Arrange:
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN));
	}

	@Test
	public void chainIsInvalidIfTwoBlocksContainTheSameChildTransaction() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
		parentBlock.sign();

		final MockTransaction childTx = createValidSignedTransaction();
		final MockTransaction tx1 = createValidSignedTransaction();
		tx1.setChildTransactions(Collections.singletonList(childTx));
		tx1.sign();

		final MockTransaction tx2 = createValidSignedTransaction();
		tx2.setChildTransactions(Collections.singletonList(childTx));
		tx2.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block1 = blocks.get(1);
		block1.addTransaction(tx1);
		final Block block2 = blocks.get(2);
		block2.setPrevious(block1);
		block2.addTransaction(tx2);
		NisUtils.signAllBlocks(blocks);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_DUPLICATE_IN_CHAIN));
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

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	// endregion

	// region block execution

	@Test
	public void executorIsCreatedForEachBlockInChain() {
		// Arrange:
		final TestContextForExecutorTests context = new TestContextForExecutorTests();

		// Act:
		context.validate();

		// Assert:
		MatcherAssert.assertThat(context.numExecutorFactoryCalls[0], IsEqual.equalTo(2));
		MatcherAssert.assertThat(context.heights, IsEqual.equalTo(Arrays.asList(new BlockHeight(12), new BlockHeight(13))));
	}

	@Test
	public void executorIsCalledForEachBlockInChain() {
		// Arrange:
		final TestContextForExecutorTests context = new TestContextForExecutorTests();

		// Act:
		context.validate();

		// Assert:
		Mockito.verify(context.processor, Mockito.times(2)).process();
	}

	@Test
	public void executorIsCalledForEachTransactionInChain() {
		// Arrange:
		final TestContextForExecutorTests context = new TestContextForExecutorTests();

		// Act:
		context.validate();

		// Assert:
		Mockito.verify(context.processor, Mockito.times(5)).process(Mockito.any());
		final List<Transaction> allTransactions = new ArrayList<>();
		allTransactions.addAll(context.block1.getTransactions());
		allTransactions.addAll(context.block2.getTransactions());
		for (final Transaction transaction : allTransactions) {
			Mockito.verify(context.processor, Mockito.times(1)).process(transaction);
		}
	}

	private static class TestContextForExecutorTests {
		private final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		private final BlockProcessor processor = Mockito.mock(BlockProcessor.class);
		private final int[] numExecutorFactoryCalls = new int[]{
				0
		};
		private final List<BlockHeight> heights = new ArrayList<>();

		private final BlockChainValidator validator;
		private final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		private final Block block1;
		private final Block block2;
		private final List<Block> blocks;

		public TestContextForExecutorTests() {
			this.factory.processorFactory = block -> {
				++this.numExecutorFactoryCalls[0];
				this.heights.add(block.getHeight());
				return this.processor;
			};

			this.validator = this.factory.create();

			this.parentBlock.sign();

			this.block1 = createBlock(Utils.generateRandomAccount(), this.parentBlock);
			this.block1.addTransaction(createValidSignedTransaction());
			this.block1.addTransaction(createValidSignedTransaction());
			this.block1.addTransaction(createValidSignedTransaction());

			this.block2 = createBlock(Utils.generateRandomAccount(), this.block1);
			this.block2.addTransaction(createValidSignedTransaction());
			this.block2.addTransaction(createValidSignedTransaction());

			this.blocks = Arrays.asList(this.block1, this.block2);
			NisUtils.signAllBlocks(this.blocks);
		}

		public void validate() {
			// Act:
			final ValidationResult result = this.validator.isValid(this.parentBlock, this.blocks);

			// Assert:
			MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		}
	}

	// endregion

	// region transaction validate arguments - single validation

	@Test
	public void singleTransactionValidationContextConfirmedBlockHeightIsConstant() {
		// Act:
		final ArgumentCaptor<ValidationContext> contextCaptor = captureValidationContext(11);

		// Assert:
		for (int i = 0; i < contextCaptor.getAllValues().size(); ++i) {
			MatcherAssert.assertThat(contextCaptor.getAllValues().get(i).getConfirmedBlockHeight(), IsEqual.equalTo(new BlockHeight(11)));
		}
	}

	@Test
	public void singleTransactionValidationContextCurrentBlockHeightIsIncrementingPerBlock() {
		// Act:
		final ArgumentCaptor<ValidationContext> contextCaptor = captureValidationContext(11);

		// Assert:
		for (int i = 0; i < contextCaptor.getAllValues().size(); ++i) {
			MatcherAssert.assertThat(contextCaptor.getAllValues().get(i).getBlockHeight(), IsEqual.equalTo(new BlockHeight(12 + i / 2)));
		}
	}

	@Test
	public void singleTransactionValidationContextContainsValidationStatePassedToConstructor() {
		// Act:
		final BlockChainValidatorFactory factory = new BlockChainValidatorFactory();
		final ArgumentCaptor<ValidationContext> contextCaptor = captureValidationContext(factory, 11);

		// Assert:
		for (int i = 0; i < contextCaptor.getAllValues().size(); ++i) {
			MatcherAssert.assertThat(contextCaptor.getAllValues().get(i).getState(), IsEqual.equalTo(factory.validationState));
		}
	}

	private static ArgumentCaptor<ValidationContext> captureValidationContext(final long parentBlockHeight) {
		return captureValidationContext(new BlockChainValidatorFactory(), parentBlockHeight);
	}

	private static ArgumentCaptor<ValidationContext> captureValidationContext(final BlockChainValidatorFactory factory,
			final long parentBlockHeight) {
		// Arrange:
		final SingleTransactionValidator transactionValidator = Mockito.mock(SingleTransactionValidator.class);
		Mockito.when(transactionValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		factory.transactionValidator = transactionValidator;

		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), parentBlockHeight);
		parentBlock.sign();

		final List<Block> blocks = createBlocksForValidationContextCaptureTests(parentBlock);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));

		final ArgumentCaptor<ValidationContext> contextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
		Mockito.verify(transactionValidator, Mockito.times(6)).validate(Mockito.any(), contextCaptor.capture());
		return contextCaptor;
	}

	// endregion

	private static List<Block> createBlocksForValidationContextCaptureTests(final Block parentBlock) {
		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		Block previousBlock = null;
		int id = 0;
		for (final Block block : blocks) {
			if (null != previousBlock) {
				block.setPrevious(previousBlock);
			}

			block.addTransaction(createValidSignedTransaction(++id));
			block.addTransaction(createValidSignedTransaction(++id));
			block.sign();

			previousBlock = block;
		}

		return blocks;
	}

	// region helper functions - transactions

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

	// endregion

	// region helper functions - blocks

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

	// endregion

	private static class BlockChainValidatorFactory {
		public Function<Block, BlockProcessor> processorFactory = block -> Mockito.mock(BlockProcessor.class);
		public BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public final int maxChainSize = 21;
		public BlockValidator blockValidator = Mockito.mock(BlockValidator.class);
		public SingleTransactionValidator transactionValidator = Mockito.mock(SingleTransactionValidator.class);
		public final ValidationState validationState = Mockito.mock(ValidationState.class);

		public BlockChainValidatorFactory() {
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);

			Mockito.when(this.blockValidator.validate(Mockito.any())).thenReturn(ValidationResult.SUCCESS);
			Mockito.when(this.transactionValidator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);
		}

		public BlockChainValidator create() {
			return create(new ForkConfiguration());
		}

		public BlockChainValidator create(final ForkConfiguration forkConfiguration) {
			return new BlockChainValidator(this.processorFactory, this.scorer, this.maxChainSize, this.blockValidator,
					this.transactionValidator, this.validationState, forkConfiguration);
		}
	}

	private static BlockChainValidator createValidator() {
		return createValidator(new ForkConfiguration());
	}

	private static BlockChainValidator createValidator(final ForkConfiguration forkConfiguration) {
		return new BlockChainValidatorFactory().create(forkConfiguration);
	}
}
