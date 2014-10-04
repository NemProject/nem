package org.nem.nis.test;

import org.nem.core.math.*;

/**
 * Static factory class that exposes functions for creating well-known outlink matrices.
 */
public class OutlinkMatrixFactory {

	/**
	 * Creates an outlink matrix representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The outlink matrix.
	 */
	public static Matrix create(final GraphType graphType) {
		Matrix matrix;
		switch (graphType) {
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

	private static Matrix makeAntiSymmetric(final Matrix matrix) {
		return matrix.addElementWise(matrix.transpose().multiply(-1));
	}

}
