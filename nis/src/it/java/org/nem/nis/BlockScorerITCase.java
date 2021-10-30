package org.nem.nis;

import org.hamcrest.MatcherAssert;
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

	@After
	public void resetBlockChainConfiguration() {
		NemGlobals.setBlockChainConfiguration(null);
	}

	private static void setupCustomBlockChainConfiguration() {
		final BlockChainConfigurationBuilder builder = new BlockChainConfigurationBuilder()
				.setBlockGenerationTargetTime(15)
				.setBlockChainRewriteLimit(360)
				.setBlockChainFeatures(new BlockChainFeature[] { BlockChainFeature.PROOF_OF_STAKE, BlockChainFeature.STABILIZE_BLOCK_TIMES });
		NemGlobals.setBlockChainConfiguration(builder.build());
	}

	@Test
	public void timeBetweenBlocksIsAboutThirtySecondsWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.assertTimeBetweenBlocks(15);
	}

	@Test
	public void timeBetweenBlocksIsAboutSixtySeconds() {
		this.assertTimeBetweenBlocks(60);
	}

	private void assertTimeBetweenBlocks(final int targetTime) {
		// Only 500M nem (going below this limit will result in higher block creation times)
		this.harvesterTest(10000, 500_000_000L, 10, targetTime);

		// 2B nem
		this.harvesterTest(10000, 2_000_000_000L, 10, targetTime);

		// Full 9B nem
		this.harvesterTest(10000, 9_000_000_000L, 10, targetTime);
	}

	private void harvesterTest(final int numRounds, final long numNEM, final int numHarvesters, final int targetTime) {
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
		final int[] slots = new int[1000];
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
					this.createTimestampsList(historicalBlocks)));
			secondsBetweenBlocks[i] = Integer.MAX_VALUE;
			for (int j = 0; j < numHarvesters; j++) {
				final Block temporaryDummy = new Block(harvesterAccounts[j], blocks[i - 1], new TimeInstant(1));
				final BigInteger hit = scorer.calculateHit(temporaryDummy);
				final int seconds = getTimeForNextBlock(
						hit,
						block.getDifficulty().asBigInteger(),
						BigInteger.valueOf(harvesterAccounts[j].getInfo().getBalance().getNumNem()));
				if (seconds < secondsBetweenBlocks[i]) {
					secondsBetweenBlocks[i] = seconds;
					index = j;
				}
			}
			if (secondsBetweenBlocks[i] == 0) {
				// This will not happen in our network
				secondsBetweenBlocks[i] = 1;
			}

			final int slot = secondsBetweenBlocks[i] >= 1000 ? 999 : secondsBetweenBlocks[i];
			slots[slot]++;
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
		Assert.assertTrue("Average time between blocks not within reasonable range!", targetTime * 0.9 < averageTime && averageTime < targetTime * 1.1);
	}

	private static int getTimeForNextBlock(final BigInteger hit, final BigInteger difficulty, final BigInteger balance) {
		int lowerBound = 0;
		int upperBound = 1000;
		BigInteger target = getTarget(upperBound, difficulty, balance);
		if (target.compareTo(hit) <= 0) {
			return upperBound;
		}

		while (upperBound - lowerBound > 1) {
			final int middle = (upperBound + lowerBound) / 2;
			target = getTarget(middle, difficulty, balance);
			if (target.compareTo(hit) <= 0) {
				lowerBound = middle;
			} else {
				upperBound = middle;
			}
		}

		return upperBound;
	}

	private static BigInteger getTarget(final int timeDiff, final BigInteger difficulty, final BigInteger balance) {
		final BigInteger multiplier = getMultiplierAt(timeDiff);
		return BigInteger.valueOf(timeDiff)
				.multiply(balance)
				.multiply(multiplier)
				.divide(difficulty);
	}

	private static BigInteger getMultiplierAt(final int timeDiff) {
		final BlockChainConfiguration configuration = NemGlobals.getBlockChainConfiguration();
		final double targetTime = (double)configuration.getBlockGenerationTargetTime();
		final double tmp = configuration.isBlockChainFeatureSupported(BlockChainFeature.STABILIZE_BLOCK_TIMES)
				? Math.min(Math.exp(6.0 * (timeDiff - targetTime) / targetTime), 100.0)
				: 1.0;
		return BigInteger.valueOf((long)(BlockScorer.TWO_TO_THE_POWER_OF_54 * tmp)).shiftLeft(10);
	}

	@Test
	public void oneHarvesterIsNotBetterThanManyHarvestersWithSameCumulativeBalanceWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.oneHarvesterIsNotBetterThanManyHarvestersWithSameCumulativeBalance();
	}

	@Test
	public void oneHarvesterIsNotBetterThanManyHarvestersWithSameCumulativeBalance() {
		final long OneHarvesterPercentageBlocks = this.oneHarvestersVersusManyHarvesters(2000 * 60, 10, 1_000_000_000L);
		Assert.assertTrue("One harvests creates too many/not enough blocks compared to many harvesters!",
				45 < OneHarvesterPercentageBlocks && OneHarvesterPercentageBlocks < 55);
	}

	@Test
	public void selfishHarvesterCannotHarvestBetterChainWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.selfishHarvesterCannotHarvestBetterChain();
	}

	@Test
	public void selfishHarvesterCannotHarvestBetterChain() {
		final int[] selfishHarvesterWins = new int[7];
		final int numRounds = 20;
		final int numBlocks = 100;
		final int timeInterval = numBlocks * 60;
		//  1% attack: 10 rounds with approximately 100 blocks each
		// NOTE: Since the attacker has a balance considerably lower than 200 million,
		//       his the average time between blocks is considerably higher than 60 seconds!
		//       That's why the normal harvester wins this time.
		selfishHarvesterWins[0] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 3_960_000_000L, 40_000_000L);

		//  5% attack: numRounds rounds with approximately numBlocks blocks each
		selfishHarvesterWins[1] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 3_800_000_000L, 200_000_000L);

		//  10% attack: numRounds rounds with approximately numBlocks blocks each
		selfishHarvesterWins[2] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 2_700_000_000L, 300_000_000L);

		//  20% attack: numRounds rounds with approximately numBlocks blocks each
		selfishHarvesterWins[3] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 2_000_000_000L, 500_000_000L);

		//  30% attack: numRounds rounds with approximately numBlocks blocks each
		selfishHarvesterWins[4] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 1_400_000_000L, 600_000_000L);

		//  40% attack: numRounds rounds with approximately numBlocks blocks each
		selfishHarvesterWins[5] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 1_500_000_000L, 1_000_000_000L);

		//  45% attack: numRounds rounds with approximately numBlocks blocks each
		//  Due to variance the selfish harvester sometimes wins
		selfishHarvesterWins[6] = this.normalHarvesterVersusSelfishHarvester(numRounds, timeInterval, 1_100_000_000L, 900_000_000L);

		// Assert:
		final float[] thresholdWins = new float[] { 0, 0, 0, 0, 0, 0, 0.01f };
		for (int i = 0; i < selfishHarvesterWins.length; ++i) {
			final int maxWins = (int)Math.floor(numRounds * thresholdWins[i]);
			final int actualWins = selfishHarvesterWins[i];
			final String message = String.format(
					"Selfish harvester(%d) created better chain! (actual: %d, max: %d)",
					i,
					actualWins,
					maxWins);
			MatcherAssert.assertThat(message, actualWins <= maxWins, IsEqual.equalTo(true));
		}
	}

	@Test
	public void selfishHarvesterVersusMultipleNormalWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.selfishHarvesterVersusMultipleNormal();
	}

	@Test
	public void selfishHarvesterVersusMultipleNormal() {
		// Act
		long selfishHarvesterWins = 0;

		selfishHarvesterWins += this.normalXHarvesterVersusSelfishHarvester(10, 100 * 60, 10, 1_000_000_000L, 500_000_000L);

		// Assert:
		Assert.assertTrue("Selfish harvester vs multiple normal: created better chain!", selfishHarvesterWins == 0);
	}

	@Test
	public void manyOldNormalHarvestersVersusManyFreshSelfishHarvestersTimeWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.manyOldNormalHarvestersVersusManyFreshSelfishHarvestersTime();
	}

	@Test
	public void manyOldNormalHarvestersVersusManyFreshSelfishHarvestersTime() {
		// The selfish harvester waits till the normal harvesters have harvested harvestedBlocksPerNormalHarvester blocks in average.
		// (Assuming 5000 normal harvesters in a real NEM network each harvester has harvested about 100 blocks after a year.)
		// Then the selfish harvester creates brand-new accounts and harvests with them.
		long selfishHarvesterWins = 0;

		selfishHarvesterWins += this.normalHarvestersOldVersusSelfishNew(10, 50 * 60, 100, 10, 1_000_000_000L, 1, 1_000_000_000);

		// Assert
		Assert.assertTrue("(multiple) Selfish harvester vs vs multiple normal: created better chain!", selfishHarvesterWins == 0);
	}

	/**
	 * I have been doing more extensive tests to see if there is something wrong but i tend to say it's just variance.
	 * Even when both have 50% coins you can see the outcome can be quite different.
	 * In each run the competitors played 50 rounds. About 1.2 million blocks were harvested per run.
	 * So both parties were harvesting very long chains before comparing the results (numBlocks = 10000).
	 * That is why the win percentage for the selfish harvester is a lot lower than in the usual tests where we have numBlocks = 100.
	 * <br>
	 * Selfish harvester with 40%:
	 * ---------------------------
	 * selfish harvester (535677 blocks) vs x random (614974 blocks) : 0% : 100%
	 * selfish harvester (531433 blocks) vs x random (615514 blocks) : 0% : 100%
	 * selfish harvester (535330 blocks) vs x random (620317 blocks) : 0% : 100%
	 * selfish harvester (544165 blocks) vs x random (634019 blocks) : 0% : 100%
	 * selfish harvester (548905 blocks) vs x random (646912 blocks) : 0% : 100%
	 * <br>
	 * Selfish harvester with 45%:
	 * ---------------------------
	 * selfish harvester (607568 blocks) vs x random (662459 blocks) : 0% : 100%
	 * selfish harvester (578060 blocks) vs x random (626793 blocks) : 2% : 98%
	 * selfish harvester (575709 blocks) vs x random (623305 blocks) : 0% : 100%
	 * selfish harvester (583795 blocks) vs x random (634828 blocks) : 2% : 98%
	 * selfish harvester (582006 blocks) vs x random (633891 blocks) : 0% : 100%
	 * <br>
	 * Selfish harvester with 50%:
	 * ---------------------------
	 * selfish harvester (626171 blocks) vs x random (625783 blocks) : 46% : 54%
	 * selfish harvester (625568 blocks) vs x random (625604 blocks) : 48% : 52%
	 * selfish harvester (619833 blocks) vs x random (618509 blocks) : 60% : 40%
	 * selfish harvester (632255 blocks) vs x random (633888 blocks) : 38% : 62%
	 * selfish harvester (648501 blocks) vs x random (648783 blocks) : 42% : 58%
	 * selfish harvester (639336 blocks) vs x random (637563 blocks) : 60% : 40%
	 * selfish harvester (660047 blocks) vs x random (661422 blocks) : 46% : 54%
	 * selfish harvester (678180 blocks) vs x random (676547 blocks) : 56% : 44%
	 * selfish harvester (640489 blocks) vs x random (640088 blocks) : 46% : 54%
	 * selfish harvester (617941 blocks) vs x random (618577 blocks) : 52% : 48%
	 */

	@Test
	public void selfishHarvesterVersusManyRandomBetterScoreWithCustomBlockChainConfiguration() {
		setupCustomBlockChainConfiguration();
		this.selfishHarvesterVersusManyRandomBetterScore();
	}

	@Test
	public void selfishHarvesterVersusManyRandomBetterScore() {
		final int numRounds = 25;
		final int numBlocks = 100;
		final int timeInterval = numBlocks * 60;
		final int[] selfishHarvesterWins = new int[5];

		// 5%
		selfishHarvesterWins[0] = this.normalXRandomHarvesterVersusSelfishHarvester(numRounds, timeInterval, 5, 10, 2_000_000_000L);

		// 10%
		selfishHarvesterWins[1] = this.normalXRandomHarvesterVersusSelfishHarvester(numRounds, timeInterval, 10, 10, 2_000_000_000L);

		// 20%
		selfishHarvesterWins[2] = this.normalXRandomHarvesterVersusSelfishHarvester(numRounds, timeInterval, 20, 10, 2_000_000_000L);

		// 40%
		selfishHarvesterWins[3] = this.normalXRandomHarvesterVersusSelfishHarvester(numRounds, timeInterval, 40, 10, 2_000_000_000L);

		// 45%
		selfishHarvesterWins[4] = this.normalXRandomHarvesterVersusSelfishHarvester(numRounds, timeInterval, 45, 10, 2_000_000_000L);

		// Assert:
		final float[] thresholdWins = new float[] { 0, 0, 0, 0.10f, 0.25f };
		for (int i = 0; i < selfishHarvesterWins.length; ++i) {
			final int maxWins = (int)Math.floor(numRounds * thresholdWins[i]);
			final int actualWins = selfishHarvesterWins[i];
			final String message = String.format(
					"Selfish harvester(%d) vs multiple normal (random): created better chain! (actual: %d, max: %d)",
					i,
					actualWins,
					maxWins);
			MatcherAssert.assertThat(message, actualWins <= maxWins, IsEqual.equalTo(true));
		}
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
		MatcherAssert.assertThat(scoreA, IsNot.not(IsEqual.equalTo(scoreB)));
	}

	private int normalHarvesterVersusSelfishHarvester(final int numRounds, final int maxTime, final long normalHarvesterBalance, final long selfishHarvesterBalance) {
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
			normalHarvesterScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvesterScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvesterWins++;
			}
		}

		final long selfishHarvesterBalancePercent = (selfishHarvesterBalance * 100L) / (normalHarvesterBalance + selfishHarvesterBalance);
		final int selfishHarvesterWinsPercent = (selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvesterWins);
		LOGGER.info("selfish harvester (" + selfishHarvesterBalancePercent + "% of all nem) wins in:   " + selfishHarvesterWinsPercent + "%.");
		return selfishHarvesterWins;
	}

	private int normalHarvestersOldVersusSelfishNew(final int numRounds, final int maxTime, final long harvestedBlocksPerNormalHarvester, final int count, final long normalHarvesterBalance, final int selfishCount, final long selfishHarvesterBalance) {
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
				final Block block = this.generateNextBlockMultiple(harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlockMultiple(selfishHarvesterAccounts, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
		}

		LOGGER.info(selfishCount + " selfish harvester vs " + count + " (" +
				(selfishCount * selfishHarvesterBalance * 100L) / (count * normalHarvesterBalance + selfishCount * selfishHarvesterBalance) +
				"% of all nem) wins in:   " + (selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	private int normalXHarvesterVersusSelfishHarvester(final int numRounds, final int maxTime, final int count, final long normalHarvesterBalance, final long selfishHarvesterBalance) {
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
				final Block block = this.generateNextBlockMultiple(harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
		}

		LOGGER.info("selfish harvester vs " + count + " (" +
				(selfishHarvesterBalance * 100L) / (count * normalHarvesterBalance + selfishHarvesterBalance) + "% of all nem) wins in:   " +
				(selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	private int normalXRandomHarvesterVersusSelfishHarvester(
			final int numRounds,
			final int maxTime,
			final int percentage,
			final int count,
			final long normalHarvesterBalance) {
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
				final Block block = this.generateNextBlockMultiple(harvesters, blocks, scorer, false);
				blocks.add(block);
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			normalHarvestersScore = this.calculateScore(blocks, scorer);

			blocks.clear();
			blocks.add(firstBlock);
			do {
				final Block block = this.generateNextBlock(selfishHarvester, blocks, scorer, false);
				blocks.add(block);
				selfishHarvester.getInfo().incrementHarvestedBlocks();
				lastBlock = block;
			} while (lastBlock.getTimeStamp().getRawTime() < maxTime);
			selfishHarvesterScore = this.calculateScore(blocks, scorer);
			if (selfishHarvesterScore > normalHarvestersScore) {
				selfishHarvesterWins++;
			} else {
				normalHarvestersWins++;
			}
		}

		LOGGER.info("selfish harvester (" + percentage + "% of all nems) vs x random wins in:   " +
				(selfishHarvesterWins * 100) / (selfishHarvesterWins + normalHarvestersWins) + "%.");
		return selfishHarvesterWins;
	}

	private long oneHarvestersVersusManyHarvesters(final int maxTime, final int count, final long manyHarvestersBalance) {
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
			final Block block = this.generateNextBlockMultiple(harvesters, blocks, scorer, false);
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
				this.createTimestampsList(historicalBlocks));
		block.setDifficulty(difficulty);
		final BigInteger hit = scorer.calculateHit(block);
		int seconds = getTimeForNextBlock(
				hit,
				block.getDifficulty().asBigInteger(),
				BigInteger.valueOf(harvester.getInfo().getBalance().getNumNem()));
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

	private Block generateNextBlockMultiple(
			final AccountWithInfo[] harvesters,
			final List<Block> blocks,
			final BlockScorer scorer,
			final boolean randomizeTime) {
		final Block lastBlock = blocks.get(blocks.size() - 1);

		Block bestBlock = null;
		AccountWithInfo bestBlockHarvester = null;
		long maxScore = Integer.MIN_VALUE;
		for (final AccountWithInfo harvester : harvesters) {
			final Block block = this.generateNextBlock(harvester, blocks, scorer, randomizeTime);

			final long score = scorer.calculateBlockScore(lastBlock, block);
			if (score > maxScore) {
				bestBlock = block;
				bestBlockHarvester = harvester;
				maxScore = score;
			}
		}

		if (null != bestBlock) {
			bestBlockHarvester.getInfo().incrementHarvestedBlocks();
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
