package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.util.logging.Logger;

public class BlockScorerTest {
	private static final Logger LOGGER = Logger.getLogger(BlockScorerTest.class.getName());

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
		final Account blockSigner = new Account(keyPair);
		final BlockScorer scorer = new BlockScorer();
		final Block previousBlock = new Block(blockSigner, Hash.ZERO, new Hash(HASH_BYTES), TimeInstant.ZERO, new BlockHeight(11));

		// Act:
		final BigInteger hit = scorer.calculateHit(previousBlock);

		// Assert:
		Assert.assertThat(hit, IsEqual.equalTo(new BigInteger("20A80E8435E74", 16)));
	}

	@Test
	public void targetIsZeroWhenBalanceIsZero() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(0);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 101, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsZero() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 1, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsZeroWhenElapsedTimeIsNegative() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 101, 11);
		final Block block = createBlock(blockSigner, 1, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert:
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.ZERO));
	}

	@Test
	public void targetIsCalculatedCorrectlyWhenBalanceIsNonZero() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block = createBlock(blockSigner, 101, 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert: (time-difference * block-signer-balance * magic-number)
		final BigInteger expectedTarget = BigInteger.valueOf(100 * 72)
				.multiply(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BlockDifficulty.INITIAL_DIFFICULTY.asBigInteger());

		Assert.assertThat(target, IsEqual.equalTo(expectedTarget));
	}

	@Test
	public void targetIncreasesAsTimeElapses() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(72);

		final Block previousBlock = createBlock(Utils.generateRandomAccount(), 1, 11);
		final Block block1 = createBlock(blockSigner, 101, 11);
		final Block block2 = createBlock(blockSigner, 201, 11);

		// Act:
		final BigInteger target1 = scorer.calculateTarget(previousBlock, block1);
		final BigInteger target2 = scorer.calculateTarget(previousBlock, block2);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertTrue(target1.compareTo(target2) < 0);
	}


	private static Block createBlock(final Account account, int timeStamp, long height) {
		return new Block(account, Hash.ZERO, Hash.ZERO, new TimeInstant(timeStamp), new BlockHeight(height));
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
