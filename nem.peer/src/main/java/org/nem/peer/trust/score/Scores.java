package org.nem.peer.trust.score;

import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.core.utils.AbstractTwoLevelMap;

public abstract class Scores<T extends Score> {

	private final AbstractTwoLevelMap<Node, T> scores = new AbstractTwoLevelMap<Node, T>() {

		@Override
		protected T createValue() {
			return Scores.this.createScore();
		}
	};

	/**
	 * Creates a new blank score.
	 *
	 * @return A new score.
	 */
	protected abstract T createScore();

	/**
	 * Gets the score source has with other.
	 *
	 * @param source The node reporting the score.
	 * @param other The node being reported about.
	 * @return The score source has with other.
	 */
	public T getScore(final Node source, final Node other) {
		return this.scores.getItem(source, other);
	}

	/**
	 * Gets the scores for the specified node as a vector.
	 *
	 * @param node The node.
	 * @param nodes The other nodes.
	 * @return The score vector.
	 */
	public ColumnVector getScoreVector(final Node node, final Node[] nodes) {
		final ColumnVector vector = new ColumnVector(nodes.length);
		for (int i = 0; i < nodes.length; ++i) {
			final T score = this.getScore(node, nodes[i]);
			vector.setAt(i, score.score().get());
		}

		return vector;
	}

	/**
	 * Sets the score that node has with each node in nodes using the specified vector.
	 *
	 * @param node The node.
	 * @param nodes The other nodes.
	 * @param scoreVector The score values.
	 */
	public void setScoreVector(final Node node, final Node[] nodes, final ColumnVector scoreVector) {
		if (nodes.length != scoreVector.size()) {
			throw new IllegalArgumentException("nodes and scoreVector must be same size");
		}

		for (int i = 0; i < nodes.length; ++i) {
			final T score = this.getScore(node, nodes[i]);
			score.score().set(scoreVector.getAt(i));
		}
	}

	/**
	 * Gets a transposed matrix of score values for all specified nodes. Matrix(r, c) contains the score that c has with respect to r.
	 *
	 * @param nodes The nodes.
	 * @return A transposed matrix of local score values.
	 */
	public Matrix getScoreMatrix(final Node[] nodes) {
		final int numNodes = nodes.length;
		final Matrix trustMatrix = new DenseMatrix(numNodes, numNodes);
		for (int i = 0; i < numNodes; ++i) {
			for (int j = 0; j < numNodes; ++j) {
				final Score score = this.getScore(nodes[i], nodes[j]);
				trustMatrix.setAt(j, i, score.score().get());
			}
		}

		return trustMatrix;
	}

	/**
	 * Normalizes the score values so that the sum of a node's scores is 1.
	 *
	 * @param nodes The nodes that should have their scores normalized.
	 */
	public void normalize(final Node[] nodes) {
		for (final Node node : nodes) {
			final ColumnVector vector = this.getScoreVector(node, nodes);
			vector.normalize();
			this.setScoreVector(node, nodes, vector);
		}
	}
}
