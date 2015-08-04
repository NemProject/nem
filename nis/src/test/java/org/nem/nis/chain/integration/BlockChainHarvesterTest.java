package org.nem.nis.chain.integration;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class BlockChainHarvesterTest {
	private static final Logger LOGGER = Logger.getLogger(BlockChainHarvesterTest.class.getName());

	//region process then harvest

	@Test
	public void externalBlockWithLowerScoreCanBeProcessedBeforeHarvesting() {
		// Arrange:
		// - create a single transaction
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final Account account = context.createAccount(Amount.fromNem(30));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(10));

		// - add both transactions to the unconfirmed cache
		context.addUnconfirmed(t1);
		context.addUnconfirmed(t2);

		// - add a block with the first transaction
		//   (set its timestamp in the past in order to increase the time-difference component of the target and make a hit very likely)
		//   note: choose a large enough time offset or the harvested block will occasionally not meet the hit
		context.setTimeOffset(-60);
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
		// - the process (remote) block was accepted
		// - the harvest (local) block with higher score was also accepted
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(30 - 22 - 4)));

		// - the harvested block contains only the second transactions because the first was already present in the block
		Assert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t2));

		// Sanity:
		// - the process block has height H and the harvest block has height H+1
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(-1L));
	}

	@Test
	public void externalBlockWithHigherScoreCanBeProcessedBeforeHarvesting() {
		// Arrange:
		// - create a single transaction
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final Account account = context.createAccount(Amount.fromNem(30));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(10));

		// - add both transactions to the unconfirmed cache
		context.addUnconfirmed(t1);
		context.addUnconfirmed(t2);

		// - add a block with the same transaction
		//   (set its timestamp in the future in order to decrease the time-difference component of the target and make a hit impossible)
		context.setTimeOffset(8);
		final Block block = context.createNextBlock();
		block.addTransaction(t1);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();

		// Assert:
		// - the process (remote) block was accepted
		// - the harvest block was not accepted
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(harvestedBlock, IsNull.nullValue());
		Assert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(30 - 12 - 2)));
	}

	//endregion

	//region harvest then process

	@Test
	public void externalBlockWithLowerScoreCanBeProcessedAfterHarvesting() {
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
		//   (set its timestamp in the future so that the processed block has a lower score)
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
		// - the harvest (local) block with higher score was accepted
		// - the process (remote) block was not accepted (because it had a lower score)
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.NEUTRAL));
		Assert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(40 - 32 - 4)));

		// - the harvested block contains two transactions (the third one doesn't fit)
		Assert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t1, t2));

		// Sanity:
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	@Test
	public void externalBlockWithHigherScoreCanBeProcessedAfterHarvesting() {
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
		//   (set its timestamp in the past so that the processed block has a higher score)
		context.setTimeOffset(-8);
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
		// - the harvest (local) block with higher score was accepted
		// - the process (remote) block with higher score was accepted (and the harvest block was reverted)
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(40 - 8 - 2)));

		// Sanity:
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	//endregion

	//region fixLessor

	@Test
	public void processBlockUpdatesBlockLessor() {
		// Arrange:
		final SynchronizedAccountStateCache accountStateCache = new SynchronizedAccountStateCache(new DefaultAccountStateCache());
		final DefaultNisCache nisCache = new DefaultNisCache(
				new SynchronizedAccountCache(new DefaultAccountCache()),
				accountStateCache,
				new SynchronizedPoiFacade(new DefaultPoiFacade(NisUtils.createImportanceCalculator())),
				new SynchronizedHashCache(new DefaultHashCache()),
				new SynchronizedNamespaceCache(new DefaultNamespaceCache()));
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);

		// Setup remote harvesting
		final Account account = context.createAccount(Amount.fromNem(100000));
		final Account remoteAccount = context.createAccount(Amount.ZERO);

		final RemoteLink remoteLink1 = new RemoteLink(
				remoteAccount.getAddress(),
				BlockHeight.ONE,
				ImportanceTransferMode.Activate,
				RemoteLink.Owner.HarvestingRemotely);
		final AccountState accountState = accountStateCache.findStateByAddress(account.getAddress());
		accountState.getRemoteLinks().addLink(remoteLink1);

		final RemoteLink remoteLink2 = new RemoteLink(account.getAddress(), BlockHeight.ONE, ImportanceTransferMode.Activate, RemoteLink.Owner.RemoteHarvester);
		final AccountState remoteAccountState = accountStateCache.findStateByAddress(remoteAccount.getAddress());
		remoteAccountState.getRemoteLinks().addLink(remoteLink2);

		final Block block = context.createNextBlock(remoteAccount);
		block.sign();

		// Act:
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(block.getLessor(), IsNull.notNullValue());
		Assert.assertThat(block.getLessor(), IsEqual.equalTo(account));
	}

	//endregion

	//region exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering

	/**
	 * The harvested block has a better score because it has an earlier timestamp.
	 * Call the transaction signer account A and abbreviate unconfirmed transaction with UT. In the beginning A has a balance of 40.
	 * After adding t1 and t2 to the UT cache, the UT observer see an unconfirmed balance of 4 for A (40 - 12 - 2 - 20 - 2).
	 * Then the external block B1 is applied which decreases the real balance of A to 30 (40 - 8 - 2).
	 * Then the harvester generates a new block B2 (no exception here) and B2 gets processed. The block B1 is reverted (A has again a balance of 40)
	 * and B2 gets applied to the nis cache so A has a balance of 4 according to the nis cache.
	 * We are at BlockChainUpdateContext.updateOurChain() line 138 now.
	 * Next relevant thing is addRevertedTransactionsAsUnconfirmed(). The transaction in block B1 gets added back to the UT cache via addExisting.
	 * This leads to the call UnconfirmedTransactionsCache.add() where this.validate.apply(transaction) is called.
	 * During validation the BalanceValidator is called which calls this.debitPredicate.canDebit().
	 * The debit predicate calls getUnconfirmedBalance() and that is when the exception happens because A has only a balance of 4.
	 */
	// TODO 20150313 BR -> J: The names of the tests are still misleading. And the problem of the BalanceValidator throwing is still there.
	@Test
	@Ignore
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
				Collections.singletonList(t3), // block
				NegativeBalanceException.class);

		// - the cache has the correct balance for the sender account
		Assert.assertThat(getAccountBalance.get(), IsEqual.equalTo(Amount.fromNem(40 - 32 - 4)));
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
				() -> {
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
				Collections.singletonList(t1), // unconfirmed
				Collections.singletonList(t2), // block
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

		final CompletableFuture future = CompletableFuture.runAsync(() -> {
			// - add both transactions to the unconfirmed cache
			unconfirmedTransactions.forEach(context::addUnconfirmed);

			// Act:
			// - harvest a block (harvestBlock should call nisCache.copy)
			final Block harvestedBlock = context.harvestBlock();
			context.processBlock(harvestedBlock);
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

	//endregion

	private static void logWithThread(final String message) {
		LOGGER.info(String.format("[%s] %s", Thread.currentThread().getId(), message));
	}
}
