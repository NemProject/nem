package org.nem.peer.v2;

/**
 * A interface that is used to request information from nodes.
 */
public interface PeerConnector {

    /**
     * Gets information about the specified node.
     *
     * @param endpoint The endpoint.
     * @return Information about the specified node.
     */
    public NodeInfo getInfo(final NodeEndpoint endpoint);

    /**
     * Requests information about all known peers from the specified node.
     *
     * @param endpoint The endpoint.
     * @return A demultiplexed list of peers.
     */
    public NodeStatusDemux getKnownPeers(final NodeEndpoint endpoint);
}