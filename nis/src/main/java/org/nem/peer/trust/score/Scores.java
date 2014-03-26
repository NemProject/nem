package org.nem.peer.trust.score;

import org.nem.peer.Node;
import org.nem.peer.trust.Vector;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Scores<T extends Score>  {

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
     * @param other The node being reported about.
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

    private Map<Node, T> getScores(final Node source) {
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
     * @param node The node.
     * @param nodes The other nodes.
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
     * @param node The node.
     * @param nodes The other nodes.
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
}
