package org.nem.nis.chain.integration;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.ReadOnlyRemoteLinks;
import org.nem.nis.test.NisCacheFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class BlockChainHarvesterTest {
	private static final Logger LOGGER = Logger.getLogger(BlockChainHarvesterTest.class.getName());

	@Test
	public void externalBlockWithLowerScoreCanBeProcessedBeforeHarvesting() {
		// Arrange:
		// - create a single transaction
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final Account account = context.createAccount(Amount.fromNem(30));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));

		// - add the transaction to the unconfirmed cache
		context.addUnconfirmed(t1);

		// - add a block with only the first transaction
		//   (set its timestamp in the past so that the harvested block has a higher score)
		context.setTimeOffset(-5);
		final Block block = context.createNextBlock();
		block.addTransaction(t1);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		Assert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// Assert:
		// - the process (remote) block was accepted; the harvest (local) block was also accepted
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));

		// - the harvested block contains zero transactions (the unconfirmed transaction was already present in the block)
		Assert.assertThat(harvestedBlock.getTransactions().size(), IsEqual.equalTo(0));

		// Sanity:
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(-1L));
	}

	@Test
	public void externalBlockCanBeProcessedAfterHarvesting() {
		// Arrange:
		// - create three transactions
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final Account account = context.createAccount(Amount.fromNem(40));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(20));
		final Transaction t3 = context.createTransfer(account, Amount.fromNem(8));

		// - add both transactions to the unconfirmed cache
		context.addUnconfirmed(t1);
		context.addUnconfirmed(t2);

		// - add a block with only the third transaction
		//   (set its timestamp in the future so that the harvested block has a higher score)
		context.setTimeOffset(5);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		Assert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		// - the harvest result was accepted; the process result failed validation
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		// TODO 20150306 J-B: why is processResult SUCCESS?
		// TODO 20150309 BR -> J: the earlier a block is created the higher the score, so you have to add time to the external block, not subtract.
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.NEUTRAL));

		// - the harvested block contains two transactions (the third one doesn't fit)
		Assert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t1, t2));

		// Sanity:
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	@Test
	public void raceConditionBetweenBlockChainAndNewBlockTransactionGatheringAllowsNewBlockWithTransfersToPassValidationButFailExecution() {
		// Arrange:
		final ReadOnlyNisCache nisCache = Mockito.spy(NisCacheFactory.createReal());
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);
		final Account account = context.createAccount(Amount.fromNem(40));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(20));
		final Transaction t3 = context.createTransfer(account, Amount.fromNem(8));
		final Supplier<Amount> getAccountBalance =
				() -> nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getAccountInfo().getBalance();

		// Assert:
		this.exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(
				nisCache,
				context,
				getAccountBalance,
				Arrays.asList(t1, t2), // unconfirmed
				Arrays.asList(t3), // block
				NegativeBalanceException.class);
		// TODO 20150308 J-B: the NegativeBalanceException is actually coming from unconfirmed transactions
		// > however; this test is showing that the "race condition" is only occurring with the harvested and processed block
		// > have the same height; i think this is always the case because the last block height is grabbed BEFORE a
		// > copy of the cache is made
		// > interestingly it seems that this test picks the harvested block but the following one picks the processed block
		// > (based on the ending state); i'm not sure why that is the case
		// TODO 20150309 BR -> J: "the 'race condition' is only occurring with the harvested and processed block have the same height"
		// > I don't see a reason why it shouldn't occur with different heights (at least in principle) during synchronization when processing part of a chain.
		//
		// > I am not sure that the test is doing what you had in mind. Here is what I think it is doing:
		// > The harvested block has a better score because it has an earlier timestamp (also true for the test below).
		// > Call the transaction signer account A and abbreviate unconfirmed transaction with UT. In the beginning A has a balance of 40.
		// > After adding t1 and t2 to the UT cache, the UT observer see an unconfirmed balance of 4 for A (40 - 12 - 2 - 20 - 2).
		// > Then the external block B1 is applied which decreases the real balance of A to 30 (40 - 8 - 2).
		// > Then the harvester generates a new block B2 (no exception here) and B2 gets processed. The block B1 is reverted (A has again a balance of 40)
		// > and B2 gets applied to the nis cache so A has a balance of 4 according to the nis cache.
		// > We are at BlockChainUpdateContext.updateOurChain() line 138 now.
		// > Next relevant thing is addRevertedTransactionsAsUnconfirmed(). The transaction in block B1 gets added back to the UT cache via addExisting.
		// > This leads to the call UnconfirmedTransactionsCache.add() where this.validate.apply(transaction) is called.
		// > During validation the BalanceValidator is called which calls this.debitPredicate.canDebit().
		// > The debit predicate calls getUnconfirmedBalance() and that is when the exception happens because A has only a balance of 4.
		// > So the real problem is adding back the transaction in B1 to the UT cache. I think a validator should not throw, it is too risky.
		// > So we have to change something there. Thoughts?
		//
		// "interestingly it seems that this test picks the harvested block but the following one picks the processed block"
		// > In both cases the harvested block has a better score than the external block. In the first test the processing of the harvested block throws
		// > so the external block stays in the db while in the second test the harvested block replaces the external block. So I do not quite understand
		// > your comment.

		// - the cache has the correct balance for the sender account
		Assert.assertThat(getAccountBalance.get(), IsEqual.equalTo(Amount.fromNem(4)));
	}

	@Test
	public void raceConditionBetweenBlockChainAndNewBlockTransactionGatheringAllowsNewBlockWithImportanceTransfersToPassValidationButFailExecution() {
		// Arrange:
		final ReadOnlyNisCache nisCache = Mockito.spy(NisCacheFactory.createReal());
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);
		final Account account = context.createAccount(Amount.fromNem(10000));
		final Account remote1 = context.createAccount(Amount.ZERO);
		final Account remote2 = context.createAccount(Amount.ZERO);
		final Transaction t1 = context.createImportanceTransfer(account, remote1, true);
		final Transaction t2 = context.createImportanceTransfer(account, remote2, true);

		LOGGER.info(String.format("remote 1 = %s; remote 2 = %s", remote1, remote2));
		final Supplier<Address> getRemoteAccount =
				() ->  {
					final ReadOnlyRemoteLinks remoteLinks = nisCache.getAccountStateCache().findStateByAddress(account.getAddress()).getRemoteLinks();
					return remoteLinks.isHarvestingRemotely()
							? remoteLinks.getCurrent().getLinkedAddress()
							: null;
				};

		// Assert:
		this.exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(
				nisCache,
				context,
				getRemoteAccount,
				Arrays.asList(t1), // unconfirmed
				Arrays.asList(t2), // block
				null);

		// - the cache has the correct remote for the sender account
		Assert.assertThat(getRemoteAccount.get(), IsEqual.equalTo(remote1.getAddress()));
	}

	protected void exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(
			final ReadOnlyNisCache nisCache,
			final RealBlockChainTestContext context,
			final Supplier<?> getStateToLog,
			final List<Transaction> unconfirmedTransactions,
			final List<Transaction> blockTransactions,
			final Class<?> expectedHarvesterException) {
		// Arrange:
		logWithThread(String.format("start state = %s", getStateToLog.get()));
		final Object lock1 = new Object();
		final Object lock2 = new Object();

		// set up a handshake between the harvester and block processing threads
		// T(0): harvester thread and makes a copy of the cache
		// T(1): process thread is unblocked and processes a new block that is accepted
		//       and makes the block that will be harvested invalid
		// T(2): harvester is unblocked creates an invalid block (it is working off the original cache)
		//       the block fails execution because the cache changed
		final boolean[] isFirstTime = new boolean[] { true };
		Mockito.when(nisCache.copy()).then(invocationOnMock -> {
			final NisCache copyCache = (NisCache)invocationOnMock.callRealMethod();
			if (!isFirstTime[0]) {
				return copyCache;
			}

			isFirstTime[0] = false;
			logWithThread("harvester copied cache and is signaling");
			Utils.monitorSignal(lock1);
			Utils.monitorWait(lock2);
			logWithThread("harvester resumed");
			return copyCache;
		});

		final ValidationResult[] harvestResult = new ValidationResult[1];
		final CompletableFuture future = CompletableFuture.runAsync(() -> {
			// - add both transactions to the unconfirmed cache
			unconfirmedTransactions.forEach(context::addUnconfirmed);

			// Act:
			// - harvest a block (harvestBlock should call nisCache.copy)
			final Block harvestedBlock = context.harvestBlock();
			harvestResult[0] = context.processBlock(harvestedBlock);
		});

		// Act:
		// - wait until the copy is called
		logWithThread(String.format("processor is waiting for signal (state = %s)", getStateToLog.get()));
		Utils.monitorWait(lock1);

		// - add a block with only the third transaction
		context.setTimeOffset(5);
		final Block block = context.createNextBlock();
		block.addTransactions(blockTransactions);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);
		logWithThread(String.format("processed block is accepted (state = %s)", getStateToLog.get()));
		Utils.monitorSignal(lock2);

		// Assert:
		// - the harvested block completes and fails as expected
		if (null == expectedHarvesterException) {
			future.join();
		} else {
			ExceptionAssert.assertThrowsCompletionException(
					v -> future.join(),
					expectedHarvesterException);
		}

		// - the processed block was accepted
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		logWithThread(String.format("end state = %s", getStateToLog.get()));
	}

	private static void logWithThread(final String message) {
		LOGGER.info(String.format("[%s] %s", Thread.currentThread().getId(), message));
	}
}
