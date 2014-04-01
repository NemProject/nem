package org.nem.peer.trust.score;

import org.nem.peer.Node;
import org.nem.peer.trust.Matrix;
import org.nem.peer.trust.Vector;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Scores<T extends Score> {

	private final Map<Node, Map<Node, T>> scores = new ConcurrentHashMap<>();

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
	 * @param other  The node being reported about.
	 *
	 * @return The score source has with other.
	 */
	public T getScore(final Node source, final Node other) {
		final Map<Node, T> localScores = this.getScores(source);

		T score = localScores.get(other);
		if (null == score) {
			score = this.createScore();
			localScores.put(other, score);
		}

		return score;
	}

	protected Map<Node, T> getScores(final Node source) {
		Map<Node, T> localExperiences = this.scores.get(source);
		if (null == localExperiences) {
			localExperiences = new ConcurrentHashMap<>();
			this.scores.put(source, localExperiences);
		}

		return localExperiences;
	}

	/**
	 * Gets the scores for the specified node as a vector.
	 *
	 * @param node  The node.
	 * @param nodes The other nodes.
	 *
	 * @return The score vector.
	 */
	public Vector getScoreVector(final Node node, final Node[] nodes) {
		final Vector vector = new Vector(nodes.length);
		for (int i = 0; i < nodes.length; ++i) {
			final T score = this.getScore(node, nodes[i]);
			vector.setAt(i, score.score().get());
		}

		return vector;
	}

	/**
	 * Sets the score that node has with each node in nodes using the specified vector.
	 *
	 * @param node        The node.
	 * @param nodes       The other nodes.
	 * @param scoreVector The score values.
	 */
	public void setScoreVector(final Node node, final Node[] nodes, final Vector scoreVector) {
		if (nodes.length != scoreVector.getSize())
			throw new InvalidParameterException("nodes and scoreVector must be same size");

		for (int i = 0; i < nodes.length; ++i) {
			final T score = this.getScore(node, nodes[i]);
			score.score().set(scoreVector.getAt(i));
		}
	}

	/**
	 * Gets a transposed matrix of score values for all specified nodes.
	 * Matrix(r, c) contains the score that c has with respect to r.
	 *
	 * @param nodes The nodes.
	 *
	 * @return A transposed matrix of local score values.
	 */
	public Matrix getScoreMatrix(final Node[] nodes) {
		final int numNodes = nodes.length;
		final Matrix trustMatrix = new Matrix(numNodes, numNodes);
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
			final Vector vector = this.getScoreVector(node, nodes);
			vector.normalize();
			this.setScoreVector(node, nodes, vector);
		}
	}
}
