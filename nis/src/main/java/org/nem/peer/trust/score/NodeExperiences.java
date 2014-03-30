package org.nem.peer.trust.score;

import org.nem.peer.Node;
import org.nem.peer.trust.Matrix;
import org.nem.peer.trust.NodeInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains experiences for a set of nodes.
 */
public class NodeExperiences {

    private final Map<Node, Map<Node, NodeExperience>> nodeExperiences = new ConcurrentHashMap<>();

    // TODO: refactor these two functions

    /**
     * Gets the NodeExperience source has with peer.
     *
     * @param source The node reporting experience.
     * @param peer The node being reported about.
     * @return The experience source has with peer.
     */
    public NodeExperience getNodeExperience(final Node source, final Node peer) {
        final Map<Node, NodeExperience> localExperiences = this.getNodeExperiencesInternal(source);

        NodeExperience experience = localExperiences.get(peer);
        if (null == experience) {
            experience = new NodeExperience();
            localExperiences.put(peer, experience);
        }

        return experience;
    }

    private Map<Node, NodeExperience> getNodeExperiencesInternal(final Node source) {
        Map<Node, NodeExperience> localExperiences = this.nodeExperiences.get(source);
        if (null == localExperiences) {
            localExperiences = new ConcurrentHashMap<>();
            this.nodeExperiences.put(source, localExperiences);
        }

        return localExperiences;
    }

    /**
     * Gets a shared experience matrix.
     * Matrix(r, c) contains 1 if node and node(r) have both interacted with node(c).
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @return The shared experiences matrix.
     */
    public Matrix getSharedExperienceMatrix(final Node node, final Node[] nodes) {
        final Matrix matrix = new Matrix(nodes.length, nodes.length);
        for (int i = 0; i < nodes.length; ++i) {
            // skip the node (since the node will not have interacted with itself)
            if (node.equals(nodes[i]))
                continue;

            for (int j = 0; j < nodes.length; ++j) {
                if (nodes[i].equals(nodes[j]))
                    continue;

                final NodeExperience experience1 = this.getNodeExperience(node, nodes[j]);
                final NodeExperience experience2 = this.getNodeExperience(nodes[i], nodes[j]);
                if (experience1.totalCalls() > 0 && experience2.totalCalls() > 0)
                    matrix.setAt(i, j, 1.0);
            }
        }

        return matrix;
    }

    /**
     * Gets all experience information for node.
     *
     * @param node The node for which to get experience information.
     * @return All experience information for node.
     */
    public List<NodeInfo> getNodeExperiences(final Node node) {
        final List<NodeInfo> nodeInfoList = new ArrayList<>();
        final Map<Node, NodeExperience> experiences = this.getNodeExperiencesInternal(node);
        for (final Map.Entry<Node, NodeExperience> entry : experiences.entrySet())
            nodeInfoList.add(new NodeInfo(entry.getKey(), entry.getValue()));

        return nodeInfoList;
    }

    /**
     * Sets experience information for node.
     *
     * @param node The node for which to set experience information.
     * @param nodeInfoList The experience information for node.
     */
    public void setNodeExperiences(final Node node, final List<NodeInfo> nodeInfoList) {
        final Map<Node, NodeExperience> experiences = this.getNodeExperiencesInternal(node);
        for (final NodeInfo info : nodeInfoList)
            experiences.put(info.getNode(), info.getExperience());
    }
}