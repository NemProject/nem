package org.nem.nis.chain.integration;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.cache.*;
import org.nem.nis.test.NisCacheFactory;

import java.util.concurrent.CompletableFuture;
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
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// Assert:
		// - the process (remote) block was accepted; the harvest (local) block was also accepted
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));

		// - the harvested block contains zero transactions (the unconfirmed transaction was already present in the block)
		Assert.assertThat(harvestedBlock, IsNull.notNullValue());
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
		//   (set its timestamp in the past so that the harvested block has a higher score)
		context.setTimeOffset(-5);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		final ValidationResult harvestResult = context.processBlock(harvestedBlock);

		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// Assert:
		// - the harvest result was accepted; the process result failed validation
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		// TODO 20150306 J-B: why is processResult SUCCESS?
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));

		// - the harvested block contains two transactions (the third one doesn't fit)
		Assert.assertThat(harvestedBlock, IsNull.notNullValue());
		Assert.assertThat(harvestedBlock.getTransactions(), IsEquivalent.equivalentTo(t1, t2));

		// Sanity:
		Assert.assertThat(block.getHeight().subtract(harvestedBlock.getHeight()), IsEqual.equalTo(0L));
	}

	// TODO 20150306 J-B: this produces a NegativeBalanceException; it has a race condition though
	@Ignore
	@Test
	public void raceConditionBetweenBlockChainAndNewBlockTransactionGathering() throws Exception {
		// Arrange:
		// - create three transactions
		final Object lock1 = new Object();
		final Object lock2 = new Object();

		final ReadOnlyNisCache nisCache = Mockito.spy(NisCacheFactory.createReal());
		final RealBlockChainTestContext context = new RealBlockChainTestContext(nisCache);
		final Account account = context.createAccount(Amount.fromNem(40));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));
		final Transaction t2 = context.createTransfer(account, Amount.fromNem(20));
		final Transaction t3 = context.createTransfer(account, Amount.fromNem(8));

		final boolean[] isFirstTime = new boolean[] { true };
		Mockito.when(nisCache.copy()).then(invocationOnMock -> {
			final NisCache copyCache = (NisCache)invocationOnMock.callRealMethod();
			if (!isFirstTime[0]) {
				return copyCache;
			}

			isFirstTime[0] = false;
			LOGGER.info("signal lock 1: " + Thread.currentThread().getId());
			Utils.monitorSignal(lock1);
			LOGGER.info("wait lock 2: " + Thread.currentThread().getId());
			Utils.monitorWait(lock2);
			return copyCache;
		});

		final ValidationResult[] harvestResult = new ValidationResult[1];
		final CompletableFuture future = CompletableFuture.runAsync(() -> {
			// - add both transactions to the unconfirmed cache
			context.addUnconfirmed(t1);
			context.addUnconfirmed(t2);

			// Act:
			// - harvest a block (harvestBlock should call nisCache.copy)
			final Block harvestedBlock = context.harvestBlock();
			harvestResult[0] = context.processBlock(harvestedBlock);
		});

		// Act:
		// - wait until the copy is called
		LOGGER.info("wait lock 1: " + Thread.currentThread().getId());
		Utils.monitorWait(lock1);

		// - add a block with only the third transaction
		context.setTimeOffset(5);
		final Block block = context.createNextBlock();
		block.addTransaction(t3);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);
		LOGGER.info("signal lock 2: " + Thread.currentThread().getId());
		Utils.monitorSignal(lock2);

		future.join();

		// Assert:
		// TODO 20150306 J-J should assert something?
	}
}
