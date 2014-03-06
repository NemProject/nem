package org.nem.peer.v2;

import org.nem.core.serialization.*;
import org.nem.peer.*;
import org.nem.peer.v2.NodeStatus;

import java.util.*;

/**
 * Demultiplexes a node collection into separate collections based on node state.
 */
public class NodeStateDemux implements SerializableEntity {

    final List<Node> activeNodes;
    final List<Node> inactiveNodes;

    /**
     * Demultiplexes an collection of nodes.
     *
     * @param nodes The collection to demux.
     */
    public NodeStateDemux(final Iterable<Node> nodes) {
        this.activeNodes = new ArrayList<>();
        this.inactiveNodes = new ArrayList<>();

        for (final Node node : nodes) {
            final List<Node> nodeList = NodeStatus.ACTIVE == node.getStatus() ? this.activeNodes : this.inactiveNodes;
            nodeList.add(node);
        }
    }

    /**
     * Deserializes demultiplexed nodes.
     *
     * @param nodes The collection to demux.
     */
    public NodeStateDemux(final Iterable<org.nem.peer.Node> nodes) {

    /**
     * Gets a collection of active nodes.
     *
     * @return A collection of active nodes.
     */
    public Iterable<org.nem.peer.Node> getActiveNodes() { return this.activeNodes; }

    /**
     * Gets a collection of inactive nodes.
     *
     * @return A collection of active nodes.
     */
    public Iterable<org.nem.peer.Node> getInactiveNodes() { return this.activeNodes; }


    @Override
    public void serialize(Serializer serializer) {

    }
}
