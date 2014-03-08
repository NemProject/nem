package org.nem.peer;

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
    public Node getInfo(final NodeEndpoint endpoint);

    /**
     * Requests information about all known peers from the specified node.
     *
     * @param endpoint The endpoint.
     * @return A collection of all known peers.
     */
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint);
}