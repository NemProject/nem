package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.NodeId;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

import java.util.*;

/**
 * Tests for the Community class.
 */
public class CommunityTest {

	private static final NodeId NODE_ID_2 = new NodeId(2);
	private static final NodeId NODE_ID_3 = new NodeId(3);
	private static final NodeId NODE_ID_5 = new NodeId(5);
	private static final NodeId NODE_ID_7 = new NodeId(7);

	//region construction

	@Test
	public void communityCannotBeCreatedAroundNullSets() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new Community(NODE_ID_7, null, new NodeNeighbors()),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new Community(NODE_ID_7, new NodeNeighbors(), null),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new Community(NODE_ID_7, null, null),
				IllegalArgumentException.class);
	}

	@Test
	public void communityWithSimilarAndDissimilarNeighborsCanBeCreated() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, new NodeNeighbors(NisUtils.toNodeIdArray(1, 4)), new NodeNeighbors(NisUtils.toNodeIdArray(3, 7, 8)));

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(1), new NodeId(4) }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(3), new NodeId(7), new NodeId(8) }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(false));
	}

	@Test
	public void communityWithOnlySimilarNeighborsCanBeCreated() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, new NodeNeighbors(NisUtils.toNodeIdArray(1, 4)), new NodeNeighbors());

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(1), new NodeId(4) }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(false));
	}

	@Test
	public void communityWithOnlyDissimilarNeighborsCanBeCreated() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, new NodeNeighbors(), new NodeNeighbors(NisUtils.toNodeIdArray(3, 7, 8)));

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(3), new NodeId(7), new NodeId(8) }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(false));
	}

	@Test
	public void isolatedCommunityCanBeCreated() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, new NodeNeighbors(NisUtils.toNodeIdArray(5)), new NodeNeighbors());

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(5) }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(true));
	}

	@Test
	public void isolatedCommunityCanBeCreatedWithoutSpecifyingNeighborIdCollections() {
		// Arrange:
		final Community community = new Community(NODE_ID_5);

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(5) }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(true));
	}

	@Test
	public void constructorFiltersOutDuplicateCollectionElements() {
		// Arrange:
		final Community community = new Community(
				new NodeId(5),
				new NodeNeighbors(NisUtils.toNodeIdArray(1, 1, 4, 4)),
				new NodeNeighbors(NisUtils.toNodeIdArray(3, 3, 7, 7, 8, 8)));

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(5)));
		Assert.assertThat(community.getSimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(1), new NodeId(4) }));
		Assert.assertThat(community.getDissimilarNeighbors().toList(),
				IsEquivalent.equivalentTo(new NodeId[] { new NodeId(3), new NodeId(7), new NodeId(8) }));
		Assert.assertThat(community.isIsolated(), IsEqual.equalTo(false));
	}

	//endregion

	//region size

	@Test
	public void sizeReturnsTheTotalNumberOfNeighbors() {
		// Arrange:
		final Community community = new Community(
				NODE_ID_5,
				new NodeNeighbors(NisUtils.toNodeIdArray(1, 4, 5)),
				new NodeNeighbors(NisUtils.toNodeIdArray(3, 7, 8, 9)));

		// Assert:
		Assert.assertThat(community.size(), IsEqual.equalTo(7));
	}

	//endregion

	//region predicates

	private static final Map<String, Community> NAME_TO_COMMUNITY_MAP = new HashMap<String, Community>() {
		{
			put("MU_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(1, 4, 7), createNeighbors(8)));
			put("MU+1_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(1, 4, 5, 7), createNeighbors(8)));
			put("MU-1_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(1, 7), createNeighbors(8)));
			put("1_SIMILAR_NEIGHBOR", new Community(NODE_ID_7, createNeighbors(1, 7), createNeighbors()));
			put("1_DISSIMILAR_NEIGHBOR", new Community(NODE_ID_7, createNeighbors(0, 7), createNeighbors(1)));
			put("2_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(1, 2, 7), createNeighbors()));
			put("2_DISSIMILAR_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(7), createNeighbors(1, 2)));
			put("2_TOTAL_NEIGHBORS", new Community(NODE_ID_7, createNeighbors(0, 7), createNeighbors(1)));
		}
	};

	@Test
	public void coreCommunitiesAreCorrectlyDetected() {
		// Assert:
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU+1_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU-1_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(false));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("1_SIMILAR_NEIGHBOR").isCore(), IsEqual.equalTo(false));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("1_DISSIMILAR_NEIGHBOR").isCore(), IsEqual.equalTo(false));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_DISSIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(false));
		Assert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_TOTAL_NEIGHBORS").isCore(), IsEqual.equalTo(false));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final NodeNeighbors neighbors1 = new NodeNeighbors(NisUtils.toNodeIdArray(2, 5, 7));
		final NodeNeighbors neighbors2 = new NodeNeighbors(NisUtils.toNodeIdArray(1, 2));
		final Community community = new Community(NODE_ID_2, neighbors1, neighbors2);

		// Assert:
		Assert.assertThat(new Community(NODE_ID_2, neighbors1, neighbors2), IsEqual.equalTo(community));
		Assert.assertThat(new Community(NODE_ID_3, neighbors1, neighbors2), IsNot.not(IsEqual.equalTo(community)));
		Assert.assertThat(new Community(NODE_ID_2, neighbors1, neighbors1), IsNot.not(IsEqual.equalTo(community)));
		Assert.assertThat(new Community(NODE_ID_2, neighbors2, neighbors2), IsNot.not(IsEqual.equalTo(community)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(community)));
		Assert.assertThat(neighbors1, IsNot.not(IsEqual.equalTo((Object)community)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final NodeNeighbors neighbors1 = new NodeNeighbors(NisUtils.toNodeIdArray(2, 5, 7));
		final NodeNeighbors neighbors2 = new NodeNeighbors(NisUtils.toNodeIdArray(1, 3));
		final Community community = new Community(NODE_ID_2, neighbors1, neighbors2);
		final int hashCode = community.hashCode();

		// Assert:
		Assert.assertThat(new Community(NODE_ID_2, neighbors1, neighbors2).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new Community(NODE_ID_3, neighbors1, neighbors2).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(new Community(NODE_ID_2, neighbors1, neighbors1).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new Community(NODE_ID_2, neighbors2, neighbors2).hashCode(), IsEqual.equalTo(hashCode));
	}

	//endregion

	//region toString

	@Test
	public void toStringReturnsAnAppropriateRepresentation() {
		// Arrange:
		final Community community = new Community(NODE_ID_5,
				new NodeNeighbors(NisUtils.toNodeIdArray(1, 4, 5)),
				new NodeNeighbors(NisUtils.toNodeIdArray(3, 7, 8)));

		// Assert:
		final String expectedString =
				"Pivot Id: 5; Similar Neighbor Ids: {1,4,5}; Dissimilar Neighbor Ids: {3,7,8}";
		Assert.assertThat(
				community.toString(),
				IsEqual.equalTo(expectedString));
	}

	//endregion

	private static NodeNeighbors createNeighbors(final int... ids) {
		return new NodeNeighbors(NisUtils.toNodeIdArray(ids));
	}
}
