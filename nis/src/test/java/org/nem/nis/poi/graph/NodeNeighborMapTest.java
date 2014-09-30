package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.NodeId;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

public class NodeNeighborMapTest {

	@Test
	public void mapCannotBeCreatedAroundNonSquareMatrix() {
		// Assert:
		ExceptionAssert.assertThrows(
				v -> new NodeNeighborMap(new DenseMatrix(7, 8)),
				IllegalArgumentException.class);
		ExceptionAssert.assertThrows(
				v -> new NodeNeighborMap(new DenseMatrix(8, 7)),
				IllegalArgumentException.class);
	}

	@Test
	public void mapCanBeCreatedAroundSquareMatrix() {
		// Arrange:
		final Matrix matrix = new SparseMatrix(7, 7, 2);
		matrix.setAt(1, 0, 3);
		matrix.setAt(1, 1, 1);
		matrix.setAt(2, 4, 2);
		matrix.setAt(3, 5, 1);
		matrix.setAt(5, 3, 8);
		matrix.setAt(5, 6, -1);
		matrix.setAt(6, 2, 2);

		// Act:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(matrix);

		// Assert: the node id is based on the column
		Assert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(7));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(0)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(1)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(2)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 4, 6)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(3)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3, 5)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(4)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 4)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(5)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3, 5, 6)));
		Assert.assertThat(neighborMap.getNeighbors(new NodeId(6)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 5, 6)));
	}

	@Test
	public void getNeighborsFailsIfNodeIdIsOutsideOfLogicalRange() {
		// Arrange:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(new DenseMatrix(7, 7));

		// Assert:
		ExceptionAssert.assertThrows(
				v -> neighborMap.getNeighbors(new NodeId(7)),
				IllegalArgumentException.class);
	}
}