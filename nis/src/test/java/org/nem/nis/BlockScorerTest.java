package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.*;
import org.nem.nis.secret.AccountImportance;
import org.nem.nis.test.NisUtils;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;

public class BlockScorerTest {

	private static final byte[] PUBKEY_BYTES = new byte[] {
			(byte)0x02,
			(byte)0xF0, (byte)0xF1, (byte)0xF2, (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7,
			(byte)0xE0, (byte)0xE1, (byte)0xE2, (byte)0xE3, (byte)0xE4, (byte)0xE5, (byte)0xE6, (byte)0xE7,
			(byte)0xD0, (byte)0xD1, (byte)0xD2, (byte)0xD3, (byte)0xD4, (byte)0xD5, (byte)0xD6, (byte)0xD7,
			(byte)0xC0, (byte)0xC1, (byte)0xC2, (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6, (byte)0xC7
	};

	private static final byte[] HASH_BYTES = new byte[] {
			(byte)0xF7, (byte)0xF6, (byte)0xF5, (byte)0xF4, (byte)0xF3, (byte)0xF2, (byte)0xF1, (byte)0xF0,
			(byte)0xE7, (byte)0xE6, (byte)0xE5, (byte)0xE4, (byte)0xE3, (byte)0xE2, (byte)0xE1, (byte)0xE0,
			(byte)0xD7, (byte)0xD6, (byte)0xD5, (byte)0xD4, (byte)0xD3, (byte)0xD2, (byte)0xD1, (byte)0xD0,
			(byte)0xC7, (byte)0xC6, (byte)0xC5, (byte)0xC4, (byte)0xC3, (byte)0xC2, (byte)0xC1, (byte)0xC0
	};

	@Test
	public void hitIsCalculatedCorrectly() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(new PublicKey(PUBKEY_BYTES));
		final Account blockSigner = new Account(keyPair);
		final BlockScorer scorer = createScorer();
		final Block previousBlock = new Block(blockSigner, Hash.ZERO, new Hash(HASH_BYTES), TimeInstant.ZERO, new BlockHeight(11));

		// Act:
		final BigInteger hit = scorer.calculateHit(previousBlock);

		// Assert:
		Assert.assertThat(hit, IsEqual.equalTo(new BigInteger("20A80E8435E74", 16)));
	}

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
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
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
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
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
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsCalculatedCorrectlyWhenBalanceIsNonZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final TestContext context = new TestContext();
		final Account blockSigner = context.createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 101, 11);

		block.setDifficulty(new BlockDifficulty((long)60E12));
		context.poiFacade.recalculateImportances(block.getHeight());
		context.getImportanceInfo(blockSigner).setImportance(block.getHeight(), 1572);

		// Act:
		final BigInteger target = context.scorer.calculateTarget(previousBlock, block);

		// Assert:
		// (time-difference [100] * block-signer-importance [72000^] * multiplier * magic-number / difficulty [60e12])
		// ^ MockBlockScorerAnalyzer calculates importance as balance / 1000
		final long multiplier = 4_000_000_000L;
		final BigInteger expectedTarget = BigInteger.valueOf(100)
				.multiply(BigInteger.valueOf(72000 * multiplier))
				.multiply(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BigInteger.valueOf((long)60E12));

