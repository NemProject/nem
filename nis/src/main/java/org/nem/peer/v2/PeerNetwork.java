package org.nem.peer.v2;

import java.util.*;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

    private final Config config;
    private NodeStatusDemux nodes;
    private final PeerConnector connector;

    /**
     * Creates a new network with the specified configuration.
     *
     * @param config The network configuration.
     * @param connector The peer connector to use.
     */
    public PeerNetwork(final Config config, final PeerConnector connector) {
        this.config = config;
        this.nodes = new NodeStatusDemux(new ArrayList<Node>());
        this.connector = connector;

        for (final NodeEndpoint endpoint : config.getWellKnownPeers())
            nodes.update(new NodeInfo(endpoint, "Unknown", "Unknown"), NodeStatus.INACTIVE);
    }

    /**
     * Gets the local node.
     *
     * @return The local node.
     */
    public NodeInfo getLocalNode() { return this.config.getLocalNode().getInfo(); }

    /**
     * Gets all nodes known to the network.
     *
     * @return All nodes known to the network.
     */
    public NodeStatusDemux getNodes() { return this.nodes; }

    /**
     * Refreshes the network.
     */
    public void refresh() {

        // TODO: need to add back calls for requesting peer lists
        // TODO: should we check for node consistency?

        // TODO: not sure if i like this, but it's late ... revisit
        NodeStatusDemux oldNodes = this.nodes;
        this.nodes = new NodeStatusDemux(new ArrayList<Node>());

        for (final NodeInfo node : oldNodes.getActiveNodes())
            this.refreshNode(node);

        for (final NodeInfo node : oldNodes.getInactiveNodes())
            this.refreshNode(node);
    }

    private void refreshNode(final NodeInfo node) {
        NodeInfo updatedNode = node;
        NodeStatus updatedStatus = NodeStatus.ACTIVE;
        try {
            updatedNode = this.connector.getInfo(node.getEndpoint());
        }
        catch (InactivePeerException e) {
            updatedStatus = NodeStatus.INACTIVE;
        }
        catch (FatalPeerException e) {
            updatedStatus = NodeStatus.FAILURE;
        }

        this.nodes.update(updatedNode, updatedStatus);
    }
}
