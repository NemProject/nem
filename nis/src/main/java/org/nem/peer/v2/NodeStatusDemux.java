package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * Demultiplexes a node collection into separate node info collections based on node status.
 * TODO: The naming no longer makes sense since there isn't a list that needs demultiplexing.
 * TODO: A name like NodeCollection makes more sense since the PeerNetwork uses this to hold all of its nodes.
 */
public class NodeStatusDemux implements SerializableEntity {

    private static ObjectDeserializer<NodeInfo> NODE_INFO_DESERIALIZER = new ObjectDeserializer<NodeInfo>() {
        @Override
        public NodeInfo deserialize(final Deserializer deserializer) {
            return new NodeInfo(deserializer);
        }
    };

    final Set<NodeInfo> activeNodes;
    final Set<NodeInfo> inactiveNodes;

    /**
     * Demultiplexes an collection of nodes.
     *
     * @param nodes The collection to demux.
     */
    public NodeStatusDemux(final Iterable<Node> nodes) {
        this.activeNodes = new HashSet<>();
        this.inactiveNodes = new HashSet<>();

        for (final Node node : nodes) {
            final Set<NodeInfo> nodeList;
            switch (node.getStatus()) {
                case ACTIVE:
                    nodeList = this.activeNodes;
                    break;

                case INACTIVE:
                    nodeList = this.inactiveNodes;
                    break;

                default:
                    nodeList = null;
                    break;
            }

            if (null == nodeList)
                continue;

            nodeList.add(node.getInfo());
        }
    }

    /**
     * Deserializes demultiplexed nodes.
     *
     * @param deserializer The deserializer.
     */
    public NodeStatusDemux(final Deserializer deserializer) {
        this.activeNodes = new HashSet<>(deserializer.readObjectArray("active", NODE_INFO_DESERIALIZER));
        this.inactiveNodes = new HashSet<>(deserializer.readObjectArray("inactive", NODE_INFO_DESERIALIZER));
    }

    /**
     * Gets a collection of active nodes.
     *
     * @return A collection of active nodes.
     */
    public Collection<NodeInfo> getActiveNodes() { return this.activeNodes; }

    /**
     * Gets a collection of inactive nodes.
     *
     * @return A collection of active nodes.
     */
    public Collection<NodeInfo> getInactiveNodes() { return this.inactiveNodes; }

    /**
     * Updates this collection to include the specified node with the associated status.
     * The new node information will replace any previous node information.
     *
     * @param node The node.
     * @param status The node status.
     */
    public void update(final NodeInfo node, final NodeStatus status) {
        this.activeNodes.remove(node);
        this.inactiveNodes.remove(node);

        final Set<NodeInfo> nodes;
        switch (status) {
            case ACTIVE:
                nodes = this.activeNodes;
                break;

            case INACTIVE:
                nodes = this.inactiveNodes;
                break;

            case FAILURE:
            default:
                return;
        }

        nodes.add(node);
    }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeObjectArray("active", new ArrayList<>(this.activeNodes));
        serializer.writeObjectArray("inactive", new ArrayList<>(this.inactiveNodes));
    }
}
