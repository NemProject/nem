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
import org.nem.nis.test.*;

import java.lang.reflect.Field;
import java.math.BigInteger;

public class BlockScorerTest {

	private static final byte[] PUBKEY_BYTES = new byte[] {
			(byte) 0x02,
			(byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7,
			(byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7,
			(byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
			(byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7
	};

	private static final byte[] HASH_BYTES = new byte[] {
		(byte) 0xF7, (byte) 0xF6, (byte) 0xF5, (byte) 0xF4, (byte) 0xF3, (byte) 0xF2, (byte) 0xF1, (byte) 0xF0,
		(byte) 0xE7, (byte) 0xE6, (byte) 0xE5, (byte) 0xE4, (byte) 0xE3, (byte) 0xE2, (byte) 0xE1, (byte) 0xE0,
		(byte) 0xD7, (byte) 0xD6, (byte) 0xD5, (byte) 0xD4, (byte) 0xD3, (byte) 0xD2, (byte) 0xD1, (byte) 0xD0,
		(byte) 0xC7, (byte) 0xC6, (byte) 0xC5, (byte) 0xC4, (byte) 0xC3, (byte) 0xC2, (byte) 0xC1, (byte) 0xC0
	};
	
	@Test
	public void hitIsCalculatedCorrectly() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(new PublicKey(PUBKEY_BYTES));
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = accountAnalyzer.addAccountToCache(new Account(keyPair).getAddress());
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);
		final Block previousBlock = new Block(blockSigner, Hash.ZERO, new Hash(HASH_BYTES), TimeInstant.ZERO, new BlockHeight(11));

		// Act:
		final BigInteger hit = scorer.calculateHit(previousBlock);

		// Assert:
		Assert.assertThat(hit, IsEqual.equalTo(new BigInteger("20A80E8435E74", 16)));
	}

	@Test
	public void targetIsZeroWhenBalanceIsZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = createAccountWithBalance(accountAnalyzer, 0);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		final Block previousBlock = createBlock(accountAnalyzer, Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(accountAnalyzer, blockSigner, 101, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = createAccountWithBalance(accountAnalyzer, 72);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		final Block previousBlock = createBlock(accountAnalyzer, Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(accountAnalyzer, blockSigner, 1, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsNegative() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = createAccountWithBalance(accountAnalyzer, 72);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		final Block previousBlock = createBlock(accountAnalyzer, Utils.generateRandomAccount(), 101, 11);
		final Block block = createBlock(accountAnalyzer, blockSigner, 1, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}


	@Test
	public void targetIsCalculatedCorrectlyWhenBalanceIsNonZero() throws NoSuchFieldException, IllegalAccessException {
		// Arrange:
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = createAccountWithBalance(accountAnalyzer, 72);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		final Block previousBlock = createBlock(accountAnalyzer, Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(accountAnalyzer, blockSigner, 101, 11);

		block.setDifficulty(new BlockDifficulty((long)60E12));
		accountAnalyzer.recalculateImportances(block.getHeight());
		blockSigner.getImportanceInfo().setImportance(block.getHeight(), 1572);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

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
		final MockBlockScorerAnalyzer accountAnalyzer = new MockBlockScorerAnalyzer();
		final Account blockSigner = createAccountWithBalance(accountAnalyzer, 72);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		final Block previousBlock = createBlock(accountAnalyzer, Utils.generateRandomAccount(), 1, 11);
		final Block block1 = createBlock(accountAnalyzer, blockSigner, 101, 11);
		final Block block2 = createBlock(accountAnalyzer, blockSigner, 201, 11);

		// Act:
		final BigInteger target1 = scorer.calculateTarget(previousBlock, block1);
		final BigInteger target2 = scorer.calculateTarget(previousBlock, block2);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertTrue(target1.compareTo(target2) < 0);
	}

	//region calculateForgerBalance

	@Test
	public void calculateForgerBalanceDerivesBalanceFromImportance() {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final Block block = NisUtils.createRandomBlockWithHeight(94);
		block.getSigner().getImportanceInfo().setImportance(new BlockHeight(94), 0.75);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		// Act:
		final long balance = scorer.calculateForgerBalance(block);

		// Assert:
		Assert.assertThat(balance, IsEqual.equalTo(3_000_000_000L));
	}

	@Test
	public void calculateForgerBalanceCallsRecalculateImportancesForGroupedBlock() {
		// Arrange:
		assertRecalculateImportancesCalledForHeight(1, 1);
		assertRecalculateImportancesCalledForHeight(30, 1);
		assertRecalculateImportancesCalledForHeight(31, 1);
		assertRecalculateImportancesCalledForHeight(32, 32);
		assertRecalculateImportancesCalledForHeight(33, 32);
		assertRecalculateImportancesCalledForHeight(90, 63);
		assertRecalculateImportancesCalledForHeight(111, 94);
	}

	private static void assertRecalculateImportancesCalledForHeight(final long height, final long groupedHeight) {
		// Arrange:
		final AccountAnalyzer accountAnalyzer = Mockito.mock(AccountAnalyzer.class);
		final Block block = NisUtils.createRandomBlockWithHeight(height);
		block.getSigner().getImportanceInfo().setImportance(new BlockHeight(groupedHeight), 0.75);
		final BlockScorer scorer = new BlockScorer(accountAnalyzer);

		// Act:
		scorer.calculateForgerBalance(block);

		// Assert:
		Mockito.verify(accountAnalyzer, Mockito.times(1)).recalculateImportances(new BlockHeight(groupedHeight));
	}

	//endregion

	private static Block roundTripBlock(AccountLookup accountLookup, Block block) throws NoSuchFieldException, IllegalAccessException {
		final VerifiableEntity.DeserializationOptions options = VerifiableEntity.DeserializationOptions.VERIFIABLE;

		final Deserializer deserializer = Utils.roundtripSerializableEntity(block, accountLookup);
		Block b = new Block(deserializer.readInt("type"), options, deserializer);

		Field field = b.getClass().getDeclaredField("generationHash");
		field.setAccessible(true);
		field.set(b, block.getGenerationHash());

		field = b.getClass().getDeclaredField("prevBlockHash");
		field.setAccessible(true);
		field.set(b, block.getPreviousBlockHash());

		return b;
	}

	private static Block createBlock(final AccountLookup accountLookup, final Account account, int timeStamp, long height) throws NoSuchFieldException, IllegalAccessException {
		final Block block = new Block(account, Hash.ZERO, Hash.ZERO, new TimeInstant(timeStamp), new BlockHeight(height));
		block.sign();
		return roundTripBlock(accountLookup, block);
	}

	private Account createAccountWithBalance(MockBlockScorerAnalyzer mockBlockScorerAnalyzer, long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		account.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(balance));
		final Account blockSigner = mockBlockScorerAnalyzer.addAccountToCache(account.getAddress());
		blockSigner.incrementBalance(Amount.fromNem(balance));
		blockSigner.getWeightedBalances().addReceive(BlockHeight.ONE, Amount.fromNem(balance));
		return account;
	}
}
