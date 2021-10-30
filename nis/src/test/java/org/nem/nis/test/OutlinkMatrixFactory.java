package org.nem.nis.test;

import org.nem.core.math.*;
import org.nem.nis.pox.poi.PoiOptionsBuilder;

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
			case GRAPH_SINGLE_NODE:
				matrix = new SparseMatrix(1, 1, 1);
				matrix.setAt(0, 0, 1);
				break;

			case GRAPH_TWO_UNCONNECTED_NODES:
				matrix = new SparseMatrix(2, 2, 2);
				matrix.setAt(0, 0, 1);
				matrix.setAt(1, 1, 1);
				break;

			case GRAPH_TWO_CONNECTED_NODES:
				matrix = new SparseMatrix(2, 2, 2);
				matrix.setAt(1, 0, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_LINE_STRUCTURE:
				matrix = new SparseMatrix(5, 5, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_LINE6_STRUCTURE: {
				matrix = new SparseMatrix(6, 6, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(5, 4, 1);
				matrix = makeAntiSymmetric(matrix);
				break;
			}

			case GRAPH_RING_STRUCTURE:
				matrix = new SparseMatrix(5, 5, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(4, 3, 1);
				matrix.setAt(0, 4, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_BOX_TWO_DIAGONALS:
				matrix = new SparseMatrix(4, 4, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(1, 3, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_BOX_MAJOR_DIAGONAL:
				matrix = new SparseMatrix(4, 4, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 0, 1);
				matrix.setAt(3, 0, 1);
				matrix.setAt(0, 1, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(1, 2, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(2, 3, 1);
				break;

			case GRAPH_BOX_MINOR_DIAGONAL:
				matrix = new SparseMatrix(4, 4, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(3, 0, 1);
				matrix.setAt(0, 1, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 1, 1);
				matrix.setAt(1, 2, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(2, 3, 1);
				break;

			case GRAPH_TREE_STRUCTURE:
				matrix = new SparseMatrix(4, 4, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 0, 1);
				matrix.setAt(3, 0, 1);
				break;

			case GRAPH_DISCONNECTED_BOX_WITH_DIAGONAL_AND_CROSS:
				matrix = new DenseMatrix(9, 9);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(0, 2, 1);

				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(7, 5, 1);
				matrix.setAt(8, 5, 1);
				break;

			default :
				throw new IllegalArgumentException("unknown graph");
		}

		return prepareMatrix(matrix);
	}

	/**
	 * Creates an outlink matrix representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The outlink matrix.
	 */
	public static Matrix create(final GraphTypeEpsilon065 graphType) {
		Matrix matrix;
		switch (graphType) {
			case GRAPH_ONE_CLUSTER_NO_HUB_ONE_OUTLIER:
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

			case GRAPH_TWO_CLUSTERS_NO_HUB_NO_OUTLIER:
				matrix = new SparseMatrix(8, 8, 4);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(4, 2, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(7, 6, 1);
				matrix.setAt(4, 6, 1);
				matrix.setAt(5, 7, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_TWO_CLUSTERS_NO_HUB_ONE_OUTLIER:
				matrix = new SparseMatrix(9, 9, 8);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(4, 2, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(7, 6, 1);
				matrix.setAt(4, 6, 1);
				matrix.setAt(5, 7, 1);
				matrix.setAt(2, 8, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_TWO_CLUSTERS_ONE_HUB_NO_OUTLIER:
				matrix = new SparseMatrix(9, 9, 8);
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(2, 8, 1);
				matrix.setAt(8, 4, 1);
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(7, 6, 1);
				matrix.setAt(4, 6, 1);
				matrix.setAt(5, 7, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			case GRAPH_TWO_CLUSTERS_TWO_HUBS_TWO_OUTLIERS:
				matrix = new SparseMatrix(13, 13, 8);
				// box 1 with diagonals + two way connected 12
				matrix.setAt(1, 0, 1);
				matrix.setAt(2, 1, 1);
				matrix.setAt(3, 2, 1);
				matrix.setAt(0, 3, 1);
				matrix.setAt(0, 2, 1);
				matrix.setAt(1, 3, 1);
				matrix.setAt(12, 3, 1);
				matrix.setAt(12, 0, 1);

				// box 2 with diagonals
				matrix.setAt(5, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(7, 6, 1);
				matrix.setAt(4, 6, 1);
				matrix.setAt(5, 7, 1);

				// connections between boxes
				matrix.setAt(2, 8, 1);
				matrix.setAt(8, 4, 1);
				matrix.setAt(1, 9, 1);
				matrix.setAt(9, 5, 1);

				// outliers
				matrix.setAt(6, 10, 1);
				matrix.setAt(8, 11, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			default :
				throw new IllegalArgumentException("unknown graph");
		}

		return prepareMatrix(matrix);
	}

	/**
	 * Creates an outlink matrix representing the specified graph type.
	 *
	 * @param graphType The graph type.
	 * @return The outlink matrix.
	 */
	public static Matrix create(final GraphTypeEpsilon040 graphType) {
		Matrix matrix;
		switch (graphType) {
			case GRAPH_TWO_CLUSTERS_NO_HUBS_NO_OUTLIERS:
				matrix = new DenseMatrix(10, 10);
				setBidirectionalLink(matrix, 0, 1);
				setBidirectionalLink(matrix, 0, 3);
				setBidirectionalLink(matrix, 0, 4);
				setBidirectionalLink(matrix, 0, 5);
				setBidirectionalLink(matrix, 1, 2);
				setBidirectionalLink(matrix, 1, 6);
				setBidirectionalLink(matrix, 1, 7);
				setBidirectionalLink(matrix, 1, 8);
				setBidirectionalLink(matrix, 1, 9);
				setBidirectionalLink(matrix, 2, 3);
				setBidirectionalLink(matrix, 2, 5);
				setBidirectionalLink(matrix, 3, 4);
				setBidirectionalLink(matrix, 3, 5);
				break;

			case GRAPH_TWO_CLUSTERS_ONE_HUB_THREE_OUTLIERS:
				matrix = new DenseMatrix(18, 18);

				// cluster 1
				setBidirectionalLink(matrix, 0, 1);
				setBidirectionalLink(matrix, 0, 2);
				setBidirectionalLink(matrix, 0, 11);
				setBidirectionalLink(matrix, 1, 2);
				setBidirectionalLink(matrix, 1, 10);
				setBidirectionalLink(matrix, 2, 11);
				setBidirectionalLink(matrix, 2, 10);
				setBidirectionalLink(matrix, 2, 14);
				setBidirectionalLink(matrix, 2, 15);
				setBidirectionalLink(matrix, 10, 14);
				setBidirectionalLink(matrix, 11, 15);

				// cluster 2
				setBidirectionalLink(matrix, 4, 5);
				setBidirectionalLink(matrix, 4, 6);
				setBidirectionalLink(matrix, 4, 8);
				setBidirectionalLink(matrix, 4, 9);
				setBidirectionalLink(matrix, 4, 12);
				setBidirectionalLink(matrix, 4, 13);
				setBidirectionalLink(matrix, 5, 6);
				setBidirectionalLink(matrix, 5, 9);
				setBidirectionalLink(matrix, 6, 8);
				setBidirectionalLink(matrix, 8, 12);
				setBidirectionalLink(matrix, 9, 13);
				setBidirectionalLink(matrix, 12, 13);

				// connections
				setBidirectionalLink(matrix, 2, 7);
				setBidirectionalLink(matrix, 2, 3);
				setBidirectionalLink(matrix, 3, 4);
				setBidirectionalLink(matrix, 3, 17);
				setBidirectionalLink(matrix, 7, 16);
				break;

			case GRAPH_THREE_CLUSTERS_TWO_HUBS_THREE_OUTLIERS:
				matrix = new DenseMatrix(20, 20);
				matrix.setAt(1, 0, 1);
				matrix.setAt(4, 0, 1);
				matrix.setAt(4, 1, 1);
				matrix.setAt(2, 3, 1);
				matrix.setAt(7, 3, 1);
				matrix.setAt(9, 3, 1);
				matrix.setAt(15, 3, 1);
				matrix.setAt(10, 4, 1);
				matrix.setAt(14, 4, 1);
				matrix.setAt(6, 5, 1);
				matrix.setAt(8, 5, 1);
				matrix.setAt(11, 5, 1);
				matrix.setAt(12, 5, 1);
				matrix.setAt(6, 8, 1);
				matrix.setAt(2, 9, 1);
				matrix.setAt(3, 16, 1);
				matrix.setAt(4, 16, 1);
				matrix.setAt(5, 16, 1);
				matrix.setAt(17, 16, 1);
				matrix.setAt(3, 18, 1);
				matrix.setAt(4, 18, 1);
				matrix.setAt(5, 18, 1);
				matrix.setAt(19, 18, 1);
				matrix = makeAntiSymmetric(matrix);
				break;

			default :
				throw new IllegalArgumentException("unknown graph");
		}

		return prepareMatrix(matrix);
	}

	private static void setBidirectionalLink(final Matrix matrix, final int id1, final int id2) {
		matrix.setAt(id1, id2, new PoiOptionsBuilder().create().getMinOutlinkWeight().getNumNem());
	}

	private static Matrix makeAntiSymmetric(final Matrix matrix) {
		return matrix.addElementWise(matrix.transpose().multiply(-1));
	}

	private static Matrix prepareMatrix(final Matrix matrix) {
		matrix.removeNegatives();
		return matrix.multiply(new PoiOptionsBuilder().create().getMinOutlinkWeight().getNumNem());
	}
}
