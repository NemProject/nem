//package org.nem.nis.poi.graph;
//
//import org.junit.Test;
//import org.nem.core.math.SparseMatrix;
//import org.nem.core.model.primitive.ClusterId;
//import org.nem.nis.poi.graph.view.*;
//import org.nem.nis.test.NisUtils;
//
//import java.io.IOException;
//import java.security.SecureRandom;
//import java.util.*;
//
// TODO: 20140930 J-M this needs to be fixed a bit
//
//public class GraphClustererImplITCase {
//
//	@Test
//	public void alphaGraphViewTest() throws IOException {
//		SparseMatrix outlinkMatrix;
////		try {
////			outlinkMatrix = SparseMatrix.load("outlink.matrix");
////		} catch (ClassNotFoundException | IOException e) {
////			e.printStackTrace();
////		}
//		final int numAccounts = 15;
//		final int numEntries = 10 * numAccounts;
//		final TestContext context = new TestContext();
//		outlinkMatrix = createOutlinkMatrix(context, numAccounts, numEntries);
//
//		PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
//		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
//		PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
////		viewer.saveGraph();
//		Cluster cluster1 = new Cluster(new ClusterId(0), NisUtils.toNodeIdList(0, 1, 2, 3, 4, 5));
//		viewer.setCluster(cluster1);
//		viewer.showGraph();
////		Cluster cluster1 = new Cluster(195, Arrays.asList(195, 132, 134, 136, 75, 77, 205, 18, 148, 88, 153, 27, 96, 97, 35, 100, 165, 37, 39, 46, 112, 179, 116, 118, 59, 126));
////		Cluster cluster2 = new Cluster(65, Arrays.asList(65, 3, 21, 120, 121));
////		Cluster cluster3 = new Cluster(5, Arrays.asList(5, 139, 31));
////		Cluster cluster4 = new Cluster(66, Arrays.asList(66, 36, 164, 6, 56, 201, 123));
////		Cluster cluster5 = new Cluster(20, Arrays.asList(20, 203, 62));
////		Cluster cluster6 = new Cluster(32, Arrays.asList(32, 140, 191));
////		Runnable code = new Runnable() {
////			@Override
////			public void run() {
////				SimpleGraphView simpleGraphView = new SimpleGraphView(cluster1);
////				simpleGraphView.showGraph();
////			}
////		};
////		code.run();
//	}
//
//	@Test
//	public void alphaGraphTest() {
//		SparseMatrix outlinkMatrix=null;
//		try {
//			outlinkMatrix = SparseMatrix.load("outlink.matrix");
//		} catch (ClassNotFoundException | IOException e) {
//			e.printStackTrace();
//		}
//		List<GraphClusteringStrategy> clusterers = getClusteringStrategies();
//		for (GraphClusteringStrategy clusterer : clusterers) {
//			long start = System.currentTimeMillis();
//			final ClusteringResult result = calculateClusteringResult(clusterer, outlinkMatrix);
//			long stop = System.currentTimeMillis();
//			System.out.println("For " + outlinkMatrix.getColumnCount() + " accounts with " + outlinkMatrix.getNumEntries() + " entries " + clusterer.getClass().getSimpleName() + " clustering needed " + (stop-start) + "ms.");
//			System.out.println("The clusterer found " + result.getClusters().size() + " regular clusters, " + result.getHubs().size() + " hubs and " + result.getOutliers().size() + " outliers.");
//			//logClusteringResult(result);
//		}
//	}
//
//	@Test
//	public void graphClustererAndInterLeveProximityMatrixSpeedTest() throws IOException {
//
//		final TestContext context = new TestContext();
////		final int numAccounts = 4;
////		final int numEntries = 5 * numAccounts;
////		SparseMatrix outlinkMatrix = allOneOutlinkMatrix(4);
//		final int numAccounts = 25000;
//		final int numEntries = 2 * numAccounts;
//		SparseMatrix outlinkMatrix = createOutlinkMatrix(context, numAccounts, numEntries);
////		outlinkMatrix.save("test_clusters");
//		System.out.println("Before makeAntiSymmetric: numEmtries=" + outlinkMatrix.getNumEntries());
//		outlinkMatrix = makeAntiSymmetric(outlinkMatrix);
//		outlinkMatrix.removeNegatives();
//
////		final PoiGraphParameters params = PoiGraphParameters.getDefaultParams();
////		params.set("layout", Integer.toString(PoiGraphViewer.KAMADA_KAWAI_LAYOUT));
////		final PoiGraphViewer viewer = new PoiGraphViewer(outlinkMatrix, params);
////		viewer.saveGraph();
//
//		System.out.println("After makeAntiSymmetric: numEmtries=" + outlinkMatrix.getNumEntries());
//		List<GraphClusteringStrategy> clusteringStrategies = getClusteringStrategies();
//		long totalStart = System.currentTimeMillis();
//		for (int i=0; i<5; i++) {
//			for (GraphClusteringStrategy clusteringStrategy : clusteringStrategies) {
//				long start = System.currentTimeMillis();
//				final NodeNeighborMap nodeNeighbordMap = new NodeNeighborMap(outlinkMatrix);
//				long stop = System.currentTimeMillis();
//				System.out.println("NodeNeighborMap ctor needed " + (stop - start) + "ms.");
//				final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighbordMap);
//				final Neighborhood neighborhood = new Neighborhood(nodeNeighbordMap, strategy);
//				start = System.currentTimeMillis();
//				ClusteringResult result = clusteringStrategy.cluster(neighborhood);
//				stop = System.currentTimeMillis();
//				//System.out.println("For " + outlinkMatrix.getColumnCount() + " accounts with " + outlinkMatrix.getNumEntries() + " entries " + clusteringStrategy.getClass().getSimpleName() + " clustering needed " + (stop-start) + "ms.");
//				//System.out.println("The clusterer found " + result.getClusters().size() + " regular clusters, " + result.getHubs().size() + " hubs and " + result.getOutliers().size() + " outliers.");
//				InterLevelProximityMatrix matrix = new InterLevelProximityMatrix(result, neighborhood, outlinkMatrix);
//				//logClusteringResult(result);
//			}
//		}
//		long totalStop = System.currentTimeMillis();
//		System.out.println("Setting up everything needed " + (totalStop - totalStart)/(5*clusteringStrategies.size()) + "ms on average.");
//	}
//
//	private SparseMatrix createOutlinkMatrix(final TestContext context, final int numAccounts, final int numEntries) {
//		final SparseMatrix outlinkMatrix = new SparseMatrix(numAccounts,numAccounts,8);
//		for (int i=0; i<numEntries; i++) {
//			final int row = (int)(context.random.nextDouble() * numAccounts);
//			final int col = (int)(context.random.nextDouble() * numAccounts);
//			final int val = (int)(context.random.nextDouble() * 100);
//			//System.out.println("account " + col + " --> account " + row + " : " + val);
//			outlinkMatrix.setAt(row, col, val);
//		}
//		return outlinkMatrix;
//	}
//
//	private SparseMatrix allOneOutlinkMatrix(final int numAccounts) {
//		final SparseMatrix outlinkMatrix = new SparseMatrix(numAccounts,numAccounts,8);
//		for (int i=0; i<numAccounts; i++) {
//			for (int j=0; j<numAccounts; j++) {
//				outlinkMatrix.setAt(i, j, 1);
//			}
//		}
//		return outlinkMatrix;
//	}
//
//	@Test
//	public void saveAndLoadTest() {
//		SparseMatrix matrix = new SparseMatrix(4,4,4);
//		for (int i=0; i<4; i++) {
//			for (int j=0; j<4; j++) {
//				matrix.setAt(i, j, 4*i+j+1);
//			}
//		}
//		try {
//			matrix.save("test.matrix");
//			matrix = SparseMatrix.load("test.matrix");
//		} catch (IOException | ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private static SparseMatrix makeAntiSymmetric(final SparseMatrix matrix) {
//		return (SparseMatrix)matrix.addElementWise(matrix.transpose().multiply(-1));
//	}
//
//	private ClusteringResult calculateClusteringResult(final GraphClusteringStrategy clusterer, final SparseMatrix outlinkMatrix) {
//		long start = System.currentTimeMillis();
//		final NodeNeighborMap nodeNeighbordMap = new NodeNeighborMap(outlinkMatrix);
//		long stop = System.currentTimeMillis();
//		System.out.println("NodeNeighborMap ctor needed " + (stop-start) + "ms.");
//		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(nodeNeighbordMap);
//		final Neighborhood neighborhood = new Neighborhood(nodeNeighbordMap, strategy);
//		ClusteringResult result = clusterer.cluster(neighborhood);
//		return result;
//	}
//
//	/**
//	 * Log result of clustering.
//	 *
//	 * @param result The clustering result.
//	 */
//	private void logClusteringResult(final ClusteringResult result) {
//		System.out.println("Clusters:");
//		result.getClusters().stream().forEach(c -> System.out.println(c.toString()));
//		System.out.println("Hubs:");
//		result.getHubs().stream().forEach(c -> System.out.println(c.toString()));
//		System.out.println("Outliers:");
//		result.getOutliers().stream().forEach(c -> System.out.println(c.toString()));
//	}
//
//	/**
//	 * Returns a list of all graph clusterers to be tested.
//	 *
//	 * @return The list of graph clusterers.
//	 */
//	private List<GraphClusteringStrategy> getClusteringStrategies() {
//		List<GraphClusteringStrategy> clusteringStrategies = new ArrayList<>();
//		clusteringStrategies.add(new Scan());
//		clusteringStrategies.add(new FastScan());
//
//		return clusteringStrategies;
//	}
//
//	private static class TestContext {
//		SecureRandom random = new SecureRandom();
//	}
//}
