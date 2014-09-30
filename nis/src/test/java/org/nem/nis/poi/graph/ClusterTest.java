package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.IsEquivalent;
import org.nem.nis.test.NisUtils;

/**
 * Tests for the Cluster class.
 */
public class ClusterTest {

	private static final ClusterId clusterId1337 = new ClusterId(1337);

	// region constructor
	@Test
	public void clusterIdConstructorWorksCorrectly() {
		// Arrange and act:
		final Cluster cluster = new Cluster(clusterId1337);

		// Assert:
		Assert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
		Assert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(0));
		Assert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(new NodeId[] { }));
	}

	@Test
	public void nodeIdMemberIdsConstructorWorksCorrectly() {
		// Arrange and act:
		final Cluster cluster = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440, 1441, 1442, 1443, 1444));

		// Assert:
		Assert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
		Assert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(7));
		Assert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 1441, 1442, 1443, 1444)));
	}
	// endregion

	// region getId / getMemberIds
	@Test
	public void getIdReturnsCorrectly() {
		// Arrange and act:
		final Cluster cluster = new Cluster(clusterId1337);

		// Assert:
		Assert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
	}

	@Test
	public void getMemberIdsReturnsCorrectly() {
		// Arrange and act:
		final Cluster cluster = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Assert:
		Assert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(3));
		Assert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}
	// endregion

	// region add / merge
	@Test
	public void addWorksCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337);
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster1.add(new NodeId(42));
		cluster1.add(new NodeId(4242));

		cluster2.add(new NodeId(4242));
		cluster2.add(new NodeId(42));

		// Assert:
		Assert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(42, 4242)));
		Assert.assertThat(cluster2.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 42, 4242)));
	}

	@Test
	public void mergeMergesTwoClustersCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		Assert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 129, 625, 1105)));
		Assert.assertThat(cluster2.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}

	@Test
	public void mergeMergesOneEmptyClusterCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337);
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		Assert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
		Assert.assertThat(cluster2.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}

	@Test
	public void mergeHandlesEmptyClustersCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337);
		final Cluster cluster2 = new Cluster(clusterId1337);

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		Assert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList()));
	}
	// endregion

	// region hashCode / equals
	@Test
	public void hashCodesAreDifferentForUniqueClusters() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act and Assert:
		Assert.assertTrue(cluster1.hashCode() != cluster2.hashCode());
	}

	@Test
	public void hashCodesAreSameForSameClusters() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));

		// Act and Assert:
		Assert.assertTrue(cluster1.hashCode() == cluster2.hashCode());
	}

	@Test
	public void equalsReturnsTrueForSameClusters() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));

		// Act and Assert:
		Assert.assertTrue(cluster1.equals(cluster2));
	}

	@Test
	public void equalsReturnsFalseForDifferentClusters() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(925, 108, 1019));

		// Act and Assert:
		Assert.assertTrue(!cluster1.equals(cluster2));
		Assert.assertTrue(!cluster1.equals("Hi there"));
		Assert.assertTrue(!cluster1.equals(null));
	}
	// endregion

	// region size / toString
	@Test
	public void sizeReturnsCorrectSizeOfCluster() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(925, 108, 1019));
		final Cluster cluster3 = new Cluster(clusterId1337);

		// Act and Assert:
		Assert.assertThat(cluster1.size(), IsEqual.equalTo(4));
		Assert.assertThat(cluster2.size(), IsEqual.equalTo(3));
		Assert.assertThat(cluster3.size(), IsEqual.equalTo(0));
	}

	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final Cluster cluster1 = new Cluster(clusterId1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(clusterId1337, NisUtils.toNodeIdList(925, 108, 1019));
		final Cluster cluster3 = new Cluster(clusterId1337);

		// Act and Assert:
		Assert.assertThat(cluster1.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: [129, 625, 1105, 1338]"));
		Assert.assertThat(cluster2.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: [1019, 108, 925]"));
		Assert.assertThat(cluster3.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: []"));
	}
	// endregion
}