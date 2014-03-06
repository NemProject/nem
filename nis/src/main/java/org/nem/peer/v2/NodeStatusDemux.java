package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * Demultiplexes a node collection into separate node info collections based on node status.
 */
public class NodeStatusDemux implements SerializableEntity {

    private static ObjectDeserializer<NodeInfo> NODE_INFO_DESERIALIZER = new ObjectDeserializer<NodeInfo>() {
        @Override
        public NodeInfo deserialize(final Deserializer deserializer) {
            return new NodeInfo(deserializer);
        }
    };

    final List<NodeInfo> activeNodes;
    final List<NodeInfo> inactiveNodes;

    /**
     * Demultiplexes an collection of nodes.
     *
     * @param nodes The collection to demux.
     */
    public NodeStatusDemux(final Iterable<Node> nodes) {
        this.activeNodes = new ArrayList<>();
        this.inactiveNodes = new ArrayList<>();

        for (final Node node : nodes) {
            final List<NodeInfo> nodeList;
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
        this.activeNodes = deserializer.readObjectArray("active", NODE_INFO_DESERIALIZER);
        this.inactiveNodes = deserializer.readObjectArray("inactive", NODE_INFO_DESERIALIZER);
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

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeObjectArray("active", this.activeNodes);
        serializer.writeObjectArray("inactive", this.inactiveNodes);
    }
}
