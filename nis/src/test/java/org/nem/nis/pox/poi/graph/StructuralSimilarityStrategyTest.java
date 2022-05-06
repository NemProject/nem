package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.NodeId;
import org.nem.nis.test.NisUtils;

public class StructuralSimilarityStrategyTest {

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveNoNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(2));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(5));
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(neighborMap);

		// Assert:
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(0.0));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveNoCommonNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(1, 2, 3));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(4, 5, 6));
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(neighborMap);

		// Assert:
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(0.0));
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(0.0));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveCommonNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(0, 1, 2, 3, 4));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(3, 4, 5, 6));
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(neighborMap);

		// Assert:
		final double expected = 2 / Math.sqrt(20);
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(expected));
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(expected));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveCommonNeighborsAndBothPivotsAreInNeighborSet() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(NisUtils.createNeighbors(1, 2, 3, 4, 5));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(NisUtils.createNeighbors(2, 3, 4, 5, 6));
		final SimilarityStrategy strategy = new StructuralSimilarityStrategy(neighborMap);

		// Assert:
		final double expected = 4 / Math.sqrt(25);
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(expected));
		MatcherAssert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(expected));
	}
}
