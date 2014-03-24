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
}
