package org.nem.nis.chain.integration;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;

public class BlockChainHarvesterTest {

	@Test
	public void externalBlockCanBeProcessedBeforeHarvesting() {
		// Arrange:
		// - create a single transaction
		final RealBlockChainTestContext context = new RealBlockChainTestContext();
		final Account account = context.createAccount(Amount.fromNem(30));
		final Transaction t1 = context.createTransfer(account, Amount.fromNem(12));

		// - add the transaction to the unconfirmed cache
		context.addUnconfirmed(t1);

		// - add a block with only the first transaction (it should have a higher score than the harvested block)
		context.setTimeOffset(-5);
		final Block block = context.createNextBlock();
		block.addTransaction(t1);
		block.sign();

		// Act:
		// - process the block
		final ValidationResult processResult = context.processBlock(block);

		// - harvest a block
		final Block harvestedBlock = context.harvestBlock();
		final ValidationResult harvestResult = context.processBlock(block);

		// Assert:
		// - the process result was accepted; the harvest result was not
		Assert.assertThat(processResult, IsEqual.equalTo(ValidationResult.SUCCESS));
		Assert.assertThat(harvestResult, IsEqual.equalTo(ValidationResult.NEUTRAL));

		// - the harvested block contains zero transactions (the unconfirmed transaction was already present in the block)
		Assert.assertThat(harvestedBlock, IsNull.notNullValue());
		Assert.assertThat(harvestedBlock.getTransactions().size(), IsEqual.equalTo(0));
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
	}
}
