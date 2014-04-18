package org.nem.nis;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
		final Block previousBlock = new Block(blockSigner, new Hash(HASH_BYTES), TimeInstant.ZERO, new BlockHeight(11));


		// Act:
		final BigInteger hit = scorer.calculateHit(previousBlock, blockSigner);

		// Assert:
		Assert.assertThat(hit, IsEqual.equalTo(new BigInteger("bd3bef408122b58c", 16)));
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
		Assert.assertThat(target, IsEqual.equalTo(BigInteger.valueOf(100 * 72).multiply(BlockScorer.TWO_TO_THE_POWER_OF_64).divide(BigInteger.valueOf(BlockScorer.INITIAL_DIFFICULTY))));
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

	@Test
	public void timeBetweenBlocksIsAboutSixtySeconds() {
		// Only 250 million nem (going below this limit will result in higher block creation times)
		foragerTest(1000, 250000000L, 10);

		// 1000 million nem
		foragerTest(1000, 1000000000L, 10);

		// Full 4000 million nem
		foragerTest(1000, 4000000000L, 10);
	}
	
	private void foragerTest(int numRounds, long numNEM, int numForagers) {
		// Arrange
		final Account[] foragerAccounts = new Account[numForagers];
		for (int i=0; i<numForagers; i++) {
			foragerAccounts[i]	= createAccountWithBalance(numNEM/numForagers);
		}
		int averageTime = 0, maxTime = 0, minTime = Integer.MAX_VALUE, index=0;
		final BlockScorer scorer = new BlockScorer();
		Block block;
		Block[] blocks = new Block[numRounds];
		int[] secondsBetweenBlocks = new int[numRounds];
		Hash hash = new Hash(HASH_BYTES);
		blocks[0] = new Block(foragerAccounts[0], hash, new TimeInstant(1), new BlockHeight(1));
		blocks[0].setDifficulty(BlockScorer.INITIAL_DIFFICULTY);
		List<Block> historicalBlocks = new LinkedList<>();
		historicalBlocks.add(blocks[0]);
		
		// Act:
		for (int i=1; i<numRounds; i++) {
			// Don't know creation time yet, so construct helper block
			block = new Block(foragerAccounts[0], blocks[i-1], new TimeInstant(1));
			block.setDifficulty(scorer.calculateDifficulty(historicalBlocks));
			secondsBetweenBlocks[i] = Integer.MAX_VALUE;
			for (int j=0; j<numForagers; j++) {
				BigInteger hit = scorer.calculateHit(blocks[i-1], foragerAccounts[j]);
				int seconds = hit.multiply(BigInteger.valueOf(block.getDifficulty()))
								 .divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
								 .divide(BigInteger.valueOf(foragerAccounts[j].getBalance().getNumNem()))
								 .intValue();
				if (seconds < secondsBetweenBlocks[i]) {
					secondsBetweenBlocks[i] = seconds;
					index = j;
				}
			}
			if (secondsBetweenBlocks[i] == 0) {
				// This will not happen in our network
				secondsBetweenBlocks[i] = 1;
			}
			blocks[i] = new Block(foragerAccounts[index], blocks[i-1], new TimeInstant(blocks[i-1].getTimeStamp().getRawTime() + secondsBetweenBlocks[i]));
			blocks[i].setDifficulty(block.getDifficulty());
			historicalBlocks.add(blocks[i]);
			if (historicalBlocks.size() > BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION) {
				historicalBlocks.remove(0);
			}
		}
		for (int i=1; i<numRounds; i++) {
			averageTime += secondsBetweenBlocks[i];
			if (secondsBetweenBlocks[i] < minTime) {
				minTime = secondsBetweenBlocks[i];
			}
			if (secondsBetweenBlocks[i] > maxTime) {
				maxTime = secondsBetweenBlocks[i];
			}
		}
		averageTime /= numRounds-1;
		LOGGER.info("Minimum time between blocks: " + minTime + " seconds.");
		LOGGER.info("Maximum time between blocks: " + maxTime + " seconds.");
		LOGGER.info("Average time between blocks: " + averageTime + " seconds.");
		Assert.assertTrue("Average time between blocks not within reasonable range!", 55 < averageTime && averageTime < 65);
	}
	
	@Test
	public void selfishForagerCannotForageBetterChain() {
		int selfishForagerWins = 0;
		//  1% attack: 10 rounds with approximately 100 blocks each
		// NOTE: Since the attacker has a balance considerably lower than 200 million,
		//       his the average time between blocks is considerably higher than 60 seconds!
		//       That's why the normal forger wins this time.
		selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 3_960_000_000L, 40_000_000L);

		//  5% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 3_800_000_000L, 200_000_000L);

		//  10% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 2_700_000_000L, 300_000_000L);

		//  20% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 2_000_000_000L, 500_000_000L);

		//  40% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 1_500_000_000L, 1_000_000_000L);

		//  50% attack: 10 rounds with approximately 100 blocks each
		// selfishForagerWins += normalForagerVersusSelfishForager(10, 100*60, 900_000_000L, 900_000_000L);
		
		Assert.assertTrue("Selfish forager created better chain!", selfishForagerWins == 0);
	}
	
	public int normalForagerVersusSelfishForager(int numRounds, int maxTime, long normalForgerBalance, long selfishForgerBalance) {
		// Arrange:
		Account normalForager = createAccountWithBalance(normalForgerBalance);
		Account selfishForager = createAccountWithBalance(selfishForgerBalance);
		
		final BlockScorer scorer = new BlockScorer();
		List<Block> blocks = new LinkedList<>();
		SecureRandom sr = new SecureRandom();
		byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalForagerWins = 0;
		int selfishForagerWins = 0;
		long normalForagerScore;
		long selfishForagerScore;

		// Act: normal forager vs. selfish forger
		for (int i=0; i<numRounds; i++) {
			sr.nextBytes(rndBytes);
			Hash hash = new Hash(rndBytes);
			firstBlock = new Block(normalForager, hash, new TimeInstant(1), new BlockHeight(1));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				Block block = generateNextBlock(normalForager, blocks, scorer, false); 
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalForagerScore = calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				Block block = generateNextBlock(selfishForager, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishForagerScore = calculateScore(blocks, scorer);
			if (selfishForagerScore > normalForagerScore) {
				selfishForagerWins++;
			}
			else {
				normalForagerWins++;
			}
		}

		LOGGER.info("selfish forager wins in:   " + (selfishForagerWins*100)/(selfishForagerWins+normalForagerWins) + "%.");
		return selfishForagerWins;
	}
	
	private Block generateNextBlock(Account forger, List<Block> blocks, BlockScorer scorer, boolean optimizeBlockScore) {
		Block lastBlock = blocks.get(blocks.size()-1);
		Block block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));
		
		List<Block> historicalBlocks = blocks.subList(Math.max(0, (int)(blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION)), blocks.size()-1);
		long difficulty = scorer.calculateDifficulty(historicalBlocks);
		block.setDifficulty(difficulty);
		BigInteger hit = scorer.calculateHit(lastBlock, forger);
		int seconds = hit.multiply(BigInteger.valueOf(block.getDifficulty()))
						 .divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
						 .divide(BigInteger.valueOf(forger.getBalance().getNumNem()))
						 .intValue();
		if (seconds == 0) {
			// This will not happen in our network
			seconds = 1;
		}

		block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds));
		block.setDifficulty(difficulty);
		
		// We abuse the totalFee field of the block to optimize the score for the next block.
		// In a real situation we would include a suited transaction for achieving the same goal.
		// Iterate a hundred times. For a uniform distributed random variable this is enough to get about 99% of the optimal score.

		// not needed anymore
