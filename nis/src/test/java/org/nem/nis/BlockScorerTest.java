package org.nem.nis;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.pox.poi.GroupedHeight;
import org.nem.nis.state.*;
import org.nem.nis.test.NisUtils;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;

public class BlockScorerTest {

	private static final byte[] HASH_BYTES = new byte[]{
			(byte) 0xF7, (byte) 0xF6, (byte) 0xF5, (byte) 0xF4, (byte) 0xF3, (byte) 0xF2, (byte) 0xF1, (byte) 0xF0, (byte) 0xE7,
			(byte) 0xE6, (byte) 0xE5, (byte) 0xE4, (byte) 0xE3, (byte) 0xE2, (byte) 0xE1, (byte) 0xE0, (byte) 0xD7, (byte) 0xD6,
			(byte) 0xD5, (byte) 0xD4, (byte) 0xD3, (byte) 0xD2, (byte) 0xD1, (byte) 0xD0, (byte) 0xC7, (byte) 0xC6, (byte) 0xC5,
			(byte) 0xC4, (byte) 0xC3, (byte) 0xC2, (byte) 0xC1, (byte) 0xC0
	};

	// region calculateHit

	@Test
	public void hitIsCalculatedCorrectly() {
		// Arrange:
		final Account blockSigner = Utils.generateRandomAccount();
		final BlockScorer scorer = createScorer();
		final Block previousBlock = new Block(blockSigner, Hash.ZERO, new Hash(HASH_BYTES), TimeInstant.ZERO, new BlockHeight(11));

		// Act:
		final BigInteger hit = scorer.calculateHit(previousBlock);

		// Assert:
		MatcherAssert.assertThat(hit, IsEqual.equalTo(new BigInteger("20A80E8435E74", 16)));
	}

	// endregion

	// region calculateTarget

	@Test
	public void targetIsZeroWhenBalanceIsZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(0);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 101, 11);

		// Act:
		final BigInteger target = context.scorer.calculateTarget(previousBlock, block);

		// Assert:
		MatcherAssert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 1, 11);

		// Act:
		final BigInteger target = context.scorer.calculateTarget(previousBlock, block);

		// Assert:
		MatcherAssert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsNegative() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 101, 11);
		final Block block = createBlock(blockSigner, 1, 11);

		// Act:
		final BigInteger target = context.scorer.calculateTarget(previousBlock, block);

		// Assert:
		MatcherAssert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsCalculatedCorrectlyWhenBalanceIsNonZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 101, 11);

		block.setDifficulty(new BlockDifficulty((long) 60E12));
		context.recalculateImportances(block.getHeight());

		// Act:
		final BigInteger target = context.scorer.calculateTarget(previousBlock, block);

		// Assert:
		// (time-difference [100] * block-signer-importance [72000^] * multiplier * magic-number / difficulty [60e12])
		// ^ MockBlockScorerAnalyzer calculates importance as balance / 1000
		final long multiplier = 8_000_000_000L;
		final BigInteger expectedTarget = BigInteger.valueOf(100).multiply(BigInteger.valueOf(72000 * multiplier))
				.multiply(BlockScorer.TWO_TO_THE_POWER_OF_64).divide(BigInteger.valueOf((long) 60E12));

		MatcherAssert.assertThat(target, IsEqual.equalTo(expectedTarget));
	}

	@Test
	public void targetIncreasesAsTimeElapses() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block1 = createBlock(blockSigner, 101, 11);
		final Block block2 = createBlock(blockSigner, 201, 11);

		// Act:
		context.recalculateImportances(block1.getHeight());
		final BigInteger target1 = context.scorer.calculateTarget(previousBlock, block1);

		context.recalculateImportances(block2.getHeight());
		final BigInteger target2 = context.scorer.calculateTarget(previousBlock, block2);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertTrue(target1.compareTo(target2) < 0);
	}

	// endregion

	// region calculateHarvesterEffectiveImportance

	@Test
	public void calculateHarvesterEffectiveImportanceDerivesEffectiveImportanceFromImportance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(1000);
		context.getImportanceInfo(block.getSigner()).setImportance(new BlockHeight(718), 0.75); // this is the grouped height

		// Act:
		final long score = context.scorer.calculateHarvesterEffectiveImportance(block);

		// Assert:
		MatcherAssert.assertThat(score, IsEqual.equalTo(6_000_000_000L)); // 0.75 * NemesisBlock.AMOUNT.getNumNem()
	}

	@Test
	public void calculateHarvesterEffectiveImportanceForwardsAccountAtProperHeightIfRemoteHarvestingAccountIsUsed() {
		// Arrange:
		final BlockHeight height = new BlockHeight(1442);
		final BlockHeight groupedHeight = GroupedHeight.fromHeight(height);
		final TestContext context = new TestContext(Mockito.mock(AccountStateCache.class));
		final Block block = NisUtils.createRandomBlockWithHeight(height.getRaw());

		final Address remoteHarvesterAddress = block.getSigner().getAddress();
		final Address ownerAddress = Utils.generateRandomAddress();
		final AccountState remoteState = new AccountState(remoteHarvesterAddress);
		final AccountState ownerState = new AccountState(ownerAddress);
		ownerState.getImportanceInfo().setImportance(groupedHeight, 0.75);
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(remoteHarvesterAddress, height)).thenReturn(ownerState);
		Mockito.when(context.accountStateCache.findForwardedStateByAddress(remoteHarvesterAddress, groupedHeight)).thenReturn(remoteState);
		Mockito.when(context.accountStateCache.mutableContents()).thenReturn(new CacheContents<>(Arrays.asList(ownerState, remoteState)));

		context.recalculateImportances(block.getHeight());

		// Act:
		final long score = context.scorer.calculateHarvesterEffectiveImportance(block);

		// Assert:
		MatcherAssert.assertThat(score, IsNot.not(IsEqual.equalTo(0L)));
		Mockito.verify(context.accountStateCache, Mockito.times(1)).findForwardedStateByAddress(remoteHarvesterAddress, height);
	}

	// endregion

	// region calculateBlockScore

	@Test
	public void blockScoreIsCalculatedCorrectly() throws Exception {
		// Arrange:
		final BlockScorer scorer = createScorer();
		final Block parent = createBlock(Utils.generateRandomAccount(), 5567320, 97);
		final Block current = createBlock(Utils.generateRandomAccount(), 5568532, 98);
		current.setDifficulty(new BlockDifficulty(44_888_000_000_000L));

		// Act:
		final long score = scorer.calculateBlockScore(parent, current);

		// Assert:
		final long expectedScore = 44_888_000_000_000L - (5568532 - 5567320);
		MatcherAssert.assertThat(score, IsEqual.equalTo(expectedScore));
	}

	// endregion

	private static Block roundTripBlock(final AccountLookup accountLookup, final Block block)
			throws NoSuchFieldException, IllegalAccessException {
		final VerifiableEntity.DeserializationOptions options = VerifiableEntity.DeserializationOptions.VERIFIABLE;

		final Deserializer deserializer = Utils.roundtripSerializableEntity(block, accountLookup);
		final Block b = new Block(deserializer.readInt("type"), options, deserializer);

		Field field = b.getClass().getDeclaredField("generationHash");
		field.setAccessible(true);
		field.set(b, block.getGenerationHash());

		field = b.getClass().getDeclaredField("prevBlockHash");
		field.setAccessible(true);
		field.set(b, block.getPreviousBlockHash());
		return b;
	}

	private static Block createBlock(final Account account, final int timeStamp, final long height)
			throws NoSuchFieldException, IllegalAccessException {
		final Block block = new Block(account, Hash.ZERO, Hash.ZERO, new TimeInstant(timeStamp), new BlockHeight(height));
		block.sign();

		final AccountCache accountCache = new DefaultAccountCache();
		accountCache.addAccountToCache(account.getAddress());
		return roundTripBlock(accountCache, block);
	}

	private static BlockScorer createScorer() {
		return new BlockScorer(Mockito.mock(AccountStateCache.class));
	}

	private static class TestContext {
		private final AccountStateCache accountStateCache;
		private final BlockScorer scorer;
		private final PoxFacade poxFacade;

		private TestContext() {
			this(new DefaultAccountStateCache().copy());
		}

		private TestContext(final AccountStateCache accountStateCache) {
			this.accountStateCache = accountStateCache;
			this.scorer = new BlockScorer(this.accountStateCache);

			this.poxFacade = new DefaultPoxFacade((blockHeight, accountStates) -> {
				for (final AccountState accountState : accountStates) {
					final Amount balance = accountState.getWeightedBalances().getUnvested(blockHeight);
					final double importance = balance.getNumMicroNem() / 1000.0;
					accountState.getImportanceInfo().setImportance(blockHeight, importance);
				}
			});
		}

		private Account createAccountWithBalance(final long balance) {
			final Account account = Utils.generateRandomAccount();
			final AccountState accountState = this.accountStateCache.findStateByAddress(account.getAddress());
			accountState.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(balance));
			accountState.setHeight(BlockHeight.ONE);
			return account;
		}

		private AccountImportance getImportanceInfo(final Account account) {
			return this.accountStateCache.findStateByAddress(account.getAddress()).getImportanceInfo();
		}

		private void recalculateImportances(final BlockHeight height) {
			this.poxFacade.recalculateImportances(GroupedHeight.fromHeight(height),
					this.accountStateCache.mutableContents().asCollection());
		}
	}
}
