package org.nem.peer.trust;

import org.nem.peer.*;

/**
 * Static class containing trust utility functions.
 */
public class TrustUtils {

    /**
     * Flattens all nodes in a NodeCollection and a local node into a node array.
     *
     * @param nodes The nodes.
     * @param localNode The local node.
     * @return An array containing all in the NodeCollection and the local node.
     */
    public static Node[] toNodeArray(final NodeCollection nodes, final Node localNode) {
        final int numNodes = nodes.getActiveNodes().size() + nodes.getInactiveNodes().size() + 1;
        final Node[] nodeArray = new Node[numNodes];

        int index = 0;
        for (final Node node : nodes.getActiveNodes())
            nodeArray[index++] = node;

        for (final Node node : nodes.getInactiveNodes())
            nodeArray[index++] = node;

        nodeArray[index] = localNode;
        return nodeArray;
    }

    /**
     * Updates the local trust values node has with respect to the nodes in nodes.
     *
     * @param node The node.
     * @param nodes The other nodes.
     * @param nodeExperiences Node experiences information.
     * @param preTrustedNodes Pre-trusted node information.
     * @param trustProvider The trust provider.
     * @return The total weight of all local trust scores.
     */
    public static double updateLocalTrust(
        final Node node,
        final Node[] nodes,
        final NodeExperiences nodeExperiences,
        final PreTrustedNodes preTrustedNodes,
        final TrustProvider trustProvider) {

        int index = 0;
        final Vector scoreVector = new Vector(nodes.length);
        for (final Node otherNode : nodes) {
            final NodeExperience experience = nodeExperiences.getNodeExperience(node, otherNode);
            final long successfulCalls = experience.successfulCalls().get();
            final long failedCalls = experience.failedCalls().get();
            final double totalCalls = successfulCalls + failedCalls;

            double score;
            if (totalCalls > 0)
                score = trustProvider.calculateScore(successfulCalls, failedCalls)/totalCalls;
            else
                score = preTrustedNodes.isPreTrusted(otherNode) || node.equals(otherNode) ? 1.0 : 0.0;

            scoreVector.setAt(index++, score);
        }

        double scoreWeight = scoreVector.sum();
        scoreVector.normalize();
        nodeExperiences.setLocalTrustVector(node, nodes, scoreVector);
        return scoreWeight;
    }
}
