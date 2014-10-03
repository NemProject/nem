package org.nem.nis.poi.graph;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

import java.util.*;
import java.util.stream.Collectors;

public class NeighborhoodTest {

	//region getCommunity

	@Test
	public void getCommunityAlwaysTreatsPivotNodeAsSimilar() {
		// Assert: the pivot node should always be treated as similar even if the similarity score indicates it is not similar with itself
		assertSimilarNeighbor(2, 2, 0.0);
	}

	@Test
	public void getCommunityTreatsNodesWithSimilarityScoreGreaterThanEpsilonAsSimilar() {
		// Assert:
		assertSimilarNeighbor(2, 4, 0.651);
	}

	@Test
	public void getCommunityTreatsNodesWithSimilarityScoreEqualToEpsilonAsDissimilar() {
		// Assert:
		assertDissimilarNeighbor(2, 4, 0.65);
	}

	@Test
	public void getCommunityTreatsNodesWithSimilarityScoreLessThanEpsilonAsDissimilar() {
		// Assert:
		assertDissimilarNeighbor(2, 4, 0.649);
	}

	private static Community getCommunity(final int pivotId, final int neighborId, final double similarity) {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(pivotId))).thenReturn(NisUtils.createNeighbors(neighborId));

		final SimilarityStrategy strategy = Mockito.mock(SimilarityStrategy.class);
		Mockito.when(strategy.calculateSimilarity(new NodeId(pivotId), new NodeId(neighborId))).thenReturn(similarity);

		final Neighborhood neighborhood = new Neighborhood(repository, strategy);

		// Act:
		return neighborhood.getCommunity(new NodeId(pivotId));
	}

	private static void assertSimilarNeighbor(final int pivotId, final int neighborId, final double similarity) {
		// Act:
		final Community community = getCommunity(pivotId, neighborId, similarity);

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(pivotId)));
		Assert.assertThat(community.getSimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(neighborId)));
		Assert.assertThat(community.getDissimilarNeighbors().toList().size(), IsEqual.equalTo(0));
	}

	private static void assertDissimilarNeighbor(final int pivotId, final int neighborId, final double similarity) {
		// Act:
		final Community community = getCommunity(pivotId, neighborId, similarity);

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(pivotId)));
		Assert.assertThat(community.getSimilarNeighbors().toList().size(), IsEqual.equalTo(0));
		Assert.assertThat(community.getDissimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(neighborId)));
	}

	@Test
	public void getCommunityCreatesCommunityAroundSpecifiedNode() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(0, 1, 3, 7, 9));

		final SimilarityStrategy strategy = Mockito.mock(SimilarityStrategy.class);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(0))).thenReturn(0.72);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(1))).thenReturn(0.701);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(3))).thenReturn(0.4);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(7))).thenReturn(0.8);
		Mockito.when(strategy.calculateSimilarity(new NodeId(2), new NodeId(9))).thenReturn(0.699);

		final Neighborhood neighborhood = new Neighborhood(repository, strategy);

		// Act:
		final Community community = neighborhood.getCommunity(new NodeId(2));

		// Assert:
		Assert.assertThat(community.getPivotId(), IsEqual.equalTo(new NodeId(2)));
		Assert.assertThat(community.getSimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1, 7, 9)));
		Assert.assertThat(community.getDissimilarNeighbors().toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3)));
	}

	@Test
	public void getCommunityCachesCommunityResult() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors());
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Community community1 = neighborhood.getCommunity(new NodeId(2));
		final Community community2 = neighborhood.getCommunity(new NodeId(2));

		// Assert:
		Assert.assertThat(community2, IsSame.sameInstance(community1));
	}

	//endregion

	//region getNeighboringCommunities

	@Test
	public void getNeighboringCommunitiesReturnsAllNeighboringCommunities() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(0, 1, 3, 7, 9));
		Mockito.when(repository.getNeighbors(new NodeId(0))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(2));
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final Collection<Community> neighboringCommunities = neighborhood.getNeighboringCommunities(new NodeId(2));

		// Assert:
		Assert.assertThat(
				neighboringCommunities.stream().map(Community::getPivotId).collect(Collectors.toList()),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1, 3, 7, 9)));
	}

	//endregion

	//region getTwoHopAwayNeighbors

	@Test
	public void getTwoHopAwayNeighborsReturnsEmptyNodeNeighborsWhenNodeHasZeroSimilarNeighbor() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors());
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert:
		Assert.assertThat(neighbors.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getTwoHopAwayNeighborsReturnsEmptyNodeNeighborsWhenNodeHasOneSimilarNeighbor() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2));
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert:
		Assert.assertThat(neighbors.size(), IsEqual.equalTo(0));
	}

	@Test
	public void getTwoHopAwayNeighborsReturnsNonEmptyNodeNeighborsWhenNodeHasAtLeastTwoSimilarNeighbors() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(1))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2, 3, 5, 7));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(3, 5, 7, 11));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(2, 9, 11));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(1, 10, 12));
		Mockito.when(repository.getNeighbors(new NodeId(10))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(11))).thenReturn(NisUtils.createNeighbors(3, 7));
		Mockito.when(repository.getNeighbors(new NodeId(12))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors(7));
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert:
		Assert.assertThat(
				neighbors.toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(1, 2, 3, 5, 7, 9, 10, 11, 12)));
	}

	// TODO 20141001 - are we sure this is correct?
    // TODO 20141002 M-J: it depends on our GraphConstants
	// TODO 20141002 - specifically, should the two-hop away neighbors include the pivot
    // TODO 20141003: It seems weird, but I think there was a reason for doing it this way :) I'll try to change it and see if it breaks everything.
	@Test
	public void getTwoHopAwayNeighborsIncludesPivot() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2, 3, 4));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(2, 5));
		Mockito.when(repository.getNeighbors(new NodeId(4))).thenReturn(NisUtils.createNeighbors(7));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors());
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert: 2 is included
		Assert.assertThat(
				neighbors.toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 5, 7)));
	}

	// TODO 20141001 - are we sure this is correct?
    // TODO 20141002 M-J: it depends on our GraphConstants
	// TODO 20141002 - specifically, should the two-hop away neighbors include neighbors that are one and two hops away
    // TODO 20141003 M-J: this seems wrong to me.
	@Test
	public void getTwoHopAwayNeighborsIncludesDirectNeighbors() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2, 3, 4));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(4, 5));
		Mockito.when(repository.getNeighbors(new NodeId(4))).thenReturn(NisUtils.createNeighbors(3, 7));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(3));
		Mockito.when(repository.getNeighbors(new NodeId(7))).thenReturn(NisUtils.createNeighbors(4));
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert: 3 and 4 are included
		Assert.assertThat(
				neighbors.toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3, 4, 5, 7)));
	}

	@Test
	public void getTwoHopAwayNeighborsFiltersDuplicateNeighbors() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2, 3, 4));
		Mockito.when(repository.getNeighbors(new NodeId(3))).thenReturn(NisUtils.createNeighbors(5, 8));
		Mockito.when(repository.getNeighbors(new NodeId(4))).thenReturn(NisUtils.createNeighbors(5, 9));
		Mockito.when(repository.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(8))).thenReturn(NisUtils.createNeighbors());
		Mockito.when(repository.getNeighbors(new NodeId(9))).thenReturn(NisUtils.createNeighbors());
		final Neighborhood neighborhood = new Neighborhood(repository, createAlwaysSimilarStrategy());

		// Act:
		final NodeNeighbors neighbors = neighborhood.getTwoHopAwayNeighbors(new NodeId(2));

		// Assert: 5 is only present once
		Assert.assertThat(
				neighbors.toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(5, 8, 9)));
	}

	private static SimilarityStrategy createAlwaysSimilarStrategy() {
		final SimilarityStrategy strategy = Mockito.mock(SimilarityStrategy.class);
		Mockito.when(strategy.calculateSimilarity(Mockito.any(), Mockito.any())).thenReturn(1.0);
		return strategy;
	}

	//endregion

	//region size

	@Test
	public void sizeReturnsNeighborhoodRepositoryLogicalSize() {
		// Arrange:
		final NeighborhoodRepository repository = Mockito.mock(NeighborhoodRepository.class);
		Mockito.when(repository.getLogicalSize()).thenReturn(18);
		final Neighborhood neighborhood = new Neighborhood(repository, Mockito.mock(SimilarityStrategy.class));

		// Act:
		final int size = neighborhood.size();

		// Assert:
		Assert.assertThat(size, IsEqual.equalTo(18));
		Mockito.verify(repository, Mockito.only()).getLogicalSize();
	}

	//endregion
}
