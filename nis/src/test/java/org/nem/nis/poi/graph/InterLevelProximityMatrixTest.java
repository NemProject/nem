package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;

import java.util.logging.Logger;

/**
 * Tests for <code>InterLevelProximityMatrix</code>.
 */
public class InterLevelProximityMatrixTest {
	private static final Logger LOGGER = Logger.getLogger(InterLevelProximityMatrixTest.class.getName());

	// TODO 20141002 - not sure how helpful these are since you're only using each once
	// > i think they would be more useful if they were shared across tests
	// > i would also use an enum
	// > maybe this was because you wanted to have the same graphs in this test and the clustering test?
	private final static int GRAPH_SINGLE_NODE = 1;
	private final static int GRAPH_TWO_UNCONNECTED_NODES = 2;
	private final static int GRAPH_TWO_CONNECTED_NODES = 3;
	private final static int GRAPH_LINE_STRUCTURE = 4;
	private final static int GRAPH_RING_STRUCTURE = 5;
	private final static int GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER = 6;
	private final static int GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER = 7;
	private final static int GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER = 8;
	private final static int GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER = 9;
	private final static int GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER = 10;
	private final static int GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS = 11;

	/**
	 * Graph interpretation: i----oj means i has directed edge to j
	 */

	/**
	 * <pre>
	 * Graph:         0
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithSingleNode() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_SINGLE_NODE);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(1, 1, 1);
		a.setAt(0, 0, 1.0);
		final SparseMatrix r = new SparseMatrix(1, 1, 1);
		r.setAt(0, 0, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0     1
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}, {1}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoUnconnectedNodes() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_UNCONNECTED_NODES);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(2, 2, 2);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		final SparseMatrix r = new SparseMatrix(2, 2, 2);
		r.setAt(0, 0, 1.0);
		r.setAt(1, 1, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0----o1
	 * </pre>
	 * Clusters: none
	 * Hubs: none
	 * Outliers: {0}, {1}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoConnectedNodes() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_CONNECTED_NODES);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(2, 2, 2);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 1, 1.0);
		final SparseMatrix r = new SparseMatrix(2, 2, 2);
		r.setAt(0, 0, 1.0 / 2.0);
		r.setAt(1, 0, 1.0 / 2.0);
		r.setAt(1, 1, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	// TODO 20141002 - these tests are dependent on FastScan, i'm not sure if that's good or bad
	// > i think i would prefer to explicitly specify the clusters

	/**
	 * <pre>
	 * Graph:         0----o1----o2----o3----o4
	 * </pre>
	 * Clusters: {1,2,3,4,5}
	 * Hubs: none
	 * Outliers: none
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithLineStructure() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_LINE_STRUCTURE);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 5, 6);
		r.setAt(0, 0, 1.0 / 5.0);
		r.setAt(0, 1, 1.0 / 5.0);
		r.setAt(0, 2, 1.0 / 5.0);
		r.setAt(0, 3, 1.0 / 5.0);
		r.setAt(0, 4, 1.0 / 5.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0----o1----o2
	 *                o           |
	 *                |           o
	 *                4o----------3
	 * </pre>
	 * Clusters: {0,1,2,3,4}
	 * Hubs: none
	 * Outliers: none
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithRingStructure() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_RING_STRUCTURE);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 5, 6);
		r.setAt(0, 0, 1.0 / 5.0);
		r.setAt(0, 1, 1.0 / 5.0);
		r.setAt(0, 2, 1.0 / 5.0);
		r.setAt(0, 3, 1.0 / 5.0);
		r.setAt(0, 4, 1.0 / 5.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0---o1
	 *                o\  o|
	 *                | \/ |
	 *                | /\ |
	 *                |/  oo
	 *                3o---2
	 * </pre>
	 * Clusters: {0,1,2,3}
	 * Hubs: none
	 * Outliers: none
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndNoOutlier() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(4, 1, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(1, 4, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 4.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0---o1---o4
	 *                o\  o|
	 *                | \/ |
	 *                | /\ |
	 *                |/  oo
	 *                3o---2
	 * </pre>
	 * Clusters: {0,1,2,3}
	 * Hubs: none
	 * Outliers: {4}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithOneClusterAndNoHubAndOneOutlier() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(5, 2, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 0, 1.0);
		a.setAt(4, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(2, 5, 4);
		r.setAt(0, 0, 1.0 / 4.0);
		r.setAt(0, 1, 1.0 / 8.0);
		r.setAt(0, 2, 1.0 / 4.0);
		r.setAt(0, 3, 1.0 / 4.0);
		r.setAt(1, 1, 1.0 / 2.0);
		r.setAt(1, 4, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0           4
	 *               / o         o \
	 *              o   \       /   o
	 *             1----o2----o3o----5
	 * </pre>
	 * Clusters: {0,1,2}, {3,4,5}
	 * Hubs: none
	 * Outliers: none
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndNoOutlier() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(6, 2, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 1, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(2, 6, 4);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 6.0);
		r.setAt(1, 2, 1.0 / 6.0);
		r.setAt(1, 3, 1.0 / 3.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0
	 *               / o
	 *              o   \
	 *             1----o2----o6
	 *                   |
	 *                   o
	 *                   3
	 *                  / o
	 *                 o   \
	 *                4----o5
	 * </pre>
	 * Clusters: {0,1,2}, {3,4,5}
	 * Hubs: none
	 * Outliers: {6}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndNoHubAndOneOutlier() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(7, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 1, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 2, 1.0);

		final SparseMatrix r = new SparseMatrix(3, 7, 8);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 9.0);
		r.setAt(1, 2, 1.0 / 9.0);
		r.setAt(1, 3, 1.0 / 3.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);
		r.setAt(2, 2, 1.0 / 3.0);
		r.setAt(2, 6, 1.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0                 5
	 *               / o               o \
	 *              o   \             /   o
	 *             1----o2----o3----o4o----6
	 * </pre>
	 * Clusters: {0,1,2}, {4,5,6}
	 * Hubs: {3}
	 * Outliers: none
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndOneHubAndNoOutlier() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(7, 3, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 2, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);

		final SparseMatrix r = new SparseMatrix(3, 7, 4);
		r.setAt(0, 0, 1.0 / 3.0);
		r.setAt(0, 1, 1.0 / 3.0);
		r.setAt(0, 2, 1.0 / 6.0);
		r.setAt(1, 3, 1.0 / 6.0);
		r.setAt(1, 4, 1.0 / 3.0);
		r.setAt(1, 5, 1.0 / 3.0);
		r.setAt(1, 6, 1.0 / 3.0);
		r.setAt(2, 2, 1.0 / 2.0);
		r.setAt(2, 3, 1.0 / 2.0);

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	/**
	 * <pre>
	 * Graph:         0-------o7-------o5-----o8
	 *               / o               o \
	 *              o   \             /   o
	 *      10o----1----o2----o3----o4o----6
	 *                         |
	 *                         o
	 *                         9
	 * </pre>
	 * Clusters: {0,1,2,10}, {4,5,6}
	 * Hubs: {3}, {7}
	 * Outliers: {8}, {9}
	 */
	@Test
	public void matricesAreCalculatedCorrectlyForGraphWithTwoClustersAndTwoHubsAndTwoOutliers() {
		// Arrange:
		final TestContext context = new TestContext(GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS);

		// Act:
		final InterLevelProximityMatrix interLevel = new InterLevelProximityMatrix(context.clusters, context.neighborhood, context.outlinkMatrix);

		// Assert:
		final SparseMatrix a = new SparseMatrix(11, 6, 4);
		a.setAt(0, 0, 1.0);
		a.setAt(1, 0, 1.0);
		a.setAt(2, 0, 1.0);
		a.setAt(3, 2, 1.0);
		a.setAt(4, 1, 1.0);
		a.setAt(5, 1, 1.0);
		a.setAt(6, 1, 1.0);
		a.setAt(7, 3, 1.0);
		a.setAt(8, 4, 1.0);
		a.setAt(9, 5, 1.0);
		a.setAt(10, 0, 1.0);

		final SparseMatrix r = new SparseMatrix(6, 11, 4);
		r.setAt(0, 0, 1.0 / 8.0);  //  N(0): 2; |A(0)|: 4
		r.setAt(0, 1, 1.0 / 4.0);  //  N(1): 1; |A(0)|: 4
		r.setAt(0, 2, 1.0 / 8.0);  //  N(2): 2; |A(0)|: 4
		r.setAt(0, 10, 1.0 / 4.0); // N(10): 1; |A(0)|: 4
		r.setAt(1, 3, 1.0 / 9.0);  //  N(3): 3; |A(1)|: 3
		r.setAt(1, 4, 1.0 / 3.0);  //  N(4): 1; |A(1)|: 3
		r.setAt(1, 5, 1.0 / 6.0);  //  N(5): 2; |A(1)|: 3
		r.setAt(1, 6, 1.0 / 3.0);  //  N(6): 1; |A(1)|: 3
		r.setAt(1, 7, 1.0 / 6.0);  //  N(7): 2; |A(1)|: 3
		r.setAt(2, 2, 1.0 / 2.0);  //  N(2): 2; |A(2)|: 1
		r.setAt(2, 3, 1.0 / 3.0);  //  N(3): 3; |A(2)|: 1
		r.setAt(3, 0, 1.0 / 2.0);  //  N(0): 2; |A(3)|: 1
		r.setAt(3, 7, 1.0 / 2.0);  //  N(7): 2; |A(3)|: 1
		r.setAt(4, 5, 1.0 / 2.0);  //  N(5): 2; |A(4)|: 1
		r.setAt(4, 8, 1.0);        //  N(8): 1; |A(4)|: 1
		r.setAt(5, 3, 1.0 / 3.0);  //  N(3): 3; |A(5)|: 1
		r.setAt(5, 9, 1.0);        //  N(9): 1; |A(5)|: 1

		Assert.assertThat(interLevel.getA(), IsEqual.equalTo(a));
		Assert.assertThat(interLevel.getR(), IsEqual.equalTo(r));
	}

