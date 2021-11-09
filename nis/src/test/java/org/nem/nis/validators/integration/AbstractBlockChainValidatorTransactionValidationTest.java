package org.nem.nis.validators.integration;

import net.minidev.json.JSONObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.JsonSerializer;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.cache.*;
import org.nem.nis.chain.BlockExecuteProcessor;
import org.nem.nis.secret.*;
import org.nem.nis.state.ReadOnlyAccountInfo;
import org.nem.nis.test.NisUtils;
import org.nem.nis.ForkConfiguration;

import java.math.BigInteger;
import java.util.*;

public abstract class AbstractBlockChainValidatorTransactionValidationTest extends AbstractTransactionValidationTest {

	@Test
	public void multisigTransferHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.fromNem(200));
		final Account recipient = context.addAccount(Amount.ZERO);

		// Act:
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, recipient);
		final NisCache nisCache = context.nisCache.copy();
		final ValidationResult result = this.validateTransactions(nisCache, Collections.singletonList(t1));

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) = 690
		// - C 200 - 0 (No Change) = 200
		// - R 0 + 100 (Inner T amount) = 100
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(getAccountInfo(nisCache, multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(690)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(200)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	@Test
	public void multisigTransferWithMultipleSignaturesHasFeesAndAmountsDeductedFromMultisigAccount() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account multisig = context.addAccount(Amount.fromNem(1000));
		final Account cosigner = context.addAccount(Amount.fromNem(201));
		final Account cosigner2 = context.addAccount(Amount.fromNem(202));
		final Account cosigner3 = context.addAccount(Amount.fromNem(203));
		final Account recipient = context.addAccount(Amount.ZERO);

		// Act:
		final Transaction t1 = createMultisigWithSignatures(context, multisig, cosigner, Arrays.asList(cosigner2, cosigner3), recipient);
		final NisCache nisCache = context.nisCache.copy();
		final ValidationResult result = this.validateTransactions(nisCache, Collections.singletonList(t1));

		// Assert:
		// - M 1000 - 200 (Outer MT fee) - 10 (Inner T fee) - 100 (Inner T amount) - 2 * 6 (Signature fee) = 678
		// - C1 201 - 0 (No Change) = 201
		// - C2 202 - 0 (No Change) = 202
		// - C3 203 - 0 (No Change) = 203
		// - R 0 + 100 (Inner T amount) = 100
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(getAccountInfo(nisCache, multisig).getBalance(), IsEqual.equalTo(Amount.fromNem(678)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, cosigner).getBalance(), IsEqual.equalTo(Amount.fromNem(201)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, cosigner2).getBalance(), IsEqual.equalTo(Amount.fromNem(202)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, cosigner3).getBalance(), IsEqual.equalTo(Amount.fromNem(203)));
		MatcherAssert.assertThat(getAccountInfo(nisCache, recipient).getBalance(), IsEqual.equalTo(Amount.fromNem(100)));
	}

	@Test
	public void allBlocksInChainMustHaveValidVersion() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block = createBlockWithVersion(blocks.get(2), 2);
		blocks.add(block);

		// Act:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(context.nisCache.copy());
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
	}

	@Override
	protected void assertTransactions(final BlockHeight chainHeight, final ReadOnlyNisCache nisCache, final List<Transaction> all,
			final List<Transaction> expectedFiltered, final ValidationResult expectedResult) {
		// Act:
		final ValidationResult result = this.validateTransactions(chainHeight, nisCache.copy(), all);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	protected abstract List<Block> getBlocks(final Block parentBlock, final List<Transaction> transactions);

	private ValidationResult validateTransactions(final NisCache nisCache, final List<Transaction> all) {
		return this.validateTransactions(new BlockHeight(511000), nisCache, all);
	}

	private ValidationResult validateTransactions(final BlockHeight chainHeight, final NisCache nisCache, final List<Transaction> all) {
		// Arrange:
		final BlockChainValidator validator = new BlockChainValidatorFactory().create(nisCache);

		final Block parentBlock = NisUtils.createParentBlock(Utils.generateRandomAccount(), chainHeight.getRaw());
		parentBlock.sign();

		final List<Block> blocks = this.getBlocks(parentBlock, all);
		NisUtils.signAllBlocks(blocks);

		// Act:
		return validator.isValid(parentBlock, blocks);
	}

	private static ReadOnlyAccountInfo getAccountInfo(final ReadOnlyNisCache nisCache, final Account account) {
		return nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo();
	}

	private static Block createBlockWithVersion(final Block parentBlock, final int version) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block blockTemplate = new Block(Utils.generateRandomAccount(), parentBlock, currentTime);

		// change version
		final JSONObject jsonObject = JsonSerializer.serializeToJson(blockTemplate.asNonVerifiable());
		jsonObject.put("version", version | NetworkInfos.getDefault().getVersion() << 24);
		final Block block = new Block(blockTemplate.getType(), VerifiableEntity.DeserializationOptions.NON_VERIFIABLE,
				Utils.createDeserializer(jsonObject));
		block.signBy(blockTemplate.getSigner());
		return block;
	}

	protected static class BlockChainValidatorFactory {
		public final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public final int maxChainSize = 21;

		public BlockChainValidatorFactory() {
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);
		}

		public BlockChainValidator create(final NisCache nisCache) {
			final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(nisCache);
			return new BlockChainValidator(block -> new BlockExecuteProcessor(nisCache, block, observer), this.scorer, this.maxChainSize,
					NisUtils.createBlockValidatorFactory().create(nisCache),
					NisUtils.createTransactionValidatorFactory().createSingle(nisCache), NisCacheUtils.createValidationState(nisCache),
					new ForkConfiguration());
		}
	}
}
