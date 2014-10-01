package org.nem.nis.poi.graph;

import org.nem.core.math.*;
import org.nem.core.model.primitive.NodeId;

import java.util.*;
import java.util.stream.Collectors;

// TODO-CR [08062014][J-J]: i will need to look close at this too

/**
 * Maintains a mapping of node ids to neighbor node ids.
 */
public class NodeNeighborMap implements NeighborhoodRepository {
	private final int logicalSize;
	private final Map<NodeId, NodeNeighbors> nodeIdToNodeNeighborsMap;

	/**
	 * Creates a new node neighbor map.
	 *
	 * @param matrix Outlink matrix for Accounts.
	 */
	public NodeNeighborMap(final Matrix matrix) {
		final int numCols = matrix.getColumnCount();
		final int numRows = matrix.getRowCount();
		if (numCols != numRows) {
			throw new IllegalArgumentException("NodeNeighborMap requires square matrix");
		}

		this.logicalSize = numCols;
		this.nodeIdToNodeNeighborsMap = new HashMap<>();
		final Matrix transposedMatrix = matrix.transpose();

		// go through the outlink matrix and collect neighbors
		for (int row = 0; row < matrix.getRowCount(); ++row) {
			final NodeId nodeId = new NodeId(row);
			NodeNeighbors nodeNeighbors = null;
			final List<MatrixElement> nonZeroEntries = new ArrayList<>();
			nonZeroEntries.add(new MatrixElement(row, row, 1));
			MatrixNonZeroElementRowIterator iterator = matrix.getNonZeroElementRowIterator(row);
			while (iterator.hasNext()) {
				nonZeroEntries.add(iterator.next());
			}

			iterator = transposedMatrix.getNonZeroElementRowIterator(row);
			while (iterator.hasNext()) {
				nonZeroEntries.add(iterator.next());
			}
			final List<MatrixElement> elements = nonZeroEntries.stream()
					.distinct()
					.sorted((e1, e2) -> e1.getColumn().compareTo(e2.getColumn()))
					.collect(Collectors.toList());
			for (final MatrixElement e : elements) {
				final NodeId neighborId = new NodeId(e.getColumn());
				if (nodeNeighbors == null) {
					nodeNeighbors = new NodeNeighbors(neighborId);
					this.nodeIdToNodeNeighborsMap.put(nodeId, nodeNeighbors);
				} else {
					nodeNeighbors.addNeighbor(neighborId);
				}
			}
		}
	}

	@Override
	public NodeNeighbors getNeighbors(final NodeId nodeId) {
		if (nodeId.getRaw() >= this.logicalSize) {
			throw new IllegalArgumentException("id is out of range");
		}

		// A node is in its set of neighbors
		return this.nodeIdToNodeNeighborsMap.getOrDefault(nodeId, new NodeNeighbors(nodeId));
	}

	@Override
	public int getLogicalSize() {
		return this.logicalSize;
	}
}
