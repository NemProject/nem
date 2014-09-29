package org.nem.nis.harvesting;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class HarvesterTest {

	//region harvest bypass

	@Test
	public void harvestedBlockIsNullIfLastDbBlockIsNull() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(null);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		Assert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.only()).getLastDbBlock();
	}

	@Test
	public void harvestedBlockIsNullIfNoAccountsAreUnlocked() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.unlockedAccounts.size()).thenReturn(0);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		Assert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.unlockedAccounts, Mockito.only()).size();
	}

	//endregion

	//region single unlocked account

	@Test
	public void harvestBlockReturnsNullIfGeneratorReturnsNull() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(Arrays.asList(account).iterator());
		Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(null);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		Assert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.generator, Mockito.only())
				.generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any());
	}

	@Test
	public void harvestBlockReturnsNonNullIfGeneratorReturnsNonNull() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final GeneratedBlock generatedBlock = new GeneratedBlock(NisUtils.createRandomBlockWithHeight(account, 11), 12L);
		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(Arrays.asList(account).iterator());
		Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(generatedBlock);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		Assert.assertThat(block, IsEqual.equalTo(generatedBlock.getBlock()));
		Mockito.verify(context.generator, Mockito.only())
				.generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any());
	}

	@Test
	public void currentTimeIsPassedToGenerator() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(14));

		// Act:
		context.harvester.harvestBlock();

		// Assert:
		Mockito.verify(context.generator, Mockito.only())
				.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.eq(new TimeInstant(14)));
	}

	@Test
	public void mappedLastBlockIsPassedToGenerator() {
		// Arrange:
		final TestContext context = new TestContext();
		final org.nem.nis.dbmodel.Block dbLastBlock = NisUtils.createDbBlockWithTimeStamp(50);
		Mockito.when(context.accountLookup.findByAddress(Address.fromPublicKey(dbLastBlock.getForger().getPublicKey())))
				.thenReturn(Utils.generateRandomAccount());

		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbLastBlock);

		// Act:
		context.harvester.harvestBlock();

		// Assert:
		final ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
		Mockito.verify(context.generator, Mockito.only())
				.generateNextBlock(blockCaptor.capture(), Mockito.any(), Mockito.any());
		Assert.assertThat(blockCaptor.getValue().getTimeStamp(), IsEqual.equalTo(new TimeInstant(50)));
	}

	//endregion

	//region multiple unlocked accounts

	@Test
	public void harvestBlockReturnsBestGeneratedBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<GeneratedBlock> generatedBlocks = Arrays.asList(
				null,
				new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 12L),
				null,
				new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 18L),
				new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 14L));

		final List<Account> accounts = generatedBlocks.stream().map(b -> {
			final Account account = null == b ? Utils.generateRandomAccount() : b.getBlock().getSigner();
			Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any()))
					.thenReturn(b);
			return account;
		}).collect(Collectors.toList());

		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(accounts.iterator());

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		Assert.assertThat(block, IsEqual.equalTo(generatedBlocks.get(3).getBlock()));
		Mockito.verify(context.generator, Mockito.times(5))
				.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any());
	}

	//endregion

	private static class TestContext {
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		final BlockGenerator generator = Mockito.mock(BlockGenerator.class);
		final Harvester harvester = new Harvester(
				this.accountLookup,
				this.timeProvider,
				this.blockChainLastBlockLayer,
				this.unlockedAccounts,
				this.generator);

		private TestContext() {
			final org.nem.nis.dbmodel.Block dbLastBlock = NisUtils.createDbBlockWithTimeStamp(50);
			Mockito.when(this.accountLookup.findByAddress(Address.fromPublicKey(dbLastBlock.getForger().getPublicKey())))
					.thenReturn(Utils.generateRandomAccount());

			Mockito.when(this.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbLastBlock);
			Mockito.when(this.unlockedAccounts.size()).thenReturn(1);
			Mockito.when(this.unlockedAccounts.iterator()).thenReturn(Arrays.asList(Utils.generateRandomAccount()).iterator());
			Mockito.when(this.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new GeneratedBlock(NisUtils.createRandomBlock(), 12L));
		}
	}
}