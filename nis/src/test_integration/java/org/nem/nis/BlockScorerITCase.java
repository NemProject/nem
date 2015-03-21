package org.nem.nis;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.DefaultAccountStateCache;
import org.nem.nis.state.AccountInfo;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BlockScorerITCase {
	private static final Logger LOGGER = Logger.getLogger(BlockScorerITCase.class.getName());

	private static final byte[] HASH_BYTES = new byte[] {
			(byte)0xF7, (byte)0xF6, (byte)0xF5, (byte)0xF4, (byte)0xF3, (byte)0xF2, (byte)0xF1, (byte)0xF0,
			(byte)0xE7, (byte)0xE6, (byte)0xE5, (byte)0xE4, (byte)0xE3, (byte)0xE2, (byte)0xE1, (byte)0xE0,
			(byte)0xD7, (byte)0xD6, (byte)0xD5, (byte)0xD4, (byte)0xD3, (byte)0xD2, (byte)0xD1, (byte)0xD0,
			(byte)0xC7, (byte)0xC6, (byte)0xC5, (byte)0xC4, (byte)0xC3, (byte)0xC2, (byte)0xC1, (byte)0xC0
	};

	@Test
	public void timeBetweenBlocksIsAboutSixtySeconds() {
		// Only 250 million nem (going below this limit will result in higher block creation times)
		this.harvesterTest(1000, 250000000L, 10);

		// 1000 million nem
		this.harvesterTest(1000, 1000000000L, 10);

		// Full 4000 million nem
		this.harvesterTest(1000, 4000000000L, 10);
	}

	private void harvesterTest(final int numRounds, final long numNEM, final int numHarvesters) {
		// Arrange
		final AccountWithInfo[] harvesterAccounts = new AccountWithInfo[numHarvesters];
		for (int i = 0; i < numHarvesters; i++) {
			harvesterAccounts[i] = new AccountWithInfo(numNEM / numHarvesters);
		}
		int averageTime = 0, maxTime = 0, minTime = Integer.MAX_VALUE, index = 0;
		final BlockScorer scorer = createBlockScorer();
		Block block;
		final Block[] blocks = new Block[numRounds];
		final int[] secondsBetweenBlocks = new int[numRounds];
		blocks[0] = this.createFirstBlock(harvesterAccounts[0], new Hash(HASH_BYTES));
		blocks[0].setDifficulty(BlockDifficulty.INITIAL_DIFFICULTY);

		final List<Block> historicalBlocks = new LinkedList<>();
		historicalBlocks.add(blocks[0]);

		// Act:
		for (int i = 1; i < numRounds; i++) {
			// Don't know creation time yet, so construct helper block
			block = new Block(harvesterAccounts[0], blocks[i - 1], new TimeInstant(1));
			block.setDifficulty(scorer.getDifficultyScorer().calculateDifficulty(
					this.createDifficultiesList(historicalBlocks),
					this.createTimestampsList(historicalBlocks),
					block.getHeight().getRaw()));
			secondsBetweenBlocks[i] = Integer.MAX_VALUE;
			for (int j = 0; j < numHarvesters; j++) {
				final Block temporaryDummy = new Block(harvesterAccounts[j], blocks[i - 1], new TimeInstant(1));
				final BigInteger hit = scorer.calculateHit(temporaryDummy);
				final int seconds = hit.multiply(block.getDifficulty().asBigInteger())
						.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
						.divide(BigInteger.valueOf(harvesterAccounts[j].getInfo().getBalance().getNumNem()))
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
			blocks[i] = new Block(
					harvesterAccounts[index],
					blocks[i - 1],
					new TimeInstant(blocks[i - 1].getTimeStamp().getRawTime() + secondsBetweenBlocks[i]));
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
	public void oneHarvesterIsNotBetterThanManyHarvestersWithSameCumulativeBalance() {
		final long OneHarvesterPercentageBlocks = this.oneHarvestersVersusManyHarvesters(2000 * 60, 10, 1_000_000_000L);
		Assert.assertTrue("One harvests creates too many/not enough blocks compared to many harvesters!",
				45 < OneHarvesterPercentageBlocks && OneHarvesterPercentageBlocks < 55);
	}

	// TODO 20150320 J-B: this test is failing
	@Test
	public void selfishHarvesterCannotHarvestBetterChain() {
		int selfishHarvesterWins = 0;
		//  1% attack: 10 rounds with approximately 100 blocks each
		// NOTE: Since the attacker has a balance considerably lower than 200 million,
		//       his the average time between blocks is considerably higher than 60 seconds!
		//       That's why the normal harvester wins this time.
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 3_960_000_000L, 40_000_000L);

		//  5% attack: 10 rounds with approximately 100 blocks each
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 3_800_000_000L, 200_000_000L);

		//  10% attack: 10 rounds with approximately 100 blocks each
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 2_700_000_000L, 300_000_000L);

		//  20% attack: 10 rounds with approximately 100 blocks each
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 2_000_000_000L, 500_000_000L);

		//  30% attack: 10 rounds with approximately 100 blocks each
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 1_400_000_000L, 600_000_000L);

		//  40% attack: 10 rounds with approximately 100 blocks each
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 1_500_000_000L, 1_000_000_000L);

		//  45% attack: 10 rounds with approximately 100 blocks each
		//  Due to variance the selfish harvester sometimes wins
		selfishHarvesterWins += this.normalHarvesterVersusSelfishHarvester(10, 100 * 60, 1_100_000_000L, 900_000_000L);

		Assert.assertTrue("Selfish harvester created better chain!", selfishHarvesterWins == 0);
	}

	@Test
	public void selfishHarvesterVersusMultipleNormal() {
		// Act
		long selfishHarvesterWins = 0;

		selfishHarvesterWins += this.normalXHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 10, 1_000_000_000L, 500_000_000L);
		//selfishHarvesterWins += normalXHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100 * 60, 10, 1_000_000_000L, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish harvester vs multiple normal: created better chain!", selfishHarvesterWins == 0);
	}

	@Test
	public void manyOldNormalHarvestersVersusManyFreshSelfishHarvestersTime() {
		// The selfish harvester waits till the normal harvesters have harvested harvestedBlocksPerNormalHarvester blocks in average.
		// (Assuming 5000 normal harvesters in a real NEM network each harvester has harvested about 100 blocks after a year.)
		// Then the selfish harvester creates brand-new accounts and harvests with them.
		long selfishHarvesterWins = 0;

		selfishHarvesterWins += this.normalHarvestersOldVersusSelfishNew(GenerateStrategy.Time_Matters, 10, 50 * 60, 100, 10, 1_000_000_000L, 1, 1_000_000_000);
		//selfishHarvesterWins += normalHarvestersOldVersusSelfishNew(GenerateStrategy.Score_Matters, 10, 50 * 60, 100, 10, 1_000_000_000L, 1, 1_000_000_000);

		// Assert
		Assert.assertTrue("(multiple) Selfish harvester vs vs multiple normal: created better chain!", selfishHarvesterWins == 0);
	}

	// TODO 20150320 J-B: this test is failing
	@Test
	public void selfishHarvesterVersusManyRandomBetterTime() {
		long selfishHarvesterWins = 0;

		// 5%
		selfishHarvesterWins += this.normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 5, 10, 500_000_000L);

		// 10%
		selfishHarvesterWins += this.normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 10, 10, 500_000_000L);

		// 20%
		selfishHarvesterWins += this.normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 20, 10, 500_000_000L);

		// 40%
		selfishHarvesterWins += this.normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 40, 10, 500_000_000L);

		// 45%
		selfishHarvesterWins += this.normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Time_Matters, 10, 100 * 60, 45, 10, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish harvester vs multiple normal (random): created better chain!", selfishHarvesterWins == 0);
	}

	@Test
	public void selfishHarvesterVersusManyRandomBetterScore() {
		final long selfishHarvesterWins = 0;

		// 5%
		//		selfishHarvesterWins += normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100*60, 5, 10, 500_000_000L);
		//
		//		// 10%
		//		selfishHarvesterWins += normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100*60, 10, 10, 500_000_000L);
		//
		//		// 20%
		//		selfishHarvesterWins += normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100*60, 20, 10, 500_000_000L);
		//
		//		// 40%
		//		selfishHarvesterWins += normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100*60, 40, 10, 500_000_000L);
		//
		//		// 45%
		//		//selfishHarvesterWins += normalXRandomHarvesterVersusSelfishHarvester(GenerateStrategy.Score_Matters, 10, 100*60, 45, 10, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish harvester vs multiple normal (random): created better chain!", selfishHarvesterWins == 0);
	}

	@Test
	public void differentChainsProduceDifferentScores() {
		// Arrange:
		final AccountWithInfo harvesterA = new AccountWithInfo(1_000_000_000);
		final AccountWithInfo harvesterB = new AccountWithInfo(100_000_000);

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final byte[] rndBytes = new byte[32];

		final Block firstBlock = new Block(harvesterA, new Hash(rndBytes), new Hash(HASH_BYTES), new TimeInstant(1), new BlockHeight(1));
		harvesterA.getInfo().incrementHarvestedBlocks();
		Block block;

		// Act:
		blocks.clear();
		blocks.add(firstBlock);
		for (int i = 0; i < 2; ++i) {
			block = this.generateNextBlock(harvesterA, blocks, scorer, false);
			harvesterA.getInfo().incrementHarvestedBlocks();
			blocks.add(block);
		}
		final long scoreA = this.calculateScore(blocks, scorer);

		blocks.clear();
		blocks.add(firstBlock);
		for (int i = 0; i < 2; ++i) {
			block = this.generateNextBlock(harvesterB, blocks, scorer, true);
			harvesterB.getInfo().incrementHarvestedBlocks();
			blocks.add(block);
		}
		final long scoreB = this.calculateScore(blocks, scorer);

		LOGGER.info("a: " + scoreA + " b: " + scoreB);
		Assert.assertThat(scoreA, IsNot.not(IsEqual.equalTo(scoreB)));
	}

	public int normalHarvesterVersusSelfishHarvester(final int numRounds, final int maxTime, final long normalHarvesterBalance, final long selfishHarvesterBalance) {
		// Arrange:
		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalHarvesterWins = 0;
		int selfishHarvesterWins = 0;
		long normalHarvesterScore;
		long selfishHarvesterScore;

		// Act: normal harvester vs. selfish harvester
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of harvested blocks
			final AccountWithInfo normalHarvester = new AccountWithInfo(normalHarvesterBalance);
			final AccountWithInfo selfishHarvester = new AccountWithInfo(selfishHarvesterBalance);

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(normalHarvester, new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(normalHarvester, blocks, scorer, false);
				blocks.add(block);
				normalHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalHarvesterScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvesterScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvesterWins++;
			}
			//LOGGER.info("score " + selfishHarvesterScore + " vs " + normalHarvesterScore);
		}

		final long selfishHarvesterBalancePercent = (selfishHarvesterBalance * 100L) / (normalHarvesterBalance + selfishHarvesterBalance);
		final int selfishHarvesterWinsPercent = (selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvesterWins);
		LOGGER.info("selfish harvester (" + selfishHarvesterBalancePercent + "% of all nem) wins in:   " + selfishHarvesterWinsPercent + "%.");
		return selfishHarvesterWins;
	}

	public int normalHarvestersOldVersusSelfishNew(final GenerateStrategy strategy, final int numRounds, final int maxTime, final long harvestedBlocksPerNormalHarvester, final int count, final long normalHarvesterBalance, final int selfishCount, final long selfishHarvesterBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalHarvestersWins = 0;
		int selfishHarvesterWins = 0;
		long normalHarvestersScore;
		long selfishHarvesterScore;

		// Act: normal harvester duo vs. selfish harvester
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of harvested blocks
			final AccountWithInfo[] selfishHarvesterAccounts = new AccountWithInfo[selfishCount];
			final AccountWithInfo[] harvesters = new AccountWithInfo[count];
			for (int j = 0; j < count; j++) {
				harvesters[j] = new AccountWithInfo(normalHarvesterBalance);
				for (int k = 0; k < harvestedBlocksPerNormalHarvester; k++) {
					harvesters[j].getInfo().incrementHarvestedBlocks();
				}
			}
			for (int j = 0; j < selfishCount; ++j) {
				selfishHarvesterAccounts[j] = new AccountWithInfo(selfishHarvesterBalance);
			}

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(harvesters[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, selfishHarvesterAccounts, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
			//LOGGER.info("score " + selfishHarvesterScore + " vs " + normalHarvestersScore + " " + ((selfishHarvesterScore>normalHarvestersScore)?"*":" "));
		}

		LOGGER.info(selfishCount + " selfish harvester vs " + count + " (" +
				(selfishCount * selfishHarvesterBalance * 100L) / (count * normalHarvesterBalance + selfishCount * selfishHarvesterBalance) +
				"% of all nem) wins in:   " + (selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	public int normalXHarvesterVersusSelfishHarvester(final GenerateStrategy strategy, final int numRounds, final int maxTime, final int count, final long normalHarvesterBalance, final long selfishHarvesterBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalHarvestersWins = 0;
		int selfishHarvesterWins = 0;
		long normalHarvestersScore;
		long selfishHarvesterScore;

		// Act: normal harvester duo vs. selfish harvester
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of harvested blocks
			final AccountWithInfo selfishHarvester = new AccountWithInfo(selfishHarvesterBalance);
			final AccountWithInfo[] harvesters = new AccountWithInfo[count];
			for (int j = 0; j < count; j++) {
				harvesters[j] = new AccountWithInfo(normalHarvesterBalance / count);
			}

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(harvesters[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
			//LOGGER.info("score " + selfishHarvesterScore + " vs " + normalHarvestersScore + " " + ((selfishHarvesterScore>normalHarvestersScore)?"*":" "));
		}

		LOGGER.info((strategy == GenerateStrategy.Time_Matters ? "(time)" : "(score)") + " selfish harvester vs " + count + " (" +
				(selfishHarvesterBalance * 100L) / (count * normalHarvesterBalance + selfishHarvesterBalance) + "% of all nem) wins in:   " +
				(selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	public int normalXRandomHarvesterVersusSelfishHarvester(final GenerateStrategy strategy, final int numRounds, final int maxTime, final int percentage, final int count, final long normalHarvesterBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		Block firstBlock;
		Block lastBlock;
		int normalHarvestersWins = 0;
		int selfishHarvesterWins = 0;
		long normalHarvestersScore;
		long selfishHarvesterScore;

		// Act: normal harvester duo vs. selfish harvester
		for (int i = 0; i < numRounds; i++) {
			// create here to reset number of harvested blocks
			final AccountWithInfo[] harvesters = new AccountWithInfo[count];
			long selfishHarvesterBalance = 0L;
			for (int j = 0; j < count; j++) {
				harvesters[j] = new AccountWithInfo(Math.abs(sr.nextLong() % normalHarvesterBalance));
				selfishHarvesterBalance += harvesters[j].getInfo().getBalance().getNumNem();
			}
			final AccountWithInfo selfishHarvester = new AccountWithInfo(selfishHarvesterBalance * percentage * 2 / 100);

			sr.nextBytes(rndBytes);
			firstBlock = this.createFirstBlock(harvesters[i % count], new Hash(rndBytes));

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(strategy, harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("NORMAL==== ==== ==== ==== ");
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			//LOGGER.info("SELFISH=== ==== ==== ==== ");
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
			//LOGGER.info("score " + selfishHarvesterScore + " vs " + normalHarvestersScore + " " + ((selfishHarvesterScore>normalHarvestersScore)?"*":" "));
		}

		LOGGER.info("selfish harvester (" + percentage + "% of all nems) vs x random wins in:   " +
				(selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	public long oneHarvestersVersusManyHarvesters(final int maxTime, final int count, final long manyHarvestersBalance) {
		// Arrange:

		final BlockScorer scorer = createBlockScorer();
		final List<Block> blocks = new LinkedList<>();
		final SecureRandom sr = new SecureRandom();
		final byte[] rndBytes = new byte[32];
		final Block firstBlock;
		Block lastBlock;

		// Act: one harvester vs. count harvesters
		final AccountWithInfo[] harvesters = new AccountWithInfo[count + 1];
		for (int j = 0; j < count; j++) {
			harvesters[j] = new AccountWithInfo(manyHarvestersBalance / count);
		}
		harvesters[count] = new AccountWithInfo(manyHarvestersBalance);

		sr.nextBytes(rndBytes);
		firstBlock = this.createFirstBlock(harvesters[0], new Hash(rndBytes));

		blocks.clear();
		blocks.add(firstBlock);
		do {
			final Block block = this.generateNextBlockMultiple(GenerateStrategy.Time_Matters, harvesters, blocks, scorer, false);
			blocks.add(block);
			lastBlock = block;
		} while (lastBlock.getTimeStamp().getRawTime() < maxTime);

		long manyHarvestersNumBlocks = 0;
		for (int j = 0; j < count; j++) {
			manyHarvestersNumBlocks += harvesters[j].getInfo().getHarvestedBlocks().getRaw();
		}

		final long numHarvestedBlocks = harvesters[count].getInfo().getHarvestedBlocks().getRaw();
		final long percentage = (numHarvestedBlocks * 100) / (numHarvestedBlocks + manyHarvestersNumBlocks);
		LOGGER.info("One harvester created  " + numHarvestedBlocks + " blocks (" + percentage + "%), " + count + " harvesters created " +
				manyHarvestersNumBlocks + " blocks.");
		return percentage;
	}

	private Block createFirstBlock(final Account account, final Hash previousHash) {
		final Hash generationHash = HashUtils.nextHash(previousHash, account.getAddress().getPublicKey());
		return new Block(account, previousHash, generationHash, new TimeInstant(1), new BlockHeight(1));
	}

	private List<BlockDifficulty> createDifficultiesList(final List<Block> blocks) {
		return blocks.stream().map(Block::getDifficulty).collect(Collectors.toList());
	}

	private List<TimeInstant> createTimestampsList(final List<Block> blocks) {
		return blocks.stream().map(VerifiableEntity::getTimeStamp).collect(Collectors.toList());
	}

	private Block generateNextBlock(final AccountWithInfo harvester, final List<Block> blocks, final BlockScorer scorer, final boolean randomizeTime) {
		final Block lastBlock = blocks.get(blocks.size() - 1);
		Block block = new Block(harvester, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

		final List<Block> historicalBlocks = blocks.subList(Math.max(0, (blocks.size() - BlockScorer.NUM_BLOCKS_FOR_AVERAGE_CALCULATION)), blocks.size());
		final BlockDifficulty difficulty = scorer.getDifficultyScorer().calculateDifficulty(
				this.createDifficultiesList(historicalBlocks),
				this.createTimestampsList(historicalBlocks),
				block.getHeight().getRaw());
		block.setDifficulty(difficulty);
		final BigInteger hit = scorer.calculateHit(block);
		int seconds = hit.multiply(block.getDifficulty().asBigInteger())
				.divide(BlockScorer.TWO_TO_THE_POWER_OF_64)
				.divide(BigInteger.valueOf(harvester.getInfo().getBalance().getNumNem()))
				.intValue();
		if (seconds == 0) {
			// This will not happen in our network
			seconds = 1;
		}
		if (randomizeTime) {
			seconds += (new SecureRandom()).nextInt(10);
		}

		block = new Block(harvester, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds));
		block.setDifficulty(difficulty);

		return block;
	}

	private Block generateNextBlockMultiple(final GenerateStrategy strategy, final AccountWithInfo[] harvesters, final List<Block> blocks, final BlockScorer scorer, final boolean randomizeTime) {
		final Block lastBlock = blocks.get(blocks.size() - 1);

		Block bestBlock = null;
		long maxSum = Integer.MIN_VALUE;
		int minTime = Integer.MAX_VALUE;
		for (final AccountWithInfo harvester : harvesters) {
			Block block = new Block(harvester, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + 1));

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
					.divide(BigInteger.valueOf(harvester.getInfo().getBalance().getNumNem()))
					.intValue();
			if (seconds == 0) {
				// This will not happen in our network
				seconds = 1;
			}
			if (randomizeTime) {
				seconds += (new SecureRandom()).nextInt(10);
			}

			block = new Block(harvester, lastBlock, new TimeInstant(lastBlock.getTimeStamp().getRawTime() + seconds));
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

		if (null != bestBlock) {
			for (final AccountWithInfo account : harvesters) {
				if (account.getAddress().equals(bestBlock.getSigner().getAddress())) {
					account.getInfo().incrementHarvestedBlocks();
				}
			}
		}

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

	private static BlockScorer createBlockScorer() {
		return new BlockScorer(new DefaultAccountStateCache());
	}

	private static class AccountWithInfo extends Account {
		private final AccountInfo accountInfo;

		public AccountWithInfo(final long balance) {
			super(new KeyPair());
			this.accountInfo = new AccountInfo();
			this.accountInfo.incrementBalance(Amount.fromNem(balance));
		}

		public final AccountInfo getInfo() {
			return this.accountInfo;
		}
	}
}
