package org.nem.nis.test.validation;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.chain.BlockExecuteProcessor;
import org.nem.nis.secret.*;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.test.NisUtils;

import java.math.BigInteger;
import java.util.List;

public abstract class AbstractBlockChainValidatorTransactionValidationTest extends AbstractTransactionValidationTest {

	@Test
	public void allBlocksInChainMustHaveValidTimestamp() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block = createFutureBlock(blocks.get(2));
		blocks.add(block);

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE));
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInBlockIsSignedByBlockHarvester() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(context.createValidSignedTransaction());
		block.addTransaction(context.createValidSignedTransaction(block.getSigner()));
		block.addTransaction(context.createValidSignedTransaction());
		block.sign();

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
	}

	@Override
	protected void assertTransactions(
			final ReadOnlyNisCache nisCache,
			final List<Transaction> all,
			final List<Transaction> expectedFiltered,
			final ValidationResult expectedResult) {
		// Arrange:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(nisCache.copy());

		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), BlockMarkerConstants.BETA_EXECUTION_CHANGE_FORK);
		parentBlock.sign();

		final List<Block> blocks = this.getBlocks(parentBlock, all);
		NisUtils.signAllBlocks(blocks);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	protected abstract List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions);

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static class BlockChainValidatorFactory {
		public final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public final int maxChainSize = 21;

		public BlockChainValidatorFactory() {
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);
		}

		public BlockChainValidator create(final NisCache nisCache) {
			final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(nisCache);
			return new BlockChainValidator(
					block -> new BlockExecuteProcessor(nisCache, block, observer),
					this.scorer,
					this.maxChainSize,
					NisUtils.createBlockValidatorFactory().create(nisCache),
					NisUtils.createTransactionValidatorFactory().createSingle(nisCache.getAccountStateCache()),
					new DefaultDebitPredicate(nisCache.getAccountStateCache()));
		}
	}
}
