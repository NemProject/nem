package org.nem.peer.trust;

import org.nem.peer.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains experiences for a set of nodes.
 */
public class NodeExperiences {

    private final Map<Node, Map<Node, NodeExperience>> nodeExperiences = new ConcurrentHashMap<>();

    /**
     * Gets the NodeExperience source has with peer.
     *
     * @param source The node reporting experience.
     * @param peer The node being reported about.
     * @return The experience source has with peer.
     */
    public NodeExperience getNodeExperience(final Node source, final Node peer) {
        Map<Node, NodeExperience> localExperiences = this.nodeExperiences.get(source);
        if (null == localExperiences) {
            localExperiences = new ConcurrentHashMap<>();
            this.nodeExperiences.put(source, localExperiences);
        }

        NodeExperience experience = localExperiences.get(peer);
        if (null == experience) {
            experience = new NodeExperience();
            localExperiences.put(peer, experience);
        }

        return experience;
    }

    /**
     * Gets a transposed matrix of local trust values for all specified nodes.
     * Matrix(r, c) contains the local experience that c has with r.
     *
     * @param nodes The nodes.
     * @return A transposed matrix of local trust values.
     */
    public Matrix getTransposedLocalTrustMatrix(final Node[] nodes) {
        final int numNodes = nodes.length;
        final Matrix trustMatrix = new Matrix(numNodes, numNodes);
        for (int i = 0; i < numNodes; ++i) {
            for (int j = 0; j < numNodes; ++j) {
                final NodeExperience experience = this.getNodeExperience(nodes[i], nodes[j]);
                trustMatrix.setAt(j, i, experience.getLocalTrust());
            }
        }

        return trustMatrix;
    }
}
