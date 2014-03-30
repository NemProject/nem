package org.nem.peer.trust;

import org.nem.peer.Node;
import org.nem.peer.trust.score.NodeExperience;

/**
 * A Node and NodeExperience pair.
 */
public class NodeExperiencePair {

    private final Node node;
    private final NodeExperience experience;

    /**
     * Creates a new node info.
     *
     * @param node The node.
     * @param experience The node experience.
     */
    public NodeExperiencePair(final Node node, final NodeExperience experience) {
        this.node = node;
        this.experience = experience;
    }

    /**
     * Gets the node.
     *
     * @return The node.
     */
    public Node getNode() { return this.node; }

    /**
     * Gets the node experience.
     *
     * @return The node experience.
     */
    public NodeExperience getExperience() { return this.experience; }
}
