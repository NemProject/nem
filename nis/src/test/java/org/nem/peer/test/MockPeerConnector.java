package org.nem.peer.test;

import org.nem.peer.*;

/**
 * A mock PeerConnector implementation.
 */
public class MockPeerConnector implements PeerConnector {

    private int numGetInfoCalls;
    private int numGetKnownPeerCalls;

    private String getErrorTrigger;
    private TriggerAction getErrorTriggerAction;

    /**
     * Possible actions that can be triggered.
     */
    public enum TriggerAction {
        /**
         * No action.
         */
        NONE,

        /**
         * Throws an InactivePeerException.
         */
        INACTIVE,

        /**
         * Throws a FatalPeerException.
         */
        FATAL
    }

    /**
     * Gets the number of times getInfo was called.
     * @return The number of times getInfo was called.
     */
    public int getNumGetInfoCalls() { return this.numGetInfoCalls; }

    /**
     * Gets the number of times getKnownPeers was called.
     * @return The number of times getKnownPeers was called.
     */
    public int getNumGetKnownPeerCalls() { return this.numGetKnownPeerCalls; }

    /**
     * Triggers a specific action in getInfo.
     *
     * @param trigger The endpoint hostname that should cause the action.
     * @param action The action.
     */
    public void setGetInfoError(final String trigger, final TriggerAction action) {
        this.getErrorTrigger = trigger;
        this.getErrorTriggerAction = action;
    }

    @Override
    public Node getInfo(final NodeEndpoint endpoint) {
        ++this.numGetInfoCalls;

        if (endpoint.getBaseUrl().getHost().equals(this.getErrorTrigger)) {
            switch (this.getErrorTriggerAction) {
                case INACTIVE:
                    throw new InactivePeerException("inactive peer");

                case FATAL:
                    throw new FatalPeerException("fatal peer");
            }
        }

        return new Node(endpoint, "P", "A");
    }

    @Override
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
        ++numGetKnownPeerCalls;
        return null;
    }
}