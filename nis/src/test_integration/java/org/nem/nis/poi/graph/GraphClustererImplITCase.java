package org.nem.nis.poi.graph;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.ClusterId;
import org.nem.nis.test.NisUtils;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;

public class GraphClustererImplITCase {
	private static final String WORKING_DIRECTORY = System.getProperty("user.dir");
	private static final File TEST_FILE_DIRECTORY = new File(WORKING_DIRECTORY, "test_files");
	private static final File TEST_MATRIX_FILE = new File(TEST_FILE_DIRECTORY, "test.matrix");

	//region BeforeClass / AfterClass

	@BeforeClass
	public static void createTestFiles() throws IOException {
		final boolean result = TEST_FILE_DIRECTORY.mkdir();

		if (!result) {
			throw new RuntimeException("unable to initialize test suite");
		}
	}

	@AfterClass
	public static void removeTestFiles() throws IOException {
		FileUtils.deleteDirectory(TEST_FILE_DIRECTORY);
	}

	//endregion

	@Test
	public void alphaGraphViewTest() throws IOException {
		final SparseMatrix outlinkMatrix;
		//		try {
		//			outlinkMatrix = SparseMatrix.load("outlink.matrix");
		//		} catch (ClassNotFoundException | IOException e) {
		//			e.printStackTrace();
		//		}
		final int numAccounts = 15;
		final int numEntries = 10 * numAccounts;
		final TestContext context = new TestContext();
		outlinkMatrix = this.createOutlinkMatrix(context, numAccounts, numEntries);

		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params, null);
		//		viewer.saveGraph();
		final Cluster cluster1 = new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5));
		//viewer.setCluster(cluster1);
		viewer.showGraph();
		//		Cluster cluster1 = new Cluster(195, Arrays.asList(195, 132, 134, 136, 75, 77, 205, 18, 148, 88, 153, 27, 96, 97, 35, 100, 165, 37, 39, 46, 112, 179, 116, 118, 59, 126));
		//		Cluster cluster2 = new Cluster(65, Arrays.asList(65, 3, 21, 120, 121));
		//		Cluster cluster3 = new Cluster(5, Arrays.asList(5, 139, 31));
		//		Cluster cluster4 = new Cluster(66, Arrays.asList(66, 36, 164, 6, 56, 201, 123));
		//		Cluster cluster5 = new Cluster(20, Arrays.asList(20, 203, 62));
		//		Cluster cluster6 = new Cluster(32, Arrays.asList(32, 140, 191));
		//		Runnable code = new Runnable() {
		//			@Override
		//			public void run() {
		//				SimpleGraphView simpleGraphView = new SimpleGraphView(cluster1);
		//				simpleGraphView.showGraph();
		//			}
		//		};
		//		code.run();
	}

	@Test
	public void alphaGraphTest() {
		final Matrix outlinkMatrix = MatrixRepository.load(new File("outlink.matrix"));

		final List<GraphClusteringStrategy> clusterers = this.getClusteringStrategies();
		for (final GraphClusteringStrategy clusterer : clusterers) {
			final long start = System.currentTimeMillis();
			final ClusteringResult result = this.calculateClusteringResult(clusterer, outlinkMatrix);
			final long stop = System.currentTimeMillis();
			System.out.println(
					"For " + outlinkMatrix.getColumnCount() + " accounts " + clusterer.getClass().getSimpleName() + " clustering needed " + (stop - start) +
							"ms.");
			System.out.println("The clusterer found " + result.getClusters().size() + " regular clusters, " + result.getHubs().size() + " hubs and " +
					result.getOutliers().size() + " outliers.");
			//logClusteringResult(result);
		}
	}

	@Test
	public void graphClustererAndInterLeveProximityMatrixSpeedTest() throws IOException {

		final TestContext context = new TestContext();
		//		final int numAccounts = 4;
		//		final int numEntries = 5 * numAccounts;
		//		SparseMatrix outlinkMatrix = allOneOutlinkMatrix(4);
		final int numAccounts = 25000;
		final int numEntries = 2 * numAccounts;
		SparseMatrix outlinkMatrix = this.createOutlinkMatrix(context, numAccounts, numEntries);
		//		outlinkMatrix.save("test_clusters");
		System.out.println("Before makeAntiSymmetric: numEmtries=" + outlinkMatrix.getNumEntries());
		outlinkMatrix = makeAntiSymmetric(outlinkMatrix);
		outlinkMatrix.removeNegatives();

		//		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
		//		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
		//		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
		//		viewer.saveGraph();

		System.out.println("After makeAntiSymmetric: numEmtries=" + outlinkMatrix.getNumEntries());
		final List<GraphClusteringStrategy> clusteringStrategies = this.getClusteringStrategies();
		final long totalStart = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			for (final GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
				long start = System.currentTimeMillis();
				final NodeNeighborMap nodeNeighbordMap = new NodeNeighborMap(outlinkMatrix);
				long stop = System.currentTimeMillis();
				System.out.println("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
				final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighbordMap);
				final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighbordMap, strategy);
				start = System.currentTimeMillis();
				final ClusteringResult result = clusteringStrategy.cluster(neighborhood);
				stop = System.currentTimeMillis();
				//System.out.println("For " + outlinkMatrix.getColumnCount() + " accounts with " + outlinkMatrix.getNumEntries() + " entries " + clusteringStrategy.getClass().getSimpleName() + " clustering needed " + (stop-start) + "ms.");
				//System.out.println("The clusterer found " + result.getClusters().size() + " regular clusters, " + result.getHubs().size() + " hubs and " + result.getOutliers().size() + " outliers.");
				final InterLevelProximityMatrix matrix = new InterLevelProximityMatrix(result, neighborhood, outlinkMatrix);
				//logClusteringResult(result);
			}
		}
		final long totalStop = System.currentTimeMillis();
		System.out.println("Setting up everything needed " + (totalStop - totalStart) / (5 * clusteringStrategies.size()) + "ms on average.");
	}

	private SparseMatrix createOutlinkMatrix(final TestContext context, final int numAccounts, final int numEntries) {
		final SparseMatrix outlinkMatrix = new SparseMatrix(numAccounts, numAccounts, 8);
		for (int i = 0; i < numEntries; i++) {
			final int row = (int)(context.random.nextDouble() * numAccounts);
			final int col = (int)(context.random.nextDouble() * numAccounts);
			final int val = (int)(context.random.nextDouble() * 100);
			//System.out.println("account " + col + " --> account " + row + " : " + val);
			outlinkMatrix.setAt(row, col, val);
		}
		return outlinkMatrix;
	}

	private SparseMatrix allOneOutlinkMatrix(final int numAccounts) {
		final SparseMatrix outlinkMatrix = new SparseMatrix(numAccounts, numAccounts, 8);
		for (int i = 0; i < numAccounts; i++) {
			for (int j = 0; j < numAccounts; j++) {
				outlinkMatrix.setAt(i, j, 1);
			}
		}
		return outlinkMatrix;
	}

	@Test
	public void saveAndLoadTest() {
		final int size = 4;
		Matrix matrix = new SparseMatrix(size, size, size);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				matrix.setAt(i, j, size * i + j + 1);
			}
		}

		MatrixRepository.save(matrix, TEST_MATRIX_FILE);
		matrix = MatrixRepository.load(TEST_MATRIX_FILE);

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Assert.assertThat(
						String.format("(%d,%d)", i, j),
						matrix.getAt(i, j),
						IsEqual.equalTo(size * i + j + 1.0));
			}
		}
	}

	private static SparseMatrix makeAntiSymmetric(final SparseMatrix matrix) {
		return (SparseMatrix)matrix.addElementWise(matrix.transpose().multiply(-1));
	}

	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final Matrix outlinkMatrix) {
		final long start = System.currentTimeMillis();
		final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(outlinkMatrix);
		final long stop = System.currentTimeMillis();
		System.out.println("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighborMap);
		final Neighborhood neighborhood = NisUtils.createNeighborhood(nodeNeighborMap, strategy);
		final ClusteringResult result = clusterer.cluster(neighborhood);
		return result;
	}

	/**
	 * Log result of clustering.
	 *
	 * @param result The clustering result.
	 */
	private void logClusteringResult(final ClusteringResult result) {
		System.out.println("Clusters:");
		result.getClusters().stream().forEach(c -> System.out.println(c.toString()));
		System.out.println("Hubs:");
		result.getHubs().stream().forEach(c -> System.out.println(c.toString()));
		System.out.println("Outliers:");
		result.getOutliers().stream().forEach(c -> System.out.println(c.toString()));
	}

	/**
	 * Returns a list of all graph clusterers to be tested.
	 *
	 * @return The list of graph clusterers.
	 */
	private List<GraphClusteringStrategy> getClusteringStrategies() {
		final List<GraphClusteringStrategy> clusteringStrategies = new ArrayList<>();
		clusteringStrategies.add(new ScanClusteringStrategy());
		clusteringStrategies.add(new FastScanClusteringStrategy());

		return clusteringStrategies;
	}

	private static class TestContext {
		final SecureRandom random = new SecureRandom();
	}
}
