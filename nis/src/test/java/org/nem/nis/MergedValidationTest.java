package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.chain.BlockExecuteProcessor;
import org.nem.nis.harvesting.*;
import org.nem.nis.secret.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.sync.DefaultDebitPredicate;
import org.nem.nis.test.*;
import org.nem.nis.validators.*;
import org.nem.nis.validators.transaction.AggregateSingleTransactionValidatorBuilder;

import java.math.BigInteger;
import java.util.*;

/**
 * just an example for now; if it works, should address:
 * TODO 20150103 J-G: consider a test with mixed Del and Add modifications
 * TODO 20150103 J-G: is there a reason these tests are testing at the BlockChainServices level instead of the BlockChainValidator level?
 * TODO 20150105 J-G: should we look into merging those tests somehow?
 */

@RunWith(Enclosed.class)
public class MergedValidationTest {

	private static abstract class AbstractMergedValidationTest {

		@Test
		public void transactionFailsValidationIfConfirmedBalanceIsInsufficient() {
			// Arrange:
			final TestContext context = new TestContext();
			final Account sender = context.addAccount(Amount.fromNem(20));
			final Account recipient = context.addAccount(Amount.fromNem(10));
			final TimeInstant currentTime = new TimeInstant(11);

			// - T(O) - S: 20 | R: 10
			// - T(1) - R -12-> S | XXX
			// - T(2) - S -15-> R | S: 03 | R: 25
			final Transaction t1 = createTransferTransaction(currentTime, recipient, sender, Amount.fromNem(12));
			t1.setFee(Amount.fromNem(3));
			t1.sign();
			final Transaction t2 = createTransferTransaction(currentTime, sender, recipient, Amount.fromNem(15));
			t2.setFee(Amount.fromNem(2));
			t2.sign();

			// Act / Assert:
			this.assertSecondTransactionIsKept(
					context.nisCache,
					t1,
					t2,
					ValidationResult.FAILURE_INSUFFICIENT_BALANCE);
		}

		protected abstract void assertSecondTransactionIsKept(
				final NisCache nisCache,
				final Transaction t1,
				final Transaction t2,
				final ValidationResult expectedResult);

		public static TransferTransaction createTransferTransaction(final TimeInstant timeStamp, final Account sender, final Account recipient, final Amount amount) {
			final TransferTransaction transferTransaction = new TransferTransaction(timeStamp, sender, recipient, amount, null);
			transferTransaction.setDeadline(timeStamp.addSeconds(1));
			return transferTransaction;
		}

		public static class TestContext {
			private final NisCache nisCache = Mockito.mock(NisCache.class);
			private final AccountStateCache accountStateCache = new DefaultAccountStateCache().asAutoCache();

			public TestContext() {
				Mockito.when(this.nisCache.getAccountStateCache()).thenReturn(this.accountStateCache);
				Mockito.when(this.nisCache.getTransactionHashCache()).thenReturn(new DefaultHashCache());
			}

			//region addAccount

			public Account addAccount(final Amount amount) {
				return this.prepareAccount(Utils.generateRandomAccount(), amount);
			}

			public Account prepareAccount(final Account account, final Amount amount) {
				final AccountState accountState = this.accountStateCache.findStateByAddress(account.getAddress());
				accountState.getAccountInfo().incrementBalance(amount);
				accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, amount);
				return account;
			}

			//endregion
		}
	}

	public static class DefaultNewBlockTransactionProviderTest extends AbstractMergedValidationTest {

		@Override
		protected void assertSecondTransactionIsKept(
				final NisCache nisCache,
				final Transaction t1,
				final Transaction t2,
				final ValidationResult expectedResult) {
			// Arrange:
			final TestContext context = new TestContext(nisCache);
			context.addTransaction(t1);
			context.addTransaction(t2);

			// Act:
			final List<Transaction> blockTransactions = context.getBlockTransactions();

			// Assert:
			Assert.assertThat(blockTransactions.size(), IsEqual.equalTo(1));
			Assert.assertThat(blockTransactions, IsEquivalent.equivalentTo(t2));
		}

		private static class TestContext {
			private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
			private final List<Transaction> transactions = new ArrayList<>();
			private final NewBlockTransactionsProvider provider;

			private TestContext(final NisCache nisCache) {
				Mockito.when(this.unconfirmedTransactions.getTransactionsBefore(Mockito.any())).thenReturn(this.transactions);

				// set up the nis copy
				final AccountStateCache accountStateCache = nisCache.getAccountStateCache();
				final NisCache nisCacheCopy = Mockito.mock(NisCache.class);
				Mockito.when(nisCacheCopy.getAccountCache()).thenReturn(Mockito.mock(AccountCache.class));
				Mockito.when(nisCacheCopy.getAccountStateCache()).thenReturn(accountStateCache);
				Mockito.when(nisCache.copy()).thenReturn(nisCacheCopy);

				this.provider = new DefaultNewBlockTransactionsProvider(
						nisCache,
						NisUtils.createTransactionValidatorFactory(),
						NisUtils.createBlockValidatorFactory(),
						new BlockTransactionObserverFactory(),
						this.unconfirmedTransactions);
			}

			public List<Transaction> getBlockTransactions(final Account account, final TimeInstant timeInstant) {
				return this.provider.getBlockTransactions(account.getAddress(), timeInstant, new BlockHeight(10));
			}

			public List<Transaction> getBlockTransactions(final Account account) {
				return this.getBlockTransactions(account, TimeInstant.ZERO);
			}

			public List<Transaction> getBlockTransactions(final Account account, final BlockHeight height) {
				return this.provider.getBlockTransactions(account.getAddress(), TimeInstant.ZERO, height);
			}

			public List<Transaction> getBlockTransactions(final TimeInstant timeInstant) {
				return this.getBlockTransactions(Utils.generateRandomAccount(), timeInstant);
			}

			public List<Transaction> getBlockTransactions() {
				return this.getBlockTransactions(Utils.generateRandomAccount());
			}

			//region addTransaction

			public void addTransaction(final Transaction transaction) {
				this.transactions.add(transaction);
			}

			public void addTransactions(final Collection<? extends Transaction> transactions) {
				this.transactions.addAll(transactions);
			}

			public void addTransactions(final Account signer, final int startCustomField, final int endCustomField) {
				for (int i = startCustomField; i <= endCustomField; ++i) {
					this.addTransaction(new MockTransaction(signer, i));
				}
			}

			//endregion
		}
	}

	public static class BlockChainValidatorTest extends AbstractMergedValidationTest {

		@Override
		protected void assertSecondTransactionIsKept(
				final NisCache nisCache,
				final Transaction t1,
				final Transaction t2,
				final ValidationResult expectedResult) {
			// Arrange:
			final BlockChainValidator validator = new BlockChainValidatorFactory().create(nisCache);

			final Block parentBlock = createParentBlock(Utils.generateRandomAccount(), 10);
			parentBlock.sign();

			final List<Block> blocks = NisUtils.createBlockList(parentBlock, 1);
			blocks.get(0).addTransaction(t1);
			blocks.get(0).addTransaction(t2);
			resignBlocks(blocks);

			// Act:
			final ValidationResult result = validator.isValid(parentBlock, blocks);

			// Assert:
			Assert.assertThat(result, IsEqual.equalTo(expectedResult));
		}

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
}
