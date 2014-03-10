package org.nem.peer;

import org.nem.core.model.Transaction;

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

	/**
	 * Not sure if this is proper place for this
	 */
	public void pushTransaction(final NodeEndpoint endpoint, final Transaction transaction);
}