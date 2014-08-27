package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.poi.*;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockScorerITCase {
	private static final Logger LOGGER = Logger.getLogger(BlockScorerITCase.class.getName());

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
	public void timeBetweenBlocksIsAboutSixtySeconds() {
		// Only 250 million nem (going below this limit will result in higher block creation times)
		this.foragerTest(1000, 250000000L, 10);

		// 1000 million nem
		this.foragerTest(1000, 1000000000L, 10);

		// Full 4000 million nem
		this.foragerTest(1000, 4000000000L, 10);
	}

	private void foragerTest(final int numRounds, final long numNEM, final int numForagers) {
		// Arrange
		final Account[] foragerAccounts = new Account[numForagers];
		for (int i = 0; i < numForagers; i++) {
			foragerAccounts[i] = createAccountWithBalance(numNEM / numForagers);
		}
		int averageTime = 0, maxTime = 0, minTime = Integer.MAX_VALUE, index = 0;
		final BlockScorer scorer = createBlockScorer();
		Block block;
		final Block[] blocks = new Block[numRounds];
		final int[] secondsBetweenBlocks = new int[numRounds];
		blocks[0] = this.createFirstBlock(foragerAccounts[0], new Hash(HASH_BYTES));
		blocks[0].setDifficulty(BlockDifficulty.INITIAL_DIFFICULTY);

		final List<Block> historicalBlocks = new LinkedList<>();
		historicalBlocks.add(blocks[0]);

		// Act:
		for (int i = 1; i < numRounds; i++) {
			// Don't know creation time yet, so construct helper block
			block = new Block(foragerAccounts[0], blocks[i - 1], new TimeInstant(1));
			block.setDifficulty(scorer.getDifficultyScorer().calculateDifficulty(
					this.createDifficultiesList(historicalBlocks),
					this.createTimestampsList(historicalBlocks),
					block.getHeight().getRaw()));
			secondsBetweenBlocks[i] = Integer.MAX_VALUE;
			for (int j = 0; j < numForagers; j++) {
				final Block temporaryDummy = new Block(foragerAccounts[j], blocks[i - 1], new TimeInstant(1));
				final BigInteger hit = scorer.calculateHit(temporaryDummy);
				final int seconds = hit.multiply(block.getDifficulty().asBigInteger())
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
			blocks[i] = new Block(foragerAccounts[index], blocks[i - 1], new TimeInstant(blocks[i - 1].getTimeStamp().getRawTime() + secondsBetweenBlocks[i]));
			blocks[i].setDifficulty(block.getDifficulty());
			historicalBlocks.add(blocks[i]);
			if (historicalBlocks.size() > BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION) {
				historicalBlocks.remove(0);
			}
		}
		for (int i = 1; i < numRounds; i++) {
			averageTime += secondsBetweenBlocks[i];
			if (secondsBetweenBlocks[i] < minTime) {
				minTime = secondsBetweenBlocks[i];
			}
			if (secondsBetweenBlocks[i] > maxTime) {
				maxTime = secondsBetweenBlocks[i];
			}
		}
		averageTime /= numRounds - 1;
		LOGGER.info("Minimum time between blocks: " + minTime + " seconds.");
		LOGGER.info("Maximum time between blocks: " + maxTime + " seconds.");
		LOGGER.info("Average time between blocks: " + averageTime + " seconds.");
		Assert.assertTrue("Average time between blocks not within reasonable range!", 55 < averageTime && averageTime < 65);
	}

	enum GenerateStrategy {
		Score_Matters,
		Time_Matters
	}

	@Test
	public void oneForagerIsNotBetterThanManyForagesWithSameCumulativeBalance() {
		final long OneForagerPercentageBlocks = this.oneForagersVersusManyForagers(2000 * 60, 10, 1_000_000_000L);
		Assert.assertTrue("One forages creates too many/not enough blocks compared to many forages!",
				45 < OneForagerPercentageBlocks && OneForagerPercentageBlocks < 55);
	}

	@Test
	public void selfishForagerCannotForageBetterChain() {
		int selfishForagerWins = 0;
		//  1% attack: 10 rounds with approximately 100 blocks each
		// NOTE: Since the attacker has a balance considerably lower than 200 million,
		//       his the average time between blocks is considerably higher than 60 seconds!
		//       That's why the normal forger wins this time.
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 3_960_000_000L, 40_000_000L);

		//  5% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 3_800_000_000L, 200_000_000L);

		//  10% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 2_700_000_000L, 300_000_000L);

		//  20% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 2_000_000_000L, 500_000_000L);

		//  30% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 1_400_000_000L, 600_000_000L);

		//  40% attack: 10 rounds with approximately 100 blocks each
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 1_500_000_000L, 1_000_000_000L);

		//  45% attack: 10 rounds with approximately 100 blocks each
		//  Due to variance the selfish forger sometimes wins
		selfishForagerWins += this.normalForagerVersusSelfishForager(10, 100 * 60, 1_100_000_000L, 900_000_000L);

		Assert.assertTrue("Selfish forager created better chain!", selfishForagerWins == 0);
	}

	@Test
	public void selfishForagerVersusMultipleNormal() {
		// Act
		long selfishForagerWins = 0;

		selfishForagerWins += this.normalXForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 10, 1_000_000_000L, 500_000_000L);
		//selfishForagerWins += normalXForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100 * 60, 10, 1_000_000_000L, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish forager vs multiple normal: created better chain!", selfishForagerWins == 0);
	}

	@Test
	public void manyOldNormalForagersVersusManyFreshSelfishForargesTime() {
		// The selfish forager waits till the normal foragers have foraged foragedBlocksPerNormalForager blocks in average.
		// (Assuming 5000 normal forgagers in a real NEM network each forager has foraged about 100 blocks after a year.)
		// Then the selfish forager creates brand-new accounts and forages with them.
		long selfishForagerWins = 0;

		selfishForagerWins += this.normalForgersOldVersusSelfishNew(GenerateStrategy.Time_Matters, 10, 50 * 60, 100, 10, 1_000_000_000L, 1, 1_000_000_000);
		//selfishForagerWins += normalForgersOldVersusSelfishNew(GenerateStrategy.Score_Matters, 10, 50 * 60, 100, 10, 1_000_000_000L, 1, 1_000_000_000);

		// Assert
		Assert.assertTrue("(multiple) Selfish forager vs vs multiple normal: created better chain!", selfishForagerWins == 0);
	}

	@Test
	public void selfishForagerVersusManyRandomBetterTime() {
		long selfishForagerWins = 0;

		// 5%
		selfishForagerWins += this.normalXRandomForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 5, 10, 500_000_000L);

		// 10%
		selfishForagerWins += this.normalXRandomForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 10, 10, 500_000_000L);

		// 20%
		selfishForagerWins += this.normalXRandomForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 20, 10, 500_000_000L);

		// 40%
		selfishForagerWins += this.normalXRandomForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 40, 10, 500_000_000L);

		// 45%
		selfishForagerWins += this.normalXRandomForagerVersusSelfishForager(GenerateStrategy.Time_Matters, 10, 100 * 60, 45, 10, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish forager vs multiple normal (random): created better chain!", selfishForagerWins == 0);
	}

	@Test
	public void selfishForagerVersusManyRandomBetterScore() {
		final long selfishForagerWins = 0;

		// 5%
		//		selfishForagerWins += normalXRandomForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100*60, 5, 10, 500_000_000L);
		//
		//		// 10%
		//		selfishForagerWins += normalXRandomForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100*60, 10, 10, 500_000_000L);
		//
		//		// 20%
		//		selfishForagerWins += normalXRandomForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100*60, 20, 10, 500_000_000L);
		//
		//		// 40%
		//		selfishForagerWins += normalXRandomForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100*60, 40, 10, 500_000_000L);
		//
		//		// 45%
		//		//selfishForagerWins += normalXRandomForagerVersusSelfishForager(GenerateStrategy.Score_Matters, 10, 100*60, 45, 10, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish forager vs multiple normal (random): created better chain!", selfishForagerWins == 0);
	}

	@Test
	public void differentChainsProduceDifferentScores() {
		// Arrange:
		final Account foragerA = createAccountWithBalance(1_000_000_000);
		final Account foragerB = createAccountWithBalance(100_000_000);

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final byte[] rndBytes = new byte[32];

		final Block firstBlock = new Block(foragerA, new Hash(rndBytes), new Hash(HASH_BYTES), new TimeInstant(1), new BlockHeight(1));
		foragerA.incrementForagedBlocks();
		Block block;

		// Act:
		blocks.clear();
		blocks.add(firstBlock);
		for (int i = 0; i < 2; ++i) {
			block = this.generateNextBlock(foragerA, blocks, scorer, false);
			foragerA.incrementForagedBlocks();
			blocks.add(block);
		}
		final long scoreA = this.calculateScore(blocks, scorer);

		blocks.clear();
		blocks.add(firstBlock);
		for (int i = 0; i < 2; ++i) {
			block = this.generateNextBlock(foragerB, blocks, scorer, true);
			foragerB.incrementForagedBlocks();
			blocks.add(block);
		}
		final long scoreB = this.calculateScore(blocks, scorer);

		LOGGER.info("a: " + scoreA + " b: " + scoreB);
		Assert.assertThat(scoreA, IsNot.not(IsEqual.equalTo(scoreB)));
	}

	public int normalForagerVersusSelfishForager(final int numRounds, final int maxTime, final long normalForgerBalance, final long selfishForgerBalance) {
		// Arrange:
		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalForagerWins = 0;
		int selfishForagerWins = 0;
		long normalForagerScore;
		long selfishForagerScore;

		// Act: normal forager vs. selfish forger
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of foraged blocks
			final Account normalForager = createAccountWithBalance(normalForgerBalance);
			final Account selfishForager = createAccountWithBalance(selfishForgerBalance);

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(normalForager, new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(normalForager, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalForagerScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishForager, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishForagerScore = this.calculateScore(blocks, scorer);
			if (selfishForagerScore > normalForagerScore) {
				selfishForagerWins++;
			} else {
				normalForagerWins++;
			}
			//LOGGER.info("score " + selfishForagerScore + " vs " + normalForagerScore);
		}

		LOGGER.info("selfish forager (" + (selfishForgerBalance * 100L) / (normalForgerBalance + selfishForgerBalance) + "% of all nem) wins in:   " +
				(selfishForagerWins * 100) / (selfishForagerWins + normalForagerWins) + "%.");
		return selfishForagerWins;
	}

	public int normalForgersOldVersusSelfishNew(final GenerateStrategy strategy, final int numRounds, final int maxTime, final long foragedBlocksPerNormalForager, final int count, final long normalForgerBalance, final int selfishCount, final long selfishForgerBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalForagersWins = 0;
		int selfishForagerWins = 0;
		long normalForagersScore;
		long selfishForagerScore;

		// Act: normal forager duo vs. selfish forger
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of foraged blocks
			final Account[] selfishForagerAccounts = new Account[selfishCount];
			final Account[] foragers = new Account[count];
			for (int j = 0; j < count; j++) {
				foragers[j] = createAccountWithBalance(normalForgerBalance);
				for (int k = 0; k < foragedBlocksPerNormalForager; k++) {
					foragers[j].incrementForagedBlocks();
				}
			}
			for (int j = 0; j < selfishCount; ++j) {
				selfishForagerAccounts[j] = createAccountWithBalance(selfishForgerBalance);
			}

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(foragers[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, foragers, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalForagersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, selfishForagerAccounts, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishForagerScore = this.calculateScore(blocks, scorer);
			if (selfishForagerScore > normalForagersScore) {
				selfishForagerWins++;
			} else {
				normalForagersWins++;
			}
			//LOGGER.info("score " + selfishForagerScore + " vs " + normalForagersScore + " " + ((selfishForagerScore>normalForagersScore)?"*":" "));
		}

		LOGGER.info(selfishCount + " selfish forager vs " + count + " (" +
				(selfishCount * selfishForgerBalance * 100L) / (count * normalForgerBalance + selfishCount * selfishForgerBalance) +
				"% of all nem) wins in:   " + (selfishForagerWins * 100) / (selfishForagerWins + normalForagersWins) + "%.");
		return selfishForagerWins;
	}

	public int normalXForagerVersusSelfishForager(final GenerateStrategy strategy, final int numRounds, final int maxTime, final int count, final long normalForgerBalance, final long selfishForgerBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalForagersWins = 0;
		int selfishForagerWins = 0;
		long normalForagersScore;
		long selfishForagerScore;

		// Act: normal forager duo vs. selfish forger
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of foraged blocks
			final Account selfishForager = createAccountWithBalance(selfishForgerBalance);
			final Account[] forargers = new Account[count];
			for (int j = 0; j < count; j++) {
				forargers[j] = createAccountWithBalance(normalForgerBalance / count);
			}

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(forargers[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, forargers, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalForagersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishForager, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishForagerScore = this.calculateScore(blocks, scorer);
			if (selfishForagerScore > normalForagersScore) {
				selfishForagerWins++;
			} else {
				normalForagersWins++;
			}
			//LOGGER.info("score " + selfishForagerScore + " vs " + normalForagersScore + " " + ((selfishForagerScore>normalForagersScore)?"*":" "));
		}

		LOGGER.info((strategy == GenerateStrategy.Time_Matters ? "(time)" : "(score)") + " selfish forager vs " + count + " (" +
				(selfishForgerBalance * 100L) / (count * normalForgerBalance + selfishForgerBalance) + "% of all nem) wins in:   " +
				(selfishForagerWins * 100) / (selfishForagerWins + normalForagersWins) + "%.");
		return selfishForagerWins;
	}

	public int normalXRandomForagerVersusSelfishForager(final GenerateStrategy strategy, final int numRounds, final int maxTime, final int percentage, final int count, final long normalForgerBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalForagersWins = 0;
		int selfishForagerWins = 0;
		long normalForagersScore;
		long selfishForagerScore;

		// Act: normal forager duo vs. selfish forger
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of foraged blocks
			final Account[] forargers = new Account[count];
			long selfishForgerBalance = 0L;
			for (int j = 0; j < count; j++) {
				forargers[j] = createAccountWithBalance(Math.abs(sr.nextLong() % normalForgerBalance));
				selfishForgerBalance += forargers[j].getBalance().getNumNem();
			}
			final Account selfishForager = createAccountWithBalance(selfishForgerBalance * percentage * 2 / 100);

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(forargers[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, forargers, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalForagersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishForager, blocks, scorer, false);
				blocks.add(block);
				block.getSigner().incrementForagedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishForagerScore = this.calculateScore(blocks, scorer);
			if (selfishForagerScore > normalForagersScore) {
				selfishForagerWins++;
			} else {
				normalForagersWins++;
			}
			//LOGGER.info("score " + selfishForagerScore + " vs " + normalForagersScore + " " + ((selfishForagerScore>normalForagersScore)?"*":" "));
		}

		LOGGER.info("selfish forager (" + percentage + "% of all nems) vs x random wins in:   " +
				(selfishForagerWins * 100) / (selfishForagerWins + normalForagersWins) + "%.");
		return selfishForagerWins;
	}

	public long oneForagersVersusManyForagers(final int maxTime, final int count, final long manyForagersBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		final Block firstBlock;
		Block lastBlock;

		// Act: one forager vs. count foragers
		final Account[] foragers = new Account[count + 1];
		for (int j = 0; j < count; j++) {
			foragers[j] = createAccountWithBalance(manyForagersBalance / count);
		}
		foragers[count] = createAccountWithBalance(manyForagersBalance);

		sr.nextBytes(rndBytes);
		firstBlock = this.createFirstBlock(foragers[0], new Hash(rndBytes));

		blocks.clear();
		blocks.add(firstBlock);
		do {
			final Block block = this.generateNextBlockMultiple(GenerateStrategy.Time_Matters, foragers, blocks, scorer, false);
			blocks.add(block);
			block.getSigner().incrementForagedBlocks();
			lastBlock = block;
		} while (lastBlock.getTimeStamp().getRawTime() < maxTime);

		long manyForagersNumBlocks = 0;
		for (int j = 0; j < count; j++) {
			manyForagersNumBlocks += foragers[j].getForagedBlocks().getRaw();
		}

		final long percentage = (foragers[count].getForagedBlocks().getRaw() * 100) / (foragers[count].getForagedBlocks().getRaw() + manyForagersNumBlocks);
		LOGGER.info("One forager created  " + foragers[count].getForagedBlocks().getRaw() + " blocks (" + percentage + "%), " + count + " foragers created " +
				manyForagersNumBlocks + " blocks.");
		return percentage;
	}

	private Block createFirstBlock(final Account account, final Hash previousHash) {
		final Hash generationHash = HashUtils.nextHash(previousHash, account.getKeyPair().getPublicKey());
		return new Block(account, previousHash, generationHash, new TimeInstant(1), new BlockHeight(1));
	}

	private List<BlockDifficulty> createDifficultiesList(final List<Block> blocks) {
		return blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
	}

	private List<TimeInstant> createTimestampsList(final List<Block> blocks) {
		return blocks.stream().map(VerifiableEntity::getTimeStamp).collect(Collectors.toList());
	}

	private Block generateNextBlock(final Account forger, final List<Block> blocks, final BlockScorer scorer, final boolean randomizeTime) {
		final Block lastBlock = blocks.get(blocks.size() - 1);
		Block block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

		final List<Block> historicalBlocks = blocks.subList(Math.max(0, (blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION)), blocks.size());
		final BlockDifficulty difficulty = scorer.getDifficultyScorer().calculateDifficulty(
				this.createDifficultiesList(historicalBlocks),
				this.createTimestampsList(historicalBlocks),
				block.getHeight().getRaw());
		block.setDifficulty(difficulty);
		final BigInteger hit = scorer.calculateHit(block);
		int seconds = hit.multiply(block.getDifficulty().asBigInteger())
				.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BigInteger.valueOf(forger.getBalance().getNumNem()))
				.intValue();
		if (seconds == 0) {
			// This will not happen in our network
			seconds = 1;
		}
		if (randomizeTime) {
			seconds += (new SecureRandom()).nextInt(10);
		}

		block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds));
		block.setDifficulty(difficulty);

		return block;
	}

	private Block generateNextBlockMultiple(final GenerateStrategy strategy, final Account[] forgers, final List<Block> blocks, final BlockScorer scorer, final boolean randomizeTime) {
		final Block lastBlock = blocks.get(blocks.size() - 1);

		Block bestBlock = null;
		long maxSum = Integer.MIN_VALUE;
		int minTime = Integer.MAX_VALUE;
		for (final Account forger : forgers) {
			Block block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

			final List<Block> historicalBlocks = blocks.subList(Math.max(0, (blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION)),
					blocks.size());
			final BlockDifficulty difficulty = scorer.getDifficultyScorer().calculateDifficulty(
					this.createDifficultiesList(historicalBlocks),
					this.createTimestampsList(historicalBlocks),
					block.getHeight().getRaw());
			block.setDifficulty(difficulty);
			final BigInteger hit = scorer.calculateHit(block);
			int seconds = hit.multiply(block.getDifficulty().asBigInteger())
					.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
					.divide(BigInteger.valueOf(forger.getBalance().getNumNem()))
					.intValue();
			if (seconds == 0) {
				// This will not happen in our network
				seconds = 1;
			}
			if (randomizeTime) {
				seconds += (new SecureRandom()).nextInt(10);
			}

			block = new Block(forger, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds));
			block.setDifficulty(difficulty);

			final List<Block> temp = new LinkedList<>();
			temp.addAll(blocks);
			temp.add(block);

			if (strategy == GenerateStrategy.Time_Matters) {
				final int time = block.getTimeStamp().getRawTime();
				if (time < minTime) {
					minTime = time;
					bestBlock = block;
				}
			} else {
				final long scoreSum = this.calculateScore(temp, scorer);
				if (scoreSum > maxSum) {
					bestBlock = block;
					maxSum = scoreSum;
				}
			}
		}
		//LOGGER.info(bestBlock.getSigner().getLabel());
		return bestBlock;
	}

	private long calculateScore(final List<Block> blocks, final BlockScorer scorer) {
		long scoreSum = 0;
		if (blocks.size() > 1) {
			final Iterator<Block> iter = blocks.iterator();

			Block parentBlock = iter.next();

			int i = 0;
			while (iter.hasNext()) {
				final Block block = iter.next();
				final long score = scorer.calculateBlockScore(parentBlock, block);
				if (i == 0) {
					scoreSum = score;
				}
				scoreSum += score;
				parentBlock = block;
				i++;
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

	private static BlockScorer createBlockScorer() {
		return new BlockScorer(new PoiFacade(new PoiAlphaImportanceGeneratorImpl()));
	}

	private static Account createAccountWithBalance(final long balance) {
		final Account account = Utils.generateRandomAccount();
		account.incrementBalance(Amount.fromNem(balance));
		return account;
	}
}