	//region test infrastructure

	private static SparseMatrix getUndirectedOutlinkMatrixForGraph(final int graphIndex) {
		SparseMatrix matrix;
		switch (graphIndex) {
			case GRAPH_SINGLE_NODE: {
				matrix = new SparseMatrix(1, 1, 1);
				matrix.setAt(0, 0, 1);
				break;
			}

			case GRAPH_TWO_UNCONNECTED_NODES: {
				matrix = new SparseMatrix(2, 2, 2);
				matrix.setAt(0, 0, 1);
				matrix.setAt(1, 1, 1);
				break;
			}

			case GRAPH_TWO_CONNECTED_NODES: {
				matrix = new SparseMatrix(2, 2, 2);
				matrix.setAt(1, 0, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_LINE_STRUCTURE: {
				matrix = new SparseMatrix(5, 5, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_RING_STRUCTURE: {
				matrix = new SparseMatrix(5, 5, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(0, 4, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_ONE_CLUSTERS_NO_HUB_NO_OUTLIER: {
				matrix = new SparseMatrix(4, 4, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(1, 3, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_ONE_CLUSTERS_NO_HUB_ONE_OUTLIER: {
				matrix = new SparseMatrix(5, 5, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(4, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(1, 3, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER: {
				matrix = new SparseMatrix(6, 6, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(3, 5, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER: {
				matrix = new SparseMatrix(7, 7, 8);
				matrix.setAt(1, 0, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(6, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(3, 5, 1);
				matrix.setAt(5, 4, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER: {
				matrix = new SparseMatrix(7, 7, 8);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(4, 6, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS: {
				matrix = new SparseMatrix(11, 11, 8);
				matrix.setAt(1, 0, 1);
				matrix.setAt(7, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(10, 1, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(9, 3, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(8, 5, 1);
				matrix.setAt(4, 6, 1);
				matrix.setAt(5, 7, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			default: {
				throw new IllegalArgumentException("unknown graph");
			}
		}

		return matrix;
	}

	private static SparseMatrix makeAntiSymmetric(final SparseMatrix matrix) {
		return (SparseMatrix)matrix.addElementWise(matrix.transpose().multiply(-1));
	}

	private static class TestContext {
		private final Neighborhood neighborhood;
		private final ClusteringResult clusters;
		private final Matrix outlinkMatrix;

		public TestContext(final int graphIndex) {
			this.outlinkMatrix = getUndirectedOutlinkMatrixForGraph(graphIndex);

			// initialize required variables
			final NodeNeighborMap nodeNeighborMap = new NodeNeighborMap(this.outlinkMatrix);
			this.neighborhood = new Neighborhood(nodeNeighborMap, new DefaultSimilarityStrategy(nodeNeighborMap));
			this.outlinkMatrix.removeNegatives();
			LOGGER.info(this.outlinkMatrix.toString());
			this.clusters = new FastScanClusteringStrategy().cluster(this.neighborhood);
		}
	}

	//endregion
}
