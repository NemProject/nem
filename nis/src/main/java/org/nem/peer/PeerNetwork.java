package org.nem.peer;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

    private final Config config;
    private NodeCollection nodes;
    private final PeerConnector connector;

    /**
     * Creates a new network with the specified configuration.
     *
     * @param config The network configuration.
     * @param connector The peer connector to use.
     */
    public PeerNetwork(final Config config, final PeerConnector connector) {
        this.config = config;
        this.nodes = new NodeCollection();
        this.connector = connector;

        for (final NodeEndpoint endpoint : config.getWellKnownPeers())
            nodes.update(new Node(endpoint, "Unknown", "Unknown"), NodeStatus.INACTIVE);
    }

    /**
     * Gets the local node.
     *
     * @return The local node.
     */
    public Node getLocalNode() { return this.config.getLocalNode(); }

    /**
     * Gets all nodes known to the network.
     *
     * @return All nodes known to the network.
     */
    public NodeCollection getNodes() { return this.nodes; }

    /**
     * Refreshes the network.
     */
    public void refresh() {

        // TODO: need to add back calls for requesting peer lists
        // TODO: should we check for node consistency?

        // TODO: not sure if i like this, but it's late ... revisit
        NodeCollection oldNodes = this.nodes;
        this.nodes = new NodeCollection();

        for (final Node node : oldNodes.getActiveNodes())
            this.refreshNode(node);

        for (final Node node : oldNodes.getInactiveNodes())
            this.refreshNode(node);
    }

    private void refreshNode(final Node node) {
        Node updatedNode = node;
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
