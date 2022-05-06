package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.Utils;
import org.nem.core.time.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class HarvesterTest {

	// region harvest bypass

	@Test
	public void harvestedBlockIsNullIfLastBlockLayerIsLoading() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.blockChainLastBlockLayer.isLoading()).thenReturn(true);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).isLoading();
	}

	@Test
	public void harvestedBlockIsNullIfNoAccountsAreUnlocked() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.unlockedAccounts.size()).thenReturn(0);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).size();
	}

	@Test
	public void harvestBlockPrunesUnlockedAccounts() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.unlockedAccounts.size()).thenReturn(0);
		Mockito.when(context.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(123));

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.blockChainLastBlockLayer, Mockito.times(1)).getLastBlockHeight();
		Mockito.verify(context.unlockedAccounts, Mockito.times(1)).prune(new BlockHeight(124));
	}

	// endregion

	// region single unlocked account

	@Test
	public void harvestBlockReturnsNullIfGeneratorReturnsNull() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(Collections.singletonList(account).iterator());
		Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(null);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsNull.nullValue());
		Mockito.verify(context.generator, Mockito.times(1)).generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any());
	}

	@Test
	public void harvestBlockReturnsNonNullIfGeneratorReturnsNonNull() {
		// Arrange:
		final TestContext context = new TestContext();
		final Account account = Utils.generateRandomAccount();
		final GeneratedBlock generatedBlock = new GeneratedBlock(NisUtils.createRandomBlockWithHeight(account, 11), 12L);
		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(Collections.singletonList(account).iterator());
		Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(generatedBlock);

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsEqual.equalTo(generatedBlock.getBlock()));
		Mockito.verify(context.generator, Mockito.times(1)).generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any());
	}

	@Test
	public void currentTimeIsPassedToGenerator() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(14));

		// Act:
		context.harvester.harvestBlock();

		// Assert:
		Mockito.verify(context.generator, Mockito.times(1)).generateNextBlock(Mockito.any(), Mockito.any(),
				Mockito.eq(new TimeInstant(14)));
	}

	@Test
	public void mappedLastBlockIsPassedToGenerator() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbBlock dbLastBlock = NisUtils.createDbBlockWithTimeStamp(50);
		final Block block = NisUtils.createRandomBlockWithTimeStamp(50);
		Mockito.when(context.mapper.map(dbLastBlock)).thenReturn(block);

		Mockito.when(context.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbLastBlock);

		// Act:
		context.harvester.harvestBlock();

		// Assert:
		final ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
		Mockito.verify(context.mapper, Mockito.only()).map(dbLastBlock);
		Mockito.verify(context.generator, Mockito.times(1)).generateNextBlock(blockCaptor.capture(), Mockito.any(), Mockito.any());
		MatcherAssert.assertThat(blockCaptor.getValue(), IsEqual.equalTo(block));
	}

	// endregion

	// region multiple unlocked accounts

	@Test
	public void harvestBlockReturnsBestGeneratedBlock() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<GeneratedBlock> generatedBlocks = Arrays.asList(null, new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 12L),
				null, new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 18L),
				new GeneratedBlock(NisUtils.createRandomBlockWithHeight(11), 14L));

		final List<Account> accounts = generatedBlocks.stream().map(b -> {
			final Account account = null == b ? Utils.generateRandomAccount() : b.getBlock().getSigner();
			Mockito.when(context.generator.generateNextBlock(Mockito.any(), Mockito.eq(account), Mockito.any())).thenReturn(b);
			return account;
		}).collect(Collectors.toList());

		Mockito.when(context.unlockedAccounts.iterator()).thenReturn(accounts.iterator());

		// Act:
		final Block block = context.harvester.harvestBlock();

		// Assert:
		MatcherAssert.assertThat(block, IsEqual.equalTo(generatedBlocks.get(3).getBlock()));
		Mockito.verify(context.generator, Mockito.times(5)).generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any());
	}

	// endregion

	private static class TestContext {
		final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		final BlockChainLastBlockLayer blockChainLastBlockLayer = Mockito.mock(BlockChainLastBlockLayer.class);
		final UnlockedAccounts unlockedAccounts = Mockito.mock(UnlockedAccounts.class);
		final NisDbModelToModelMapper mapper = Mockito.mock(NisDbModelToModelMapper.class);
		final BlockGenerator generator = Mockito.mock(BlockGenerator.class);
		final Harvester harvester = new Harvester(this.timeProvider, this.blockChainLastBlockLayer, this.unlockedAccounts, this.mapper,
				this.generator);

		private TestContext() {
			final DbBlock dbLastBlock = NisUtils.createDbBlockWithTimeStamp(50);
			Mockito.when(this.accountLookup.findByAddress(Address.fromPublicKey(dbLastBlock.getHarvester().getPublicKey())))
					.thenReturn(Utils.generateRandomAccount());

			Mockito.when(this.blockChainLastBlockLayer.getLastBlockHeight()).thenReturn(new BlockHeight(9));
			Mockito.when(this.blockChainLastBlockLayer.getLastDbBlock()).thenReturn(dbLastBlock);
			Mockito.when(this.unlockedAccounts.size()).thenReturn(1);
			Mockito.when(this.unlockedAccounts.iterator()).thenReturn(Collections.singletonList(Utils.generateRandomAccount()).iterator());
			Mockito.when(this.generator.generateNextBlock(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn(new GeneratedBlock(NisUtils.createRandomBlock(), 12L));
		}
	}
}
