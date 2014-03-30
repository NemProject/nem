package org.nem.peer;

import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.scheduling.*;
import org.nem.peer.trust.TrustContext;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.*;

/**
 * Represents a collection of all known NEM nodes.
 */
public class PeerNetwork {

    private final Config config;
    private NodeCollection nodes;
    private final PeerConnector connector;
    private final SchedulerFactory<Node> schedulerFactory;

    private final NodeExperiences nodeExperiences;
    private final TrustContext trustContext;

    /**
     * Creates a new network with the specified configuration.
     *
     * @param config The network configuration.
     * @param connector The peer connector to use.
     * @param schedulerFactory The node scheduler factory to use.
     */
    public PeerNetwork(final Config config, final PeerConnector connector, final SchedulerFactory<Node> schedulerFactory) {
        this.config = config;
        this.nodes = new NodeCollection();
        this.connector = connector;
        this.schedulerFactory = schedulerFactory;

        this.nodeExperiences = new NodeExperiences();

        // TODO: additional integration necessary
        this.trustContext = null;
//        new TrustContext(
//
//        );
//        final Node[] nodes,
//        final Node localNode,
//        final NodeExperiences nodeExperiences,
//        final PreTrustedNodes preTrustedNodes

        for (final Node node : config.getPreTrustedNodes().getNodes())
            nodes.update(node, NodeStatus.INACTIVE);
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
        final NodeRefresher refresher = new NodeRefresher(this.nodes, this.connector, this.schedulerFactory);
        refresher.refresh();
    }

    /**
     * Broadcasts an entity to all active nodes.
     *
     * @param broadcastId The type of entity.
     * @param entity The entity.
     */
    public void broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
        Scheduler<Node> scheduler = this.schedulerFactory.createScheduler(new Action<Node>() {
            @Override
            public void execute(final Node element) {
                connector.announce(element.getEndpoint(), broadcastId, entity);
            }
        });

        scheduler.push(this.nodes.getActiveNodes());
        scheduler.block();
    }

	private static class NodeRefresher {
        final NodeCollection nodes;
        final PeerConnector connector;
        final SchedulerFactory<Node> schedulerFactory;
        final Map<Node, NodeStatus> nodesToUpdate;

        public NodeRefresher(final NodeCollection nodes, final PeerConnector connector, final SchedulerFactory<Node> schedulerFactory) {
            this.nodes = nodes;
            this.connector = connector;
            this.schedulerFactory = schedulerFactory;
            this.nodesToUpdate = new HashMap<>();
        }

        public void refresh() {
            Scheduler<Node> scheduler = this.schedulerFactory.createScheduler(new Action<Node>() {
                @Override
                public void execute(final Node element) {
                    refreshNode(element);
                }
            });

            scheduler.push(this.nodes.getActiveNodes());
            scheduler.push(this.nodes.getInactiveNodes());
            scheduler.block();

            for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
                this.nodes.update(entry.getKey(), entry.getValue());
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
