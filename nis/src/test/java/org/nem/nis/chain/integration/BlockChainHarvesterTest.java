package org.nem.nis.chain.integration;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.cache.*;
import org.nem.nis.dbmodel.DbMosaicId;
import org.nem.nis.state.*;
import org.nem.nis.test.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

// TODO 20151124 J-B: i think we broke the handshaking in these tests again :/
public class BlockChainHarvesterTest {
	private static final Logger LOGGER = Logger.getLogger(BlockChainHarvesterTest.class.getName());

	@Before
	public void setup() {
		// required for externalBlockWithLowerScoreCanBeProcessedAfterHarvestingWithMosaicTransfers
		Utils.setupGlobals();
	}

	@After
	public void teardown() {
		Utils.resetGlobals();
	}

	// region process then harvest

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
		// (set its timestamp in the past in order to increase the time-difference component of the target and make a hit very likely)
		// note: choose a large enough time offset or the harvested block will occasionally not meet the hit
		context.setTimeOffset(-60);
		final Block block = context.createNextBlock();
		block.addTransaction(t1);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		MatcherAssert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// Assert:
		// - the process (remote) block was accepted
		// - the harvest (local) block with higher score was also accepted
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(30 - 22 - 4)));

		// - the harvested block contains only the second transactions because the first was already present in the block
		MatcherAssert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t2));

		// Sanity:
		// - the process block has height H and the harvest block has height H+1
		MatcherAssert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(-1L));
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
		// (set its timestamp in the future in order to decrease the time-difference component of the target and make a hit impossible)
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
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(harvestedBlock, IsNull.nullValue());
		MatcherAssert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(30 - 12 - 2)));
	}

	// endregion

	// region harvest then process

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
		// (set its timestamp in the future so that the processed block has a lower score)
		context.setTimeOffset(5);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		MatcherAssert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		// - the harvest (local) block with higher score was accepted
		// - the process (remote) block was not accepted (because it had a lower score)
		MatcherAssert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.NEUTRAL));
		MatcherAssert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(40 - 32 - 4)));

		// - the harvested block contains two transactions (the third one doesn't fit)
		MatcherAssert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t1, t2));

		// Sanity:
		MatcherAssert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	@Test
	public void externalBlockWithLowerScoreCanBeProcessedAfterHarvestingWithMosaicTransfers() {
		// Arrange:
		// - this test is the same as the previous, except it uses mosaic transfers instead of regular transfers
		// - setup an account with a mosaic balance
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final MosaicId mosaicId = Utils.createMosaicId(17);
		final Account account = context.createAccount(Amount.fromNem(100));
		context.addMosaicIdMapping(mosaicId, new DbMosaicId(17L));
		context.addMosaicBalance(account, mosaicId, new Quantity(40));

		// - create three transactions
		final Transaction t1 = context.createMosaicTransfer(account, mosaicId, new Quantity(12));
		final Transaction t2 = context.createMosaicTransfer(account, mosaicId, new Quantity(20));
		final Transaction t3 = context.createMosaicTransfer(account, mosaicId, new Quantity(8));

		// - add both transactions to the unconfirmed cache
		context.addUnconfirmed(t1);
		context.addUnconfirmed(t2);

		// - add a block with only the third transaction
		// (set its timestamp in the future so that the processed block has a lower score)
		context.setTimeOffset(5);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		MatcherAssert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		// - the harvest (local) block with higher score was accepted
		// - the process (remote) block was not accepted (because it had a lower score)
		MatcherAssert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.NEUTRAL));
		MatcherAssert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(100 - 30))); // 15 XEM fees per transaction
		MatcherAssert.assertThat(context.getMosaicBalance(account, mosaicId), IsEqual.equalTo(new Quantity(40 - 32)));

		// - the harvested block contains two transactions (the third one doesn't fit)
		MatcherAssert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t1, t2));

		// Sanity:
		MatcherAssert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
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
		// (set its timestamp in the past so that the processed block has a higher score)
		context.setTimeOffset(-8);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		MatcherAssert.assertThat(harvestedBlock, IsNull.notNullValue());
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		// - the harvest (local) block with higher score was accepted
		// - the process (remote) block with higher score was accepted (and the harvest block was reverted)
		MatcherAssert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(context.getBalance(account), IsEqual.equalTo(Amount.fromNem(40 - 8 - 2)));

		// Sanity:
		MatcherAssert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	// endregion

	// region fixLessor

	@Test
	public void processBlockUpdatesBlockLessor() {
		// Arrange:
		final SynchronizedAccountStateCache accountStateCache = new SynchronizedAccountStateCache(new DefaultAccountStateCache());
		final DefaultNisCache nisCache = new DefaultNisCache(new SynchronizedAccountCache(new DefaultAccountCache()), accountStateCache,
				new SynchronizedPoxFacade(new DefaultPoxFacade(NisUtils.createImportanceCalculator())),
				new SynchronizedHashCache(new DefaultHashCache()), new SynchronizedNamespaceCache(new DefaultNamespaceCache()));
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);

		// Setup remote harvesting
		final Account account = context.createAccount(Amount.fromNem(100000));
		final Account remoteAccount = context.createAccount(Amount.ZERO);

		final RemoteLink remoteLink1 = new RemoteLink(remoteAccount.getAddress(), BlockHeight.ONE, ImportanceTransferMode.Activate,
				RemoteLink.Owner.HarvestingRemotely);
		final AccountState accountState = accountStateCache.findStateByAddress(account.getAddress());
		accountState.getRemoteLinks().addLink(remoteLink1);

		final RemoteLink remoteLink2 = new RemoteLink(account.getAddress(), BlockHeight.ONE, ImportanceTransferMode.Activate,
				RemoteLink.Owner.RemoteHarvester);
		final AccountState remoteAccountState = accountStateCache.findStateByAddress(remoteAccount.getAddress());
		remoteAccountState.getRemoteLinks().addLink(remoteLink2);

		final Block block = context.createNextBlock(remoteAccount);
		block.sign();

		// Act:
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		MatcherAssert.assertThat(block.getLessor(), IsNull.notNullValue());
		MatcherAssert.assertThat(block.getLessor(), IsEqual.equalTo(account));
	}

	// endregion

	// region exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering

	/**
	 * The harvested block has a better score because it has an earlier timestamp. Call the transaction signer account A and abbreviate
	 * unconfirmed transaction with UT. In the beginning A has a balance of 40. After adding t1 and t2 to the UT cache, the UT observer see
	 * an unconfirmed balance of 4 for A (40 - 12 - 2 - 20 - 2). Then the external block B1 is applied which decreases the real balance of A
	 * to 30 (40 - 8 - 2). Then the harvester generates a new block B2 (no exception here) and B2 gets processed. The block B1 is reverted
	 * (A has again a balance of 40) and B2 gets applied to the nis cache so A has a balance of 4 according to the nis cache. We are at
	 * BlockChainUpdateContext.updateOurChain() line 138 now. Next relevant thing is addRevertedTransactionsAsUnconfirmed(). The transaction
	 * in block B1 gets added back to the UT cache via addExisting. This leads to the call UnconfirmedTransactionsCache.add() where
	 * this.validate.apply(transaction) is called. During validation the BalanceValidator is called which calls
	 * this.debitPredicate.canDebit(). The debit predicate calls getUnconfirmedBalance() and that is when the exception happens because A
	 * has only a balance of 4.
	 */
	@Test
	public void generatedNewBlockContainingTransfersCanBeRejectedByOriginatingNisIfConflictingBlockIsReceivedDuringGeneration() {
		// Arrange:
		final ReadOnlyNisCache nisCache = Mockito.spy(NisCacheFactory.createReal());
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);
		final Account account = context.createAccount(Amount.fromNem(40));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(20));
		final Transaction t3 = context.createTransfer(account, Amount.fromNem(8));
		final Supplier<Amount> getAccountBalance = () -> nisCache.getAccountStateCache().findStateByAddress(account.getAddress())
				.getAccountInfo().getBalance();

		// Assert:
		this.exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(nisCache, context, getAccountBalance,
				Arrays.asList(t1, t2), // unconfirmed
				Collections.singletonList(t3), // block
				null);

		// - the cache has the correct balance for the sender account
		// T(0): BALANCE: 40 NEM
		// T(1): process thread is unblocked and processes a new block that is accepted
		// BALANCE: 30 (40 - 10) NEM
		// T(2): harvester is unblocked creates a valid block (it is working off the modified cache)
		// containing only t1 and rolls back the processed block
		// BALANCE: 26 (30 + 10 - 14) NEM
		MatcherAssert.assertThat(getAccountBalance.get(), IsEqual.equalTo(Amount.fromNem(40 - 14)));
	}

	@Test
	public void generatedNewBlockContainingImportanceTransfersCanBeRejectedByOriginatingNisIfConflictingBlockIsReceivedDuringGeneration() {
		// Arrange:
		final ReadOnlyNisCache nisCache = Mockito.spy(NisCacheFactory.createReal());
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);
		final Account account = context.createAccount(Amount.fromNem(10000));
		final Account remote1 = context.createAccount(Amount.ZERO);
		final Account remote2 = context.createAccount(Amount.ZERO);
		final Transaction t1 = context.createImportanceTransfer(account, remote1, true);
		final Transaction t2 = context.createImportanceTransfer(account, remote2, true);

		LOGGER.info(String.format("remote 1 = %s; remote 2 = %s", remote1, remote2));
		final Supplier<Address> getRemoteAccount = () -> {
			final ReadOnlyRemoteLinks remoteLinks = nisCache.getAccountStateCache().findStateByAddress(account.getAddress())
					.getRemoteLinks();
			return remoteLinks.isHarvestingRemotely() ? remoteLinks.getCurrent().getLinkedAddress() : null;
		};

		// Assert:
		this.exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(nisCache, context, getRemoteAccount,
				Collections.singletonList(t1), // unconfirmed
				Collections.singletonList(t2), // block
				null);

		// - the cache has the correct remote for the sender account
		// T(0): REMOTE: none
		// T(1): process thread is unblocked and processes a new block that is accepted
		// REMOTE: 2
		// T(2): harvester is unblocked creates a valid block (it is working off the modified cache)
		// containing no transactions (t2 is rejected as in progress because t1 is in the chain even though
		// t1 is subsequently rolled back) and rolls back the processed block
		// REMOTE: none
		// TODO 20151207 J-B,G: not sure if this behavior is ok (rejecting transactions due to other transactions that are rolled back)
		MatcherAssert.assertThat(getRemoteAccount.get(), IsNull.nullValue());
	}

	private void exploitRaceConditionBetweenBlockChainAndNewBlockTransactionGathering(final ReadOnlyNisCache nisCache,
			final RealBlockChainTestContext context, final Supplier<?> getStateToLog, final List<Transaction> unconfirmedTransactions,
			final List<Transaction> blockTransactions, final Class<?> expectedHarvesterException) {
		final RaceConditionTestContext testContext = new RaceConditionTestContext(nisCache, context, getStateToLog, unconfirmedTransactions,
				blockTransactions, expectedHarvesterException);
		testContext.run();
	}

	private static class RaceConditionTestContext {
		private final ReadOnlyNisCache nisCache;
		private final RealBlockChainTestContext context;
		private final Supplier<?> getStateToLog;
		private final List<Transaction> unconfirmedTransactions;
		private final List<Transaction> blockTransactions;
		private final Class<?> expectedHarvesterException;
		private final AtomicInteger lock1 = new AtomicInteger(1);
		private final AtomicInteger lock2 = new AtomicInteger(1);

		public RaceConditionTestContext(final ReadOnlyNisCache nisCache, final RealBlockChainTestContext context,
				final Supplier<?> getStateToLog, final List<Transaction> unconfirmedTransactions, final List<Transaction> blockTransactions,
				final Class<?> expectedHarvesterException) {
			this.nisCache = nisCache;
			this.context = context;
			this.getStateToLog = getStateToLog;
			this.unconfirmedTransactions = unconfirmedTransactions;
			this.blockTransactions = blockTransactions;
			this.expectedHarvesterException = expectedHarvesterException;
		}

		public void run() {
			// Arrange:
			this.logWithThread("start (main)");
			this.setupCopyHandshake();

			// Act: start the threads
			final CompletableFuture<?> harvesterThread = this.runHarvesterThread();
			final CompletableFuture<ValidationResult> processorThread = this.runProcessorThread();
			final ValidationResult processResult = processorThread.join();
			this.logWithThread("processor completed");

			// Assert:
			// - the harvested block completes and fails as expected
			if (null == this.expectedHarvesterException) {
				harvesterThread.join();
			} else {
				ExceptionAssert.assertThrowsCompletionException(v -> harvesterThread.join(), this.expectedHarvesterException);
			}

			// - the processed block was accepted
			MatcherAssert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
			this.logWithThread("end (main)");
		}

		private void setupCopyHandshake() {
			// set up a handshake between the harvester and block processing threads
			// T(0): harvester thread makes a 'copy' of the cache (this isn't a deep copy since the cache is tracking changes)
			// T(1): process thread is unblocked and processes a new block that is accepted
			// T(2): harvester is unblocked creates an valid block (it is working off the modified cache)
			// but uses only a subset of transactions
			final boolean[] isFirstTime = new boolean[]{
					true
			};
			Mockito.when(this.nisCache.copy()).then(invocationOnMock -> {
				final NisCache copyCache = (NisCache) invocationOnMock.callRealMethod();
				if (!isFirstTime[0]) {
					return copyCache;
				}

				isFirstTime[0] = false;
				this.monitorSignal(this.lock1, "harvester", 1);
				this.monitorWait(this.lock2, "harvester", 2);
				return copyCache;
			});
		}

		private CompletableFuture<?> runHarvesterThread() {
			return CompletableFuture.runAsync(() -> {
				this.logWithThread("start (harvester)");

				// - add both transactions to the unconfirmed cache
				this.unconfirmedTransactions.forEach(this.context::addUnconfirmed);

				// Act:
				// - harvest a block (harvestBlock should call nisCache.copy)
				final Block harvestedBlock = this.context.harvestBlock();
				this.context.processBlock(harvestedBlock);
			});
		}

		private CompletableFuture<ValidationResult> runProcessorThread() {
			return CompletableFuture.supplyAsync(() -> {
				this.logWithThread("start (processor)");

				// - wait until the copy is called
				this.monitorWait(this.lock1, "processor", 1);

				// - add a block with only the third transaction
				this.context.setTimeOffset(5);
				final Block block = this.context.createNextBlock();
				block.addTransactions(this.blockTransactions);
				block.sign();

				// Act:
				// - process the block
				final ValidationResult processResult = this.context.processBlock(block);
				this.monitorSignal(this.lock2, "processor", 2);
				return processResult;
			});
		}

		private void monitorWait(final AtomicInteger lock, final String threadName, final int lockId) {
			this.logWithThread(String.format("%s is waiting for signal lock%d", threadName, lockId));
			while (!lock.compareAndSet(0, 1)) {
				yieldSleep();
			}
			this.logWithThread(String.format("%s was signaled by lock%d", threadName, lockId));
		}

		private void monitorSignal(final AtomicInteger lock, final String threadName, final int lockId) {
			this.logWithThread(String.format("%s is signaling lock%d", threadName, lockId));
			lock.set(0);
			this.logWithThread(String.format("%s signaled lock%d", threadName, lockId));
		}

		private void logWithThread(final String message) {
			LOGGER.info(String.format("[%d] %s (state = %s)", Thread.currentThread().getId(), message, this.getStateToLog.get()));
		}

		private static void yieldSleep() {
			ExceptionUtils.propagateVoid(() -> Thread.sleep(10));
		}
	}

	// endregion
}
