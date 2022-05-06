package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

import java.util.*;

/**
 * Tests for the Cluster class.
 */
public class ClusterTest {

	private static final ClusterId CLUSTER_ID_1337 = new ClusterId(1337);

	// region constructor

	@Test
	public void clusterCanBeCreatedAroundClusterId() {
		// Arrange and act:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337);

		// Assert:
		MatcherAssert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
		MatcherAssert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(0));
	}

	@Test
	public void clusterCanBeCreatedAroundNodeId() {
		// Arrange and act:
		final Cluster cluster = new Cluster(new NodeId(625));

		// Assert:
		MatcherAssert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(625)));
		MatcherAssert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(625)));
	}

	@Test
	public void clusterCanBeCreatedAroundClusterIdAndMemberIds() {
		// Arrange and act:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 1339, 1440, 1541, 1542, 1443, 1444));

		// Assert:
		MatcherAssert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
		MatcherAssert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(7));
		MatcherAssert.assertThat(cluster.getMemberIds(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 1541, 1542, 1443, 1444)));
	}

	@Test
	public void clusterConstructorRemovesDuplicateMemberIds() {
		// Arrange and act:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1222, 1338, 1338, 1338, 1444));

		// Assert:
		MatcherAssert.assertThat(cluster.getId(), IsEqual.equalTo(new ClusterId(1337)));
		MatcherAssert.assertThat(cluster.getMemberIds().size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1222, 1338, 1444)));
	}

	@Test
	public void clusterConstructorFailsIfMemberIdsIsNull() {
		// Act:
		ExceptionAssert.assertThrows(v -> new Cluster(CLUSTER_ID_1337, null), IllegalArgumentException.class);
	}

	// endregion

	// region add

	@Test
	public void addCanAddNewMemberIdsToEmptyCluster() {
		// Arrange:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337);

		// Act:
		cluster.add(new NodeId(42));
		cluster.add(new NodeId(4242));
		cluster.add(new NodeId(3000));

		// Assert:
		MatcherAssert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(42, 4242, 3000)));
	}

	@Test
	public void addCanAddNewMemberIdsToNonEmptyCluster() {
		// Arrange:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster.add(new NodeId(42));
		cluster.add(new NodeId(4242));
		cluster.add(new NodeId(3000));

		// Assert:
		MatcherAssert.assertThat(cluster.getMemberIds(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 42, 4242, 3000)));
	}

	@Test
	public void addHasNoEffectIfMemberIdIsAlreadyInCluster() {
		// Arrange:
		final Cluster cluster = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster.add(new NodeId(1338));
		cluster.add(new NodeId(1339));
		cluster.add(new NodeId(1440));

		// Assert:
		MatcherAssert.assertThat(cluster.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}

	// endregion

	// region merge

	@Test
	public void mergeMergesTwoClustersCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		MatcherAssert.assertThat(cluster1.getMemberIds(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440, 129, 625, 1105)));
		MatcherAssert.assertThat(cluster2.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}

	@Test
	public void mergeMergesOneEmptyClusterCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(CLUSTER_ID_1337);
		final Cluster cluster2 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 1339, 1440));

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		MatcherAssert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
		MatcherAssert.assertThat(cluster2.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList(1338, 1339, 1440)));
	}

	@Test
	public void mergeHandlesEmptyClustersCorrectly() {
		// Arrange:
		final Cluster cluster1 = new Cluster(CLUSTER_ID_1337);
		final Cluster cluster2 = new Cluster(CLUSTER_ID_1337);

		// Act:
		cluster1.merge(cluster2);

		// Assert:
		MatcherAssert.assertThat(cluster1.getMemberIds(), IsEquivalent.equivalentTo(NisUtils.toNodeIdList()));
	}

	// endregion

	// region hashCode / equals

	@SuppressWarnings("serial")
	private static final Map<String, Cluster> DESC_TO_CLUSTER_MAP = new HashMap<String, Cluster>() {
		{
			this.put("default", new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1338, 1339, 1440)));
			this.put("diff-cluster-id", new Cluster(new ClusterId(1336), NisUtils.toNodeIdList(1338, 1339, 1440)));
			this.put("diff-member-ids", new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1336, 1339, 1442)));
			this.put("diff-less-member-ids", new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1338, 1339)));
			this.put("diff-more-member-ids", new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1338, 1339, 1440, 1441)));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Cluster cluster = new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1338, 1339, 1440));

		// Assert:
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("default"), IsEqual.equalTo(cluster));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-cluster-id"), IsEqual.equalTo(cluster));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-member-ids"), IsNot.not(IsEqual.equalTo(cluster)));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-less-member-ids"), IsNot.not(IsEqual.equalTo(cluster)));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-more-member-ids"), IsNot.not(IsEqual.equalTo(cluster)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(cluster)));
		MatcherAssert.assertThat(new ClusterId(1337), IsNot.not(IsEqual.equalTo((Object) cluster)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Cluster cluster = new Cluster(new ClusterId(1337), NisUtils.toNodeIdList(1338, 1339, 1440));
		final int hashCode = cluster.hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-cluster-id").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-member-ids").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-less-member-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_CLUSTER_MAP.get("diff-more-member-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsNumberOfMembersInCluster() {
		// Arrange:
		final Cluster cluster1 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(925, 108, 1019));
		final Cluster cluster3 = new Cluster(CLUSTER_ID_1337);

		// Act and Assert:
		MatcherAssert.assertThat(cluster1.size(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(cluster2.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(cluster3.size(), IsEqual.equalTo(0));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsCorrectRepresentation() {
		// Arrange:
		final Cluster cluster1 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(1338, 129, 625, 1105));
		final Cluster cluster2 = new Cluster(CLUSTER_ID_1337, NisUtils.toNodeIdList(925, 108, 1019));
		final Cluster cluster3 = new Cluster(CLUSTER_ID_1337);

		// Act and Assert:
		MatcherAssert.assertThat(cluster1.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: [129, 625, 1105, 1338]"));
		MatcherAssert.assertThat(cluster2.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: [1019, 108, 925]"));
		MatcherAssert.assertThat(cluster3.toString(), IsEqual.equalTo("Cluster Id: 1337; Member Ids: []"));
	}

	// endregion
}