		Assert.assertThat(target, IsEqual.equalTo(expectedTarget));
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
		final BigInteger target1 = context.scorer.calculateTarget(previousBlock, block1);
		final BigInteger target2 = context.scorer.calculateTarget(previousBlock, block2);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertTrue(target1.compareTo(target2) < 0);
	}

	//region getGroupedHeight

	private static final Map<Integer, Integer> HEIGHT_TO_GROUPED_HEIGHT_MAP = new HashMap<Integer, Integer>() {
		{
			this.put(1, 1);
			this.put(30, 1);
			this.put(31, 1);
			this.put(32, 31);
			this.put(33, 31);
			this.put(90, 62);
			this.put(111, 93);
		}
	};

	@Test
	public void getGroupedHeightReturnsGroupedBlockHeight() {
		// Assert:
		for (final Map.Entry<Integer, Integer> pair : HEIGHT_TO_GROUPED_HEIGHT_MAP.entrySet()) {
			assertGroupedHeight(pair.getKey(), pair.getValue());
		}
	}

	private static void assertGroupedHeight(final long height, final long expectedGroupedHeight) {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final BlockHeight groupedHeight = context.scorer.getGroupedHeight(new BlockHeight(height));

		// Assert:
		Assert.assertThat(groupedHeight, IsEqual.equalTo(new BlockHeight(expectedGroupedHeight)));
	}

	//endregion

	//region calculateForgerBalance

	@Test
	public void calculateForgerBalanceDerivesBalanceFromImportance() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(100);
		context.getImportanceInfo(block.getSigner()).setImportance(new BlockHeight(93), 0.75); // this is the grouped height

		// Act:
		final long balance = context.scorer.calculateForgerBalance(block);

		// Assert:
		Assert.assertThat(balance, IsEqual.equalTo(3_000_000_000L)); // 0.75 * NemesisBlock.AMOUNT.getNumNem()
	}

	@Test
	public void calculateForgerBalanceCallsRecalculateImportancesForGroupedBlock() {
		// Assert:
		for (final Map.Entry<Integer, Integer> pair : HEIGHT_TO_GROUPED_HEIGHT_MAP.entrySet()) {
			assertRecalculateImportancesCalledForHeight(pair.getKey(), pair.getValue());
		}
	}

	private static void assertRecalculateImportancesCalledForHeight(final long height, final long rawGroupedHeight) {
		// Arrange:
		final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		final TestContext context = new TestContext(poiFacade);
		final Block block = NisUtils.createRandomBlockWithHeight(height);

		final BlockHeight groupedHeight = new BlockHeight(rawGroupedHeight);
		final Address signerAddress = block.getSigner().getAddress();
		final PoiAccountState state = new PoiAccountState(signerAddress);
		state.getImportanceInfo().setImportance(groupedHeight, 0.75);
		Mockito.when(poiFacade.findForwardedStateByAddress(signerAddress, groupedHeight)).thenReturn(state);

		// Act:
		context.scorer.calculateForgerBalance(block);

		// Assert:
		Mockito.verify(poiFacade, Mockito.times(1)).recalculateImportances(groupedHeight);
	}

	//endregion

	private static Block roundTripBlock(final AccountLookup accountLookup, final Block block) throws NoSuchFieldException, IllegalAccessException {
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

	private static Block createBlock(final Account account, final int timeStamp, final long height) throws NoSuchFieldException, IllegalAccessException {
		final Block block = new Block(account, Hash.ZERO, Hash.ZERO, new TimeInstant(timeStamp), new BlockHeight(height));
		block.sign();

		final AccountCache accountCache = new AccountCache();
		accountCache.addAccountToCache(account.getAddress());
		return roundTripBlock(accountCache, block);
	}

	private static BlockScorer createScorer() {
		return new BlockScorer(Mockito.mock(PoiFacade.class));
	}

	private static class TestContext {
		private final PoiFacade poiFacade;
		private final BlockScorer scorer;

		private TestContext() {
			this(new PoiFacade((blockHeight, accountStates, scoringAlg) -> {
				for (final PoiAccountState accountState : accountStates) {
					final Amount balance = accountState.getWeightedBalances().getUnvested(blockHeight);
					final double importance = balance.getNumMicroNem() / 1000.0;
					accountState.getImportanceInfo().setImportance(blockHeight, importance);
				}
			}));
		}

		private TestContext(final PoiFacade poiFacade) {
			this.poiFacade = poiFacade;
			this.scorer = new BlockScorer(this.poiFacade);
		}

		private Account createAccountWithBalance(final long balance) {
			final Account account = Utils.generateRandomAccount();
			final PoiAccountState accountState = this.poiFacade.findStateByAddress(account.getAddress());
			accountState.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(balance));
			accountState.setHeight(BlockHeight.ONE);
			return account;
		}

		private AccountImportance getImportanceInfo(final Account account) {
			return this.poiFacade.findStateByAddress(account.getAddress()).getImportanceInfo();
		}
	}
}
