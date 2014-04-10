package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.ByteUtils;

import java.math.BigInteger;
import java.util.Arrays;

public class BlockScorerTest {

	private static final byte[] SIGNATURE_BYTES = new byte[] {
			(byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7,
			(byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7,
			(byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
			(byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7,
			0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77,
			0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67,
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
			0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,
	};

	@Test
	public void hitIsCalculatedCorrectly() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Block block = new Block(Utils.generateRandomAccount(), Hash.ZERO, TimeInstant.ZERO, 11);
		block.setSignature(new Signature(SIGNATURE_BYTES));

		// Act:
		final BigInteger hit = scorer.calculateHit(block);

		// Assert:
		Assert.assertThat(hit, IsEqual.equalTo(new BigInteger("F2F3F4F5F6F7E0E1", 16)));
	}

	@Test
	public void targetIsZeroWhenBalanceIsZero() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(0);

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block = new Block(blockSigner, Hash.ZERO, new TimeInstant(101), 11);

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

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block = new Block(blockSigner, Hash.ZERO, new TimeInstant(1), 11);

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

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block = new Block(blockSigner, Hash.ZERO, new TimeInstant(101), 11);

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

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block = new Block(blockSigner, Hash.ZERO, new TimeInstant(101), 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.valueOf(100 * 72 * 614891469L)));
	}

	@Test
	public void targetIncreasesAsTimeElapses() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(72);

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block1 = new Block(blockSigner, Hash.ZERO, new TimeInstant(101), 11);
		final Block block2 = new Block(blockSigner, Hash.ZERO, new TimeInstant(201), 11);

		// Act:
		final BigInteger target1 = scorer.calculateTarget(previousBlock, block1);
		final BigInteger target2 = scorer.calculateTarget(previousBlock, block2);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertTrue(target1.compareTo(target2) < 0);
	}

	@Test
	public void targetCalculationGivesExternalSignerAccountPrecedence() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Account blockSigner = createAccountWithBalance(42);

		final Block previousBlock = new Block(Utils.generateRandomAccount(), Hash.ZERO, new TimeInstant(1), 11);
		final Block block = new Block(createAccountWithBalance(11), Hash.ZERO, new TimeInstant(101), 11);

		// Act:
		final BigInteger target = scorer.calculateTarget(previousBlock, block, blockSigner);

		// Assert: (time-difference * block-signer-balance * magic-number)
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.valueOf(100 * 42 * 614891469L)));
	}

	@Test
	public void blockScoreIsCalculatedCorrectly() {
		// Arrange:
		final BlockScorer scorer = new BlockScorer();
		final Block block = new Block(Utils.generateRandomAccount(), Hash.ZERO, TimeInstant.ZERO, 11);
		block.setSignature(new Signature(SIGNATURE_BYTES));

		long blockScoreHashPart = Math.abs(ByteUtils.bytesToInt(Arrays.copyOfRange(HashUtils.calculateHash(block).getRaw(), 10, 14)));

		// Act:
		final long blockScore = scorer.calculateBlockScore(block);

		// Assert:
		Assert.assertThat(blockScore, IsEqual.equalTo(Math.abs(0xE2E3E4E5) + blockScoreHashPart));
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
