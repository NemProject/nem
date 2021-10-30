package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
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
	private static final int MU = 3;

	// region construction

	@Test
	public void communityCannotBeCreatedAroundNullSets() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Community(NODE_ID_7, null, NisUtils.createNeighbors(), MU), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new Community(NODE_ID_7, NisUtils.createNeighbors(), null, MU), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new Community(NODE_ID_7, null, null, MU), IllegalArgumentException.class);
	}

	@Test
	public void communityWithSimilarAndDissimilarNeighborsCanBeCreated() {
		// Arrange:
		final Community community = new Community(new NodeId(4), NisUtils.createNeighbors(1, 4), NisUtils.createNeighbors(3, 7, 8), MU);

		// Assert:
		assertCommunity(community, 4, NisUtils.toNodeIdList(1, 4), NisUtils.toNodeIdList(3, 7, 8), false);
	}

	@Test
	public void communityCannotBeCreatedIfSimilarNeighborsDoesNotContainPivot() {
		// Arrange:
		ExceptionAssert.assertThrows(v -> new Community(NODE_ID_5, NisUtils.createNeighbors(1, 4), NisUtils.createNeighbors(), MU),
				IllegalArgumentException.class);
	}

	@Test
	public void communityWithOnlySimilarNeighborsCanBeCreated() {
		// Arrange:
		final Community community = new Community(new NodeId(4), NisUtils.createNeighbors(1, 4), NisUtils.createNeighbors(), MU);

		// Assert:
		assertCommunity(community, 4, NisUtils.toNodeIdList(1, 4), NisUtils.toNodeIdList(), false);
	}

	@Test
	public void communityWithOnlyDissimilarNeighborsCannotBeCreated() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new Community(NODE_ID_5, NisUtils.createNeighbors(), NisUtils.createNeighbors(3, 7, 8), MU),
				IllegalArgumentException.class);
	}

	@Test
	public void isolatedCommunityCanBeCreated() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, NisUtils.createNeighbors(5), NisUtils.createNeighbors(), MU);

		// Assert:
		assertCommunity(community, 5, NisUtils.toNodeIdList(5), NisUtils.toNodeIdList(), true);
	}

	@Test
	public void isolatedCommunityCanBeCreatedWithoutSpecifyingNeighborIdCollections() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, MU);

		// Assert:
		assertCommunity(community, 5, NisUtils.toNodeIdList(5), NisUtils.toNodeIdList(), true);
	}

	@Test
	public void constructorFiltersOutDuplicateCollectionElements() {
		// Arrange:
		final Community community = new Community(new NodeId(5), NisUtils.createNeighbors(1, 1, 5, 5),
				NisUtils.createNeighbors(3, 3, 7, 7, 8, 8), MU);

		// Assert:
		assertCommunity(community, 5, NisUtils.toNodeIdList(1, 5), NisUtils.toNodeIdList(3, 7, 8), false);
	}

	private static void assertCommunity(final Community community, final int pivotId, final List<NodeId> similarNeighborIds,
			final List<NodeId> dissimilarNeighborIds, final boolean isIsolated) {
		MatcherAssert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(pivotId)));
		MatcherAssert.assertThat(community.getSimilarNeighbors().toList(), IsEquivalent.equivalentTo(similarNeighborIds));
		MatcherAssert.assertThat(community.getDissimilarNeighbors().toList(), IsEquivalent.equivalentTo(dissimilarNeighborIds));
		MatcherAssert.assertThat(community.isIsolated(), IsEqual.equalTo(isIsolated));
	}

	// endregion

	// region size

	@Test
	public void sizeReturnsTheTotalNumberOfNeighbors() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, NisUtils.createNeighbors(1, 4, 5), NisUtils.createNeighbors(3, 7, 8, 9), MU);

		// Assert:
		MatcherAssert.assertThat(community.size(), IsEqual.equalTo(7));
	}

	// endregion

	// region predicates

	@SuppressWarnings("serial")
	private static final Map<String, Community> NAME_TO_COMMUNITY_MAP = new HashMap<String, Community>() {
		{
			this.put("MU_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, NisUtils.createNeighbors(1, 4, 7), NisUtils.createNeighbors(8), MU));
			this.put("MU+1_SIMILAR_NEIGHBORS",
					new Community(NODE_ID_7, NisUtils.createNeighbors(1, 4, 5, 7), NisUtils.createNeighbors(8), MU));
			this.put("MU-1_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, NisUtils.createNeighbors(1, 7), NisUtils.createNeighbors(8), MU));
			this.put("1_SIMILAR_NEIGHBOR", new Community(NODE_ID_7, NisUtils.createNeighbors(1, 7), NisUtils.createNeighbors(), MU));
			this.put("1_DISSIMILAR_NEIGHBOR", new Community(NODE_ID_7, NisUtils.createNeighbors(0, 7), NisUtils.createNeighbors(1), MU));
			this.put("2_SIMILAR_NEIGHBORS", new Community(NODE_ID_7, NisUtils.createNeighbors(1, 2, 7), NisUtils.createNeighbors(), MU));
			this.put("2_DISSIMILAR_NEIGHBORS", new Community(NODE_ID_7, NisUtils.createNeighbors(7), NisUtils.createNeighbors(1, 2), MU));
			this.put("2_TOTAL_NEIGHBORS", new Community(NODE_ID_7, NisUtils.createNeighbors(0, 7), NisUtils.createNeighbors(1), MU));
		}
	};

	@Test
	public void coreCommunitiesAreCorrectlyDetected() {
		// Assert:
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU+1_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("MU-1_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("1_SIMILAR_NEIGHBOR").isCore(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("1_DISSIMILAR_NEIGHBOR").isCore(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_SIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_DISSIMILAR_NEIGHBORS").isCore(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(NAME_TO_COMMUNITY_MAP.get("2_TOTAL_NEIGHBORS").isCore(), IsEqual.equalTo(false));
	}

	// endregion

	// region equals / hashCode

	@SuppressWarnings("serial")
	private static final Map<String, Community> DESC_TO_COMMUNITY_MAP = new HashMap<String, Community>() {
		{
			final NodeNeighbors similarNeighbors = NisUtils.createNeighbors(2, 5, 7);
			final NodeNeighbors dissimilarNeighbors = NisUtils.createNeighbors(0, 3);
			this.put("default", new Community(NODE_ID_2, similarNeighbors, dissimilarNeighbors, MU));
			this.put("diff-pivot-id", new Community(NODE_ID_7, similarNeighbors, dissimilarNeighbors, MU));
			this.put("diff-similar-ids", new Community(NODE_ID_2, NisUtils.createNeighbors(2, 5, 8), dissimilarNeighbors, MU));
			this.put("more-similar-ids", new Community(NODE_ID_2, NisUtils.createNeighbors(2, 5, 7, 8), dissimilarNeighbors, MU));
			this.put("fewer-similar-ids", new Community(NODE_ID_2, NisUtils.createNeighbors(2, 5), dissimilarNeighbors, MU));
			this.put("diff-dissimilar-ids", new Community(NODE_ID_2, similarNeighbors, NisUtils.createNeighbors(0, 4), MU));
			this.put("more-dissimilar-ids", new Community(NODE_ID_2, similarNeighbors, NisUtils.createNeighbors(0, 3, 4), MU));
			this.put("fewer-dissimilar-ids", new Community(NODE_ID_2, similarNeighbors, NisUtils.createNeighbors(0), MU));
			this.put("diff-id-classifications", new Community(NODE_ID_3, dissimilarNeighbors, similarNeighbors, MU));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final Community community = new Community(NODE_ID_2, NisUtils.createNeighbors(2, 5, 7), NisUtils.createNeighbors(0, 3), MU);

		// Assert:
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("default"), IsEqual.equalTo(community));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-pivot-id"), IsEqual.equalTo(community));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-similar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("more-similar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("fewer-similar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-dissimilar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("more-dissimilar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("fewer-dissimilar-ids"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-id-classifications"), IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(community)));
		MatcherAssert.assertThat(NisUtils.createNeighbors(2, 5, 7), IsNot.not(IsEqual.equalTo((Object) community)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Community community = new Community(NODE_ID_2, NisUtils.createNeighbors(2, 5, 7), NisUtils.createNeighbors(1, 3), MU);
		final int hashCode = community.hashCode();

		// Assert:
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("default").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-pivot-id").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-similar-ids").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("more-similar-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("fewer-similar-ids").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-dissimilar-ids").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("more-dissimilar-ids").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("fewer-dissimilar-ids").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(DESC_TO_COMMUNITY_MAP.get("diff-id-classifications").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAnAppropriateRepresentation() {
		// Arrange:
		final Community community = new Community(NODE_ID_5, NisUtils.createNeighbors(1, 4, 5), NisUtils.createNeighbors(3, 7, 8), MU);

		// Assert:
		final String expectedString = "Pivot Id: 5; Similar Neighbor Ids: {1,4,5}; Dissimilar Neighbor Ids: {3,7,8}";
		MatcherAssert.assertThat(community.toString(), IsEqual.equalTo(expectedString));
	}

	// endregion
}
