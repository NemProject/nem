package org.nem.nis.pox.poi.graph;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.*;
import org.nem.core.model.primitive.NodeId;
import org.nem.core.test.*;
import org.nem.nis.test.NisUtils;

public class NodeNeighborMapTest {

	// region basic

	@Test
	public void mapCannotBeCreatedAroundNonSquareMatrix() {
		// Assert:
		ExceptionAssert.assertThrows(v -> new NodeNeighborMap(new DenseMatrix(7, 8)), IllegalArgumentException.class);
		ExceptionAssert.assertThrows(v -> new NodeNeighborMap(new DenseMatrix(8, 7)), IllegalArgumentException.class);
	}

	@Test
	public void getLogicalSizeReturnsLogicalSizeOfMap() {
		// Arrange:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(new DenseMatrix(7, 7));

		// Assert:
		MatcherAssert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(7));
	}

	@Test
	public void getNeighborsFailsIfNodeIdIsOutsideOfLogicalRange() {
		// Arrange:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(new DenseMatrix(7, 7));

		// Assert:
		ExceptionAssert.assertThrows(v -> neighborMap.getNeighbors(new NodeId(7)), IllegalArgumentException.class);
	}

	// endregion

	// region map contents

	@Test
	public void nodeAlwaysReportsSelfAsNeighbor() {
		// Arrange:
		final Matrix matrix = new SparseMatrix(4, 4, 2);

		// Act:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(matrix);

		// Assert: even though the matrix is zero, all nodes include themselves as neighbors
		MatcherAssert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(0)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(1)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(1)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(2)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(3)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3)));
	}

	@Test
	public void nodeSharesColumnOrRowWithNeighbors() {
		// Arrange:
		// 0010
		// 0000
		// 0001
		// 0000
		final Matrix matrix = new SparseMatrix(4, 4, 2);
		matrix.setAt(0, 2, 1);
		matrix.setAt(2, 3, 1);

		// Act:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(matrix);

		// Assert:
		MatcherAssert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(0)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 2)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(1)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(1)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(2)).toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 2, 3)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(3)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 3)));
	}

	@Test
	public void nodeDoesNotExposeDuplicateNeighbors() {
		// Arrange:
		// 0010
		// 0010
		// 1111
		// 0010
		final Matrix matrix = new SparseMatrix(4, 4, 2);
		for (int i = 0; i < matrix.getRowCount(); ++i) {
			matrix.setAt(i, 2, 1);
			matrix.setAt(2, i, 1);
		}

		// Act:
		final NodeNeighborMap neighborMap = new NodeNeighborMap(matrix);

		// Assert:
		MatcherAssert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(4));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(0)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 2)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(1)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(1, 2)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(2)).toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1, 2, 3)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(3)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 3)));
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
		MatcherAssert.assertThat(neighborMap.getLogicalSize(), IsEqual.equalTo(7));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(0)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(1)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(0, 1)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(2)).toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 4, 6)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(3)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3, 5)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(4)).toList(), IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 4)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(5)).toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(3, 5, 6)));
		MatcherAssert.assertThat(neighborMap.getNeighbors(new NodeId(6)).toList(),
				IsEquivalent.equivalentTo(NisUtils.toNodeIdArray(2, 5, 6)));
	}

	// endregion
}
