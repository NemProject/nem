package org.nem.nis.poi;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.poi.graph.*;
import org.nem.nis.secret.AccountLink;
import org.nem.nis.test.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PoiImportanceCalculatorTest {
	private static final Logger LOGGER = Logger.getLogger(PoiImportanceCalculatorTest.class.getName());
	private static final PoiOptions DEFAULT_OPTIONS = new PoiOptionsBuilder().create();
	private static ImportanceScorer DEFAULT_IMPORTANCE_SCORER = new PoiScorer();

	@Test
	public void fastScanClusteringResultsInSameImportancesAsScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new ScanClusteringStrategy());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance < 0.00001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanOutlierScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new OutlierScan());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanSingleClusterScan() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(new SingleClusterScan());

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringResultsInDifferentImportancesThanNoClustering() {
		// Act:
		final double distance = calculateDistanceBetweenFastScanAndOtherImportances(null);

		// Assert:
		Assert.assertTrue(String.format("distance %f should be greater than threshold", distance), distance > 0.0001);
	}

	@Test
	public void fastScanClusteringCorrectlyIdentifiesMostImportantAccount() {
		// Act:
		final ColumnVector importances = calculateImportances(new FastScanClusteringStrategy());
		int maxIndex = 0;
		int nextMaxIndex = 0;
		for (int i = 0; i < importances.size(); ++i) {
			if (importances.getAt(i) > importances.getAt(maxIndex)) {
				nextMaxIndex = maxIndex;
				maxIndex = i;
			} else if (importances.getAt(i) > importances.getAt(nextMaxIndex)) {
				nextMaxIndex = i;
			}
		}

		// Assert: max index and next max index are 4 and 5
		Assert.assertThat(maxIndex + nextMaxIndex, IsEqual.equalTo(9));
		Assert.assertThat(Math.abs(maxIndex - nextMaxIndex), IsEqual.equalTo(1));
	}

	private static double calculateDistanceBetweenFastScanAndOtherImportances(final GraphClusteringStrategy clusteringStrategy) {
		// Act:
		final ColumnVector fastScanImportances = calculateImportances(new FastScanClusteringStrategy());
		final ColumnVector otherImportances = calculateImportances(clusteringStrategy);

		// Assert:
		final double distance = fastScanImportances.l2Distance(otherImportances);
		LOGGER.info(String.format("fastScanImportances - %s", fastScanImportances));
		LOGGER.info(String.format("otherImportances ---- %s", otherImportances));
		LOGGER.info(String.format("distance - %f", distance));
		return distance;
	}

	private static ColumnVector calculateImportances(final GraphClusteringStrategy clusteringStrategy) {
		final Collection<PoiAccountState> accountStates = createAccountStatesFromGraph(GraphType.GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		final PoiOptionsBuilder poiOptionsBuilder = new PoiOptionsBuilder();
		poiOptionsBuilder.setClusteringStrategy(clusteringStrategy);
		if (null == clusteringStrategy) {
			final PoiOptions defaultOptions = poiOptionsBuilder.create();
			final double totalTeleportationProbability = defaultOptions.getTeleportationProbability() + defaultOptions.getInterLevelTeleportationProbability();
			poiOptionsBuilder.setTeleportationProbability(totalTeleportationProbability);
			poiOptionsBuilder.setInterLevelTeleportationProbability(0.00);
		}

		return calculateImportances(poiOptionsBuilder.create(), new BlockHeight(2), accountStates);
	}

	//endregion

	//region prototypical graphs

	/**
	 * <pre>
	 *     1 \       / 6
	 *     2 -\     /- 7
	 *     3 --o 0 o-- 8
	 *     4 -/     \- 9
	 *     5 /       \ 10
	 * </pre>
	 */
	@Test
	public void hubSinkSpokeGraphGivesHigherImportanceToHubThanSpokes() {
		// Arrange:
		// - account 0 starts with 2000 NEM
		// - accounts 1-10 start with 2100 NEM
		// - accounts 1-10 send 100 NEM to 0
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		for (int i = 1; i <= 10; ++i) {
			outlinkMatrix.setAt(0, i, 100);

			// initialize all hubs with 2100 NEM so that after the transfers all accounts will have the same balance
			accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100)));
		}

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are 2000 (the sent NEM have not vested)
		assertEqualBalances(height, accountStates, 2000);

		// - all spokes have the same importance
		final double hubImportance = importances.getAt(0);
		final double spokeImportance = importances.getAt(1);
		for (int i = 2; i < importances.size(); ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(spokeImportance));
		}

		// - the hub importance is greater than the spoke importance
		final double ratio = hubImportance / spokeImportance;
		LOGGER.info(String.format("hub: %f; spoke %f; ratio: %f", hubImportance, spokeImportance, ratio));
		Assert.assertThat(ratio > 1.20, IsEqual.equalTo(true));
		Assert.assertThat(ratio < 1.60, IsEqual.equalTo(true));
	}

	/**
	 * <pre>
	 *     1 o\       /o 6
	 *     2 o-\     /-o 7
	 *     3 o--- 0 ---o 8
	 *     4 o-/     \-o 9
	 *     5 o/       \o 10
	 * </pre>
	 */
	@Test
	public void hubSpokeSinkGraphGivesHigherImportanceHubThanToSpokes() {
		// Arrange:
		// - account 0 starts with 3000 NEM
		// - accounts 1-10 start with 2000 NEM
		// - account 0 sends 100 NEM to accounts 0-10
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(3000)));

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		for (int i = 1; i <= 10; ++i) {
			outlinkMatrix.setAt(i, 0, 100);

			// initialize all hubs with 2000 NEM so that after the transfers all accounts will have the same balance
			accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));
		}

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are 2000 (the sent NEM have not vested)
		assertEqualBalances(height, accountStates, 2000);

		// - all spokes have the same importance
		final double hubImportance = importances.getAt(0);
		final double spokeImportance = importances.getAt(1);
		for (int i = 2; i < importances.size(); ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(spokeImportance));
		}

		// - the hub importance is greater than the spoke importance
		final double ratio = hubImportance / spokeImportance;
		LOGGER.info(String.format("hub: %f; spoke %f; ratio: %f", hubImportance, spokeImportance, ratio));
		Assert.assertThat(ratio > 1.20, IsEqual.equalTo(true));
		Assert.assertThat(ratio < 1.60, IsEqual.equalTo(true));
	}

	/**
	 * TODO 20141013 BR -> J: what we are really interested in is the importance calculated by the page rank part. Here are the (unweighted) numbers:
	 * TODO                   In your scenario 0, 1 and 6 are outliers, 2, 3, 4, 5 and 7, 8, 9, 10 build a cluster each.
	 * 0.117 0.103 0.069 0.081 0.091 0.098 0.103 0.069 0.081 0.091 0.098 original importance
	 * I must say I don't really understand why the importances rise from node 2 onwards: 2 < 3 < 4 < 5 < 1
	 * 1 should transfer more importance to 2 than 4 transfers to 5.
	 *
	 * Variation of the teleportation probabilities shows this behavior (TP = teleportation prob., ITLP = inter level teleportation prob.):
	 *  TP   ILTP
	 * 0.8  | 0.1  | 0.120 0.104 0.068 0.080 0.090 0.098 0.104 0.068 0.080 0.090 0.098
	 * 0.6  | 0.3  | 0.131 0.097 0.070 0.083 0.090 0.094 0.097 0.070 0.083 0.090 0.094
	 * 0.45 | 0.45 | 0.144 0.090 0.073 0.084 0.089 0.091 0.090 0.073 0.084 0.089 0.091
	 * 0.3  | 0.6  | 0.163 0.084 0.075 0.085 0.087 0.088 0.084 0.075 0.085 0.087 0.088
	 * 0.1  | 0.8  | 0.201 0.075 0.078 0.082 0.082 0.082 0.075 0.078 0.082 0.082 0.082
	 * 0 gets more and more important and the nodes within a cluster get more and more equal.
	 *
	 * Here are the values for SingleClusterScan (should be the same as normal page rank):
	 * 0.108 0.104 0.069 0.082 0.092 0.099 0.104 0.069 0.082 0.092 0.099 original importance
	 * The ncd-aware algorithm pushes 0 which is good. Again I don't understand why 2 has such a low importance.
	 */
	/**
	 * <pre>
	 *     4----o5----o1 --o0o-- 6----o7----o8
	 *     o           |         o           |
	 *     |           o         |           o
	 *     3o----------2        10o----------9
	 * </pre>
	 */
	@Test
	public void fiveMemberRingConnectedThroughSpoke() {
		// Arrange:
		// - account 0 starts with 2000 NEM
		// - accounts 2-5 and 7-10 start with 2100 NEM
		// - accounts 1 and 6 start with 2200 NEM
		// - account 1-5 and 2-10 send around NEM in a loop
		// - account 1-6 send NEM to 0
		final List<PoiAccountState> accountStates = new ArrayList<>();
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2200)));
		for (int i = 2; i <= 5; ++i) { accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100))); }
		accountStates.add(createAccountStateWithBalance(Amount.fromNem(2200)));
		for (int i = 7; i <= 10; ++i) { accountStates.add(createAccountStateWithBalance(Amount.fromNem(2100))); }

		final Matrix outlinkMatrix = new DenseMatrix(11, 11);
		outlinkMatrix.setAt(0, 1, 100);
		for (int i = 1; i <= 4; ++i) { outlinkMatrix.setAt(i + 1, i, 100); }
		outlinkMatrix.setAt(1, 5, 100);
		outlinkMatrix.setAt(0, 6, 100);
		for (int i = 6; i <= 9; ++i) { outlinkMatrix.setAt(i + 1, i, 100); }
		outlinkMatrix.setAt(6, 10, 100);

		final BlockHeight height = new BlockHeight(2);
		addOutlinksFromGraph(accountStates, height, outlinkMatrix);

		// Act:
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		builder.setTeleportationProbability(0.8);
		builder.setInterLevelTeleportationProbability(0.1);
		final ColumnVector importances = calculateImportances(builder.create(), height, accountStates);
		final ColumnVector balances = getBalances(height, accountStates);

		// Assert:
		// - all balances should be within a small range
		for (int i = 0; i < balances.size(); ++i) {
			final double balance = balances.getAt(i);
			Assert.assertThat(balance >= 2000 && balance < 2005, IsEqual.equalTo(true));
		}

		// - all importances should be within a small range
		for (int i = 0; i < importances.size(); ++i) {
			final double importance = importances.getAt(i);
			Assert.assertThat(importance > 0.085 && importance < 0.10, IsEqual.equalTo(true));
		}

		// - the ring importances should be the same
		for (int i = 1; i <= 5; ++i) {
			Assert.assertThat(importances.getAt(i), IsEqual.equalTo(importances.getAt(i + 5)));
		}
	}

	// region spam scenario

	/**
	 * Given 2 6-rings, the right one got only few edges, the left one got many edges.
	 * Try to find parameters so that the left ring has (ideally) not more importance than the right ring.
	 * (because the transaction within a ring should not matter to the overall importance of a ring.)
	 *
	 * Outcome: Independent of the used algorithm it seems it doesn't matter at all if there are additional transactions.
	 *          This is probably due to the normalization of the columns of the outlink matrix.
	 *
	 * <pre>
	 *           1------o2                 7------o8
	 *         o \     /  \              o          \
	 *       /             o           /             o
	 *     0--  many txs  --3         6               9
	 *      o              /          o              /
	 *       \  /       \ O            \            O
	 *        5o---------4             11o--------10
	 * </pre>
	 */
	@Test
	public void spamLinksDoNotHaveABigImpactOnImportance() {
		// Arrange:
		// - all (12) accounts start with 2000 NEM
		final List<PoiAccountState> accountStates = setupAccountStates(12);
		final StandardContext context = new StandardContext();

		// Construct basic ring connections
		final Matrix outlinkMatrix = setupBasicRingStructure();

		// Add random transactions to the left ring
		// TODO 20141014 J-B: should we attempt to filter out self loops (probably doesn't matter)
		final SecureRandom random = new SecureRandom();
		for (int i = 0; i < 100; ++i) {
			outlinkMatrix.incrementAt(random.nextInt(6), random.nextInt(6), 20);
		}

		addOutlinksFromGraph(accountStates, context.height1, outlinkMatrix);

		// Act:
		// TODO 20141014 J-B: are you intentionally using SingleClusterScan?
		context.builder.setClusteringStrategy(new SingleClusterScan());

		// Normal page rank
		LOGGER.info("normal page rank:");
		final ColumnVector normalImportances = calculateImportances(context.builder.create(), context.height1, accountStates);
		final double ratio1 = ringImportanceSum(normalImportances, 1) / ringImportanceSum(normalImportances, 2);

		// NCD aware page rank
		LOGGER.info("NCD aware page rank:");
		final ColumnVector ncdAwareImportances = calculateImportances(context.builder2.create(), context.height2, accountStates);
		final double ratio2 = ringImportanceSum(ncdAwareImportances, 1) / ringImportanceSum(ncdAwareImportances, 2);

		LOGGER.info(String.format("normal importance ratio ring 1 : ring 2 is " + ratio1));
		LOGGER.info(String.format("ncd aware importance ratio ring 1 : ring 2 is " + ratio2));

		// Ideally the ratio should be within a small range.
		Assert.assertThat(ratio1 > 0.95 && ratio1 < 1.05, IsEqual.equalTo(true));
		Assert.assertThat(ratio2 > 0.95 && ratio2 < 1.05, IsEqual.equalTo(true));
	}

	// endregion

	// region transfer of importance between clusters

	/**
	 * Given 2 6-rings with a link between them, analyze the difference between normal page rank matrix and ILP-matrix.
	 * (TP = teleportation probability, ILTP = inter level teleportation probability, PR = normal page rank)
	 *
	 * a) Connection between ring 1 and ring 2 is as strong as the connections within the ring.
	 *    The ratio for the normal page rank is 2.1398
	 * TP   | ILTP | ratio ring 1 : ring 2
	 * 0.85 | 0.0  |       2.1398
	 * 0.75 | 0.1  |       2.0999
	 * 0.65 | 0.2  |       2.0586
	 * 0.55 | 0.3  |       2.0166
	 * 0.45 | 0.4  |       1.9741
	 * 0.35 | 0.5  |       1.9327
	 * 0.25 | 0.6  |       1.8929
	 * 0.15 | 0.7  |       1.8562
	 * 0.05 | 0.8  |       1.8245
	 *
	 * Outcome:
	 * With our standard parameters (0.75/0.1) the ncd aware algorithm tends to transfer less importance from one cluster to another than normal page rank.
	 * The difference is not that huge though.
	 *
	 * b) Connection between ring 1 and ring 2 is a factor 100 weaker than the connections within the ring.
	 *    The ratio for the normal page rank is 1.01841
	 * TP   | ILTP | ratio ring 1 : ring 2
	 * 0.85 | 0.0  |       1.0184
	 * 0.75 | 0.1  |       1.1267
	 * 0.65 | 0.2  |       1.2331
	 * 0.55 | 0.3  |       1.3360
	 * 0.45 | 0.4  |       1.4346
	 * 0.35 | 0.5  |       1.5280
	 * 0.25 | 0.6  |       1.6153
	 * 0.15 | 0.7  |       1.6970
	 * 0.05 | 0.8  |       1.7737
	 *
	 * Outcome:
	 * With our standard parameters (0.75/0.1) the ncd aware algorithm tends to transfer more importance from one cluster to another than normal page rank.
	 * The difference is not that huge though.
	 *
	 * c) Connection between ring 1 and ring 2 is as strong as the connections within the ring.
	 *    ILTP is set to 0.1 for ncd aware page rank and 0.0 for normal page rank. TP is varied.
	 *                  PR                  ncd aware
	 * TP   | ratio ring 1 : ring 2 | ratio ring 1 : ring 2
	 * 1.00 |     7.281 * 10^75     |        ---
	 * 0.99 |        21.854         |        ---
	 * 0.98 |        11.289         |        ---
	 * 0.95 |        4.9520 <------ | ----   ---
	 * 0.90 |        2.8410         |    |   3106.8314
	 * 0.89 |        2.6494         |    |   21.029
	 * 0.88 |        2.4897         |    |   10.9181
	 * 0.87 |        2.3555         |    |   7.5296
	 * 0.86 |        2.2399         |    |   5.8320
	 * 0.85 |        2.1398         |    --- 4.8137
	 * 0.80 |        1.7916         |        2.7770
	 * 0.75 |        1.5839         |        2.0999
	 * 0.70 |        1.4467         |        1.7632
	 * 0.65 |        1.3498         |        1.5631
	 * 0.55 |
	 * 0.45 |
	 * 0.35 |
	 * 0.25 |
	 * 0.15 |
	 * 0.05 |
	 *
	 * Outcome:
	 * Only the sum TP + ILTP plays a role. As it goes near one, more and more importance gets transferred from ring 2 to ring 1.
	 *
	 * <pre>
	 *             ring 1                   ring 2
	 *           1------o2                 7------o8
	 *         o         |\              o |        \
	 *       /           | o           /   |         o
	 *     0             |   3o-------6    |          9
	 *      o            |  /          o   |         /
	 *       \           oo             \ o         O
	 *        5o---------4              11o-------10
	 * </pre>
	 */
	@Test
	public void linkFromRingTwoToRingOneTransfersImportanceToLeftBlock() {
		// Arrange:
		// - all accounts start with 2000 NEM
		final List<PoiAccountState> accountStates = setupAccountStates(12);
		final StandardContext context = new StandardContext();

		// Construct basic ring connections
		final Matrix outlinkMatrix = setupBasicRingStructure();

		// Link blocks
		outlinkMatrix.setAt(3, 6, 100);
		outlinkMatrix.setAt(4, 2, 1);
		outlinkMatrix.setAt(11, 7, 1);

		addOutlinksFromGraph(accountStates, context.height1, outlinkMatrix);

		// Act:
		context.builder.setTeleportationProbability(1.00);
		context.builder.setInterLevelTeleportationProbability(0.0);
		context.builder2.setTeleportationProbability(0.86);
		context.builder2.setInterLevelTeleportationProbability(0.1);

		// Normal page rank
		LOGGER.info("normal page rank:");
		final ColumnVector normalImportances = calculateImportances(context.builder.create(), context.height1, accountStates);
		final double ratio1 = ringImportanceSum(normalImportances, 1) / ringImportanceSum(normalImportances, 2);

		// NCD aware page rank
		LOGGER.info("NCD aware page rank:");
		final ColumnVector ncdAwareImportances = calculateImportances(context.builder2.create(), context.height2, accountStates);
		final double ratio2 = ringImportanceSum(ncdAwareImportances, 1) / ringImportanceSum(ncdAwareImportances, 2);

		LOGGER.info(String.format("normal importance ratio ring 1 : ring 2 is " + ratio1));
		LOGGER.info(String.format("ncd aware importance ratio ring 1 : ring 2 is " + ratio2));

		// Assert:
		// There should have been some importance transferred from the right ring to the left one.
		Assert.assertThat(ratio1 > 1, IsEqual.equalTo(true));
		Assert.assertThat(ratio2 > 1 , IsEqual.equalTo(true));
	}

	// endregion

	private List<PoiAccountState> setupAccountStates(final int numAccounts) {
		// All accounts start with 2000 NEM
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i=0; i<numAccounts; i++) {
			accountStates.add(createAccountStateWithBalance(Amount.fromNem(2000)));
		}

		return accountStates;
	}

	private Matrix setupBasicRingStructure() {
		// construct basic ring connections
		final Matrix outlinkMatrix = new DenseMatrix(12, 12);
		outlinkMatrix.setAt(0, 5, 100);
		outlinkMatrix.setAt(6, 11, 100);
		for (int i = 0; i < 5; ++i) {
			outlinkMatrix.setAt(i + 1, i, 100);
			outlinkMatrix.setAt(i + 7, i + 6, 100);
		}

		return outlinkMatrix;
	}

	private double ringImportanceSum(final ColumnVector importances, final int ring) {
		double sum = 0.0;
		for (int i=0; i<6; i++) {
			sum += importances.getAt(i + 6 * (ring - 1));
		}

		return sum;
	}

	//region users-merchant-exchange

	/**
	 * 10 users transfer NEM to a merchant. The merchant transfers NEM to a exchange.
	 * From the exchange, the NEM flow back to the users.
	 * The importances for the merchant and the exchange are independent of the amount of nem that flows.
	 * This is due to the normalization of the outlink matrix and imo a weak point because the amount should matter.
	 * This can be countered to a certain degree by setting the minOutlinkWeight to a reasonable value.
	 * For our standard values, there is not much difference between normal and ncd aware page rank.
	 *
	 * <pre>
	 *                  --------------------------
	 *                /                U          |
	 *               |    |------------0o-----    |
	 *               |    |  ----------1o-   |    |
	 *               |    | /          2   \ |    |
	 *               |    oo         / .o   \|    |
	 *               ----10o-------/   . ----11o---
	 *                   M o           .   / E
	 *                      \          . o
	 *                       ----------9
	 * </pre>
	 */
	@Test
	public void merchantAndExchangeGetALotMoreImportance() {
		// Arrange:
		// - all accounts start with 2000 NEM
		final List<PoiAccountState> accountStates = setupAccountStates(12);
		final StandardContext context = new StandardContext();

		// Setup transfers from users, merchant and exchange.
		final Matrix outlinkMatrix = new DenseMatrix(12, 12);
		for (int i = 0; i < 10; ++i) {
			outlinkMatrix.setAt(10, i, 100);
			outlinkMatrix.setAt(i, 11, 1);
		}
		outlinkMatrix.setAt(11, 10, 1);

		// Act:
		addOutlinksFromGraph(accountStates, context.height1, outlinkMatrix);

		// Normal page rank
		LOGGER.info("normal page rank:");
		final ColumnVector normalImportances = calculateImportances(context.builder.create(), context.height1, accountStates);

		// NCD aware page rank
		LOGGER.info("NCD aware page rank:");
		final ColumnVector ncdAwareImportances = calculateImportances(context.builder2.create(), context.height2, accountStates);

		// Assert:
		// Merchant and exchange should have higher importance than users.
		Assert.assertThat(normalImportances.getAt(0) < normalImportances.getAt(10), IsEqual.equalTo(true));
		Assert.assertThat(normalImportances.getAt(0) < normalImportances.getAt(11), IsEqual.equalTo(true));
		Assert.assertThat(ncdAwareImportances.getAt(0) < ncdAwareImportances.getAt(10), IsEqual.equalTo(true));
		Assert.assertThat(ncdAwareImportances.getAt(0) < ncdAwareImportances.getAt(11), IsEqual.equalTo(true));
	}

	private class StandardContext {
		final BlockHeight height1 = new BlockHeight(2);
		final BlockHeight height2 = new BlockHeight(2 + 31); // POI_GROUPING
		final PoiOptionsBuilder builder = new PoiOptionsBuilder();
		final PoiOptionsBuilder builder2 = new PoiOptionsBuilder();

		public StandardContext() {
			// TODO 20141014 J-B: i don't really like how you're changing the constant
			DEFAULT_IMPORTANCE_SCORER = new PageRankScorer();
			builder.setClusteringStrategy(new SingleClusterScan());
			builder.setTeleportationProbability(0.85);
			builder.setInterLevelTeleportationProbability(0.0);
			builder2.setTeleportationProbability(0.75);
			builder2.setInterLevelTeleportationProbability(0.1);
		}
	}

	//endregion

	/**
	 * a --o b   c
	 */
	@Test
	public void outlinkHasPositiveImpactOnImportance() {
		// Arrange:
		final List<PoiAccountState> accountStates = Arrays.asList(
				createAccountStateWithBalance(Amount.fromNem(101000)),
				createAccountStateWithBalance(Amount.fromNem(100000)),
				createAccountStateWithBalance(Amount.fromNem(100000)));

		// 0 sends part of balance to 1
		final BlockHeight height = new BlockHeight(2);
		addOutlink(accountStates.get(0), accountStates.get(1), height, Amount.fromNem(1000));

		// Act:
		final ColumnVector importances = calculateImportances(DEFAULT_OPTIONS, height, accountStates);

		// Assert:
		// - all balances are equal
		assertEqualBalances(height, accountStates, 100000);

		// - outlink > inlink > outlier
		Assert.assertThat(importances.getAt(1) > importances.getAt(0), IsEqual.equalTo(true));
		Assert.assertThat(importances.getAt(0) > importances.getAt(2), IsEqual.equalTo(true));
	}

	//endregion

	//region test helpers

	private static void addOutlink(
			final PoiAccountState senderAccountState,
			final PoiAccountState recipientAccountState,
			final BlockHeight blockHeight,
			final Amount amount) {
		senderAccountState.getWeightedBalances().addSend(blockHeight, amount);
		senderAccountState.getImportanceInfo().addOutlink(
				new AccountLink(blockHeight, amount, recipientAccountState.getAddress()));

		recipientAccountState.getWeightedBalances().addReceive(blockHeight, amount);
	}

	private static PoiAccountState createAccountStateWithBalance(final Amount balance) {
		final PoiAccountState accountState = new PoiAccountState(Utils.generateRandomAddress());
		accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, balance);
		return accountState;
	}

	private static Collection<PoiAccountState> createAccountStatesFromGraph(final GraphType graphType) {
		final Matrix outlinkMatrix = OutlinkMatrixFactory.create(graphType);
		return createAccountStatesFromGraph(outlinkMatrix);
	}

	private static Collection<PoiAccountState> createAccountStatesFromGraph(final Matrix outlinkMatrix) {
		final List<PoiAccountState> accountStates = new ArrayList<>();
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final PoiAccountState accountState = new PoiAccountState(Utils.generateRandomAddress());
			accountState.getWeightedBalances().addFullyVested(BlockHeight.ONE, Amount.fromNem(1000000000));
			accountStates.add(accountState);
		}

		addOutlinksFromGraph(accountStates, new BlockHeight(2), outlinkMatrix);
		return accountStates;
	}

	private static void addOutlinksFromGraph(
			final List<PoiAccountState> accountStates,
			final BlockHeight blockHeight,
			final Matrix outlinkMatrix) {
		for (int i = 0; i < outlinkMatrix.getRowCount(); ++i) {
			final MatrixNonZeroElementRowIterator iterator = outlinkMatrix.getNonZeroElementRowIterator(i);
			while (iterator.hasNext()) {
				final MatrixElement element = iterator.next();
				final Amount amount = Amount.fromNem(element.getValue().longValue());
				if (amount.compareTo(Amount.ZERO) > 0) {
					final PoiAccountState senderAccountState = accountStates.get(element.getColumn());
					final PoiAccountState recipientAccountState = accountStates.get(element.getRow());
					addOutlink(senderAccountState, recipientAccountState, blockHeight, amount);
				}
			}
		}
	}

	private static ColumnVector calculateImportances(
			final PoiOptions options,
			final BlockHeight importanceBlockHeight,
			final Collection<PoiAccountState> accountStates) {
		final ImportanceCalculator importanceCalculator = new PoiImportanceCalculator(
				DEFAULT_IMPORTANCE_SCORER,
				options);
		importanceCalculator.recalculate(importanceBlockHeight, accountStates);
		return getImportances(importanceBlockHeight, accountStates);
	}

	private static ColumnVector getBalances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		final List<Amount> balances = accountStates.stream()
				.map(a -> a.getWeightedBalances().getVested(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector balancesVector = new ColumnVector(balances.size());
		for (int i = 0; i < balances.size(); ++i) {
			balancesVector.setAt(i, balances.get(i).getNumNem());
		}

		LOGGER.info(String.format("balances: %s", balancesVector));
		return balancesVector;
	}

	private static void assertEqualBalances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates,
			final double amount) {
		// Act:
		final ColumnVector balances = getBalances(blockHeight, accountStates);

		// Assert:
		for (int i = 0; i < balances.size(); ++i) {
			Assert.assertThat(balances.getAt(i), IsEqual.equalTo(amount));
		}
	}

	private static ColumnVector getImportances(
			final BlockHeight blockHeight,
			final Collection<PoiAccountState> accountStates) {
		final List<Double> importances = accountStates.stream()
				.map(a -> a.getImportanceInfo().getImportance(blockHeight))
				.collect(Collectors.toList());

		final ColumnVector importancesVector = new ColumnVector(importances.size());
		for (int i = 0; i < importances.size(); ++i) {
			importancesVector.setAt(i, importances.get(i));
		}

		LOGGER.info(String.format("importances: %s", importancesVector));
		return importancesVector;
	}

	private class PageRankScorer implements ImportanceScorer {

		@Override
		public ColumnVector calculateFinalScore(final ColumnVector importanceVector, final ColumnVector outlinkVector, final ColumnVector vestedBalanceVector) {
			importanceVector.normalize();
			return importanceVector;
		}
	}

	//endregion
}