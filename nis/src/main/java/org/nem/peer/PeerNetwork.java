package org.nem.peer;

import org.nem.core.model.Transaction;

import java.util.*;

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
        final NodeRefresher refresher = new NodeRefresher(this.nodes, this.connector);
        refresher.refresh();
    }

	//
	public void announceTransaction(Transaction transaction) {
		for (final Node node : nodes.getActiveNodes()) {
			this.connector.pushTransaction(node.getEndpoint(), transaction);
		}
	}

	private static class NodeRefresher {
        final NodeCollection nodes;
        final PeerConnector connector;
        final Map<Node, NodeStatus> nodesToUpdate;

        public NodeRefresher(final NodeCollection nodes, final PeerConnector connector) {
            this.nodes = nodes;
            this.connector = connector;
            this.nodesToUpdate = new HashMap<>();
        }

        public void refresh() {
            this.refresh(nodes.getActiveNodes());
            this.refresh(nodes.getInactiveNodes());

            for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
                this.nodes.update(entry.getKey(), entry.getValue());
        }

        private void refresh(final Iterable<Node> iterable) {
            for (final Node node : iterable)
                this.refreshNode(node);
        }

        private void refreshNode(final Node node) {
            Node refreshedNode = node;
            NodeStatus updatedStatus = NodeStatus.ACTIVE;
            try {
                refreshedNode = this.connector.getInfo(node.getEndpoint());

                // if the node returned inconsistent information, drop it for this round
                if (!areCompatible(node, refreshedNode)) {
                    updatedStatus = NodeStatus.FAILURE;
                    refreshedNode = node;
                } else {
                    this.mergePeers(this.connector.getKnownPeers(node.getEndpoint()));
                }
            }
            catch (InactivePeerException e) {
                updatedStatus = NodeStatus.INACTIVE;
            }
            catch (FatalPeerException e) {
                updatedStatus = NodeStatus.FAILURE;
            }

            this.update(refreshedNode, updatedStatus);
        }

        private static boolean areCompatible(final Node lhs, final Node rhs) {
            return lhs.equals(rhs);
        }

        private void update(final Node node, final NodeStatus status) {
            if (status == this.nodes.getNodeStatus(node))
                return;

            this.nodesToUpdate.put(node, status);
        }

        private void mergePeers(final NodeCollection nodes) {
            this.mergePeers(nodes.getActiveNodes(), NodeStatus.ACTIVE);
            this.mergePeers(nodes.getInactiveNodes(), NodeStatus.INACTIVE);
        }

        private void mergePeers(final Iterable<Node> iterable, final NodeStatus status) {
            for (final Node node : iterable) {
                // nodes directly communicated with are already in this.nodes
                // give their direct connection precedence over what peers report
                if (NodeStatus.FAILURE != this.nodes.getNodeStatus(node))
                    continue;

                this.update(node, status);
            }
        }
    }
}
