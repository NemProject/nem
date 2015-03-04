package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.chain.BlockExecuteProcessor;
import org.nem.nis.secret.*;
import org.nem.nis.state.*;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * This suite is different from BlockChainValidatorTest because it uses REAL validators
 * (or at least very close to real).
 */
public class BlockChainValidatorIntegrationTest {

	@Test
	public void allBlocksInChainMustHaveValidTimestamp() {
		// Arrange:
		final BlockChainValidator validator = createValidator();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 3);
		final Block block = createFutureBlock(blocks.get(2));
		blocks.add(block);

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TIMESTAMP_TOO_FAR_IN_FUTURE));
	}

	@Test
	public void chainIsInvalidIfAnyTransactionInABlockIsSignedByBlockHarvester() {
		// Arrange:
		final BlockChainValidatorFactory factory = createValidatorFactory();
		final BlockChainValidator validator = factory.create();
		final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 11);
		parentBlock.sign();

		final List<Block> blocks = NisUtils.createBlockList(parentBlock, 2);
		final Block block = blocks.get(1);
		block.addTransaction(factory.createValidSignedTransaction());
		block.addTransaction(factory.createSignedTransactionWithGivenSender(block.getSigner()));
		block.addTransaction(factory.createValidSignedTransaction());
		block.sign();

		// Act:
		final ValidationResult result = validator.isValid(parentBlock, blocks);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
	}

	//region helper functions

	//region transactions / blocks

	private static Block createParentBlock(final Account account, final long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, new BlockHeight(height));
	}

	private static Block createFutureBlock(final Block parentBlock) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Block block = new Block(Utils.generateRandomAccount(), parentBlock, currentTime.addMinutes(2));
		block.sign();
		return block;
	}

	private static void resignBlocks(final List<Block> blocks) {
		Block previousBlock = null;
		for (final Block block : blocks) {
			if (null != previousBlock) {
				block.setPrevious(previousBlock);
			}

			block.sign();
			previousBlock = block;
		}
	}

	//endregion

	private static class BlockChainValidatorFactory {
		public final BlockScorer scorer = Mockito.mock(BlockScorer.class);
		public final int maxChainSize = 21;
		public final AccountStateCache accountStateCache = new DefaultAccountStateCache().asAutoCache();
		public final DefaultHashCache transactionHashCache = Mockito.mock(DefaultHashCache.class);
		public final ReadOnlyNisCache nisCache = NisCacheFactory.createReadOnly(this.accountStateCache, this.transactionHashCache);
		public final BlockValidator blockValidator = NisUtils.createBlockValidatorFactory().create(this.nisCache);
		public final SingleTransactionValidator transactionValidator;

		public BlockChainValidatorFactory() {
			final TransactionValidatorFactory transactionValidatorFactory = NisUtils.createTransactionValidatorFactory();
			final AggregateSingleTransactionValidatorBuilder builder = transactionValidatorFactory.createSingleBuilder(this.accountStateCache);
			this.transactionValidator = builder.build();

			Mockito.when(this.transactionHashCache.anyHashExists(Mockito.any())).thenReturn(false);
			Mockito.when(this.scorer.calculateHit(Mockito.any())).thenReturn(BigInteger.ZERO);
			Mockito.when(this.scorer.calculateTarget(Mockito.any(), Mockito.any())).thenReturn(BigInteger.ONE);
		}

		public BlockChainValidator create() {
			final NisCache nisCache = NisCacheFactory.create(this.accountStateCache);
			final BlockTransactionObserver observer = new BlockTransactionObserverFactory().createExecuteCommitObserver(nisCache);
			return new BlockChainValidator(
					block -> new BlockExecuteProcessor(nisCache, block, observer),
					this.scorer,
					this.maxChainSize,
					this.blockValidator,
					this.transactionValidator,
					new DefaultDebitPredicate(this.accountStateCache));
		}

		public AccountInfo getAccountInfo(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
		}

		private Account createAccountWithBalance(final Amount balance) {
			final Account account = Utils.generateRandomAccount();
			return this.setAccountBalance(account, balance);
		}

		private Account setAccountBalance(final Account account, final Amount balance) {
			final AccountState accountState = this.accountStateCache.findStateByAddress(account.getAddress());
			accountState.getAccountInfo().incrementBalance(balance);
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
			return account;
		}

		private void setCosigner(final Account multisig, final Account cosigner) {
			this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosigner.getAddress());
			this.accountStateCache.findStateByAddress(cosigner.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
		}

		private MockTransaction createValidSignedTransaction() {
			final TimeInstant timeInstant = NisMain.TIME_PROVIDER.getCurrentTime().addSeconds(BlockChainConstants.MAX_ALLOWED_SECONDS_AHEAD_OF_TIME / 2);
			final MockTransaction transaction = new MockTransaction(12, timeInstant);
			return this.prepareMockTransaction(transaction);
		}

		private Transaction createSignedTransactionWithGivenSender(final Account account) {
			final MockTransaction transaction = new MockTransaction(account);
			return this.prepareMockTransaction(transaction);
		}

		private MockTransaction prepareMockTransaction(final MockTransaction transaction) {
			this.setAccountBalance(transaction.getSigner(), Amount.fromNem(1000));
			return prepareTransaction(transaction);
		}
	}

	private static Transaction createTransfer(final Account sender, final Account recipient, final Amount amount) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final Transaction transaction = new TransferTransaction(currentTime.addSeconds(1), sender, recipient, amount, null);
		return prepareTransaction(transaction);
	}

	private static TransferTransaction createTransfer(final Account signer, final Amount amount, final Amount fee) {
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		final TransferTransaction transaction = new TransferTransaction(currentTime, signer, Utils.generateRandomAccount(), amount, null);
		transaction.setFee(fee);
		return prepareTransaction(transaction);
	}

	private static <T extends Transaction> T prepareTransaction(final T transaction) {
		// set the deadline appropriately and sign
		final TimeInstant currentTime = NisMain.TIME_PROVIDER.getCurrentTime();
		transaction.setDeadline(currentTime.addSeconds(10));
		transaction.sign();
		return transaction;
	}

	private static BlockChainValidatorFactory createValidatorFactory() {
		return new BlockChainValidatorFactory();
	}

	private static BlockChainValidator createValidator() {
		return new BlockChainValidatorFactory().create();
	}

	//endregion
}