//		if (optimizeBlockScore) {
//			Amount bestFee = null;
//			long bestScore = 0;
//			for (int i=0; i<100; i++) {
//				Amount fee = new Amount(i);
//				block.setTotalFee(fee);
//				byte[] hash = Hashes.sha3(ArrayUtils.concat(forger.getKeyPair().getPublicKey().getRaw(), HashUtils.calculateHash(block).getRaw()));
//				long score = Math.abs((long)ByteUtils.bytesToInt(Arrays.copyOfRange(hash, 10, 14)));
//				if (score > bestScore) {
//					bestScore = score;
//					bestFee = fee;
//				}
//			}
//			block.setTotalFee(bestFee);
//		}

		return block;
	}
	
	private long calculateScore(List<Block> blocks, BlockScorer scorer) {
		long scoreSum = 0;
		if (blocks.size() > 1) {
			Iterator<Block> iter = blocks.iterator();
			Block parentBlock = iter.next();
			while (iter.hasNext()) {
				Block block = iter.next();
				if (scoreSum == 0) {
					scoreSum += scorer.calculateBlockScore(parentBlock, block);
				}
				long score = scorer.calculateBlockScore(parentBlock, block);
				scoreSum += score;
				parentBlock = block;
			}
		}
		return scoreSum;
	}

	// TODO: not sure how to make sensible test now...
//	@Test
//	public void blockScoreIsCalculatedCorrectly() {
//		// Arrange:
//		final BlockScorer scorer = new BlockScorer();
//		final Block block = new Block(Utils.generateRandomAccount(), Hash.ZERO, TimeInstant.ZERO, 11);
//		block.setSignature(new Signature(SIGNATURE_BYTES));
//
//		long blockScoreHashPart = Math.abs(ByteUtils.bytesToInt(Arrays.copyOfRange(HashUtils.calculateHash(block).getRaw(), 10, 14)));
//
//		// Act:
//		final long blockScore = scorer.calculateBlockScore(block);
//
//		// Assert:
//		Assert.assertThat(blockScore, IsEqual.equalTo(Math.abs(0xE2E3E4E5) + blockScoreHashPart));
//	}

	private static Block createBlock(final Account account, int timeStamp, long height) {
		return new Block(account, Hash.ZERO, new TimeInstant(timeStamp), new BlockHeight(height));
	}

	private static Account createAccountWithBalance(long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
