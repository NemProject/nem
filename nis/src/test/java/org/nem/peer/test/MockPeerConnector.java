package org.nem.peer.test;

import org.nem.peer.*;

import java.net.URL;

/**
 * A mock PeerConnector implementation.
 */
public class MockPeerConnector implements PeerConnector {

    private int numGetInfoCalls;
    private int numGetKnownPeerCalls;

    private String getInfoErrorTrigger;
    private TriggerAction getInfoErrorTriggerAction;

    private String getKnownPeersErrorTrigger;
    private TriggerAction getKnownPeersErrorTriggerAction;

    private NodeCollection knownPeers = new NodeCollection();

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
        FATAL,

        /*
         * Returns a node with a different address.
         */
        CHANGE_ADDRESS
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
        this.getInfoErrorTrigger = trigger;
        this.getInfoErrorTriggerAction = action;
    }

    /**
     * Triggers a specific action in getKnownPeers.
     *
     * @param trigger The endpoint hostname that should cause the action.
     * @param action The action.
     */
    public void setGetKnownPeersError(final String trigger, final TriggerAction action) {
        this.getKnownPeersErrorTrigger = trigger;
        this.getKnownPeersErrorTriggerAction = action;
    }

    /**
     * Sets the NodeCollection that should be returned by getKnownPeers.
     *
     * @param nodes The NodeCollection that should be returned by getKnownPeers.
     */
    public void setKnownPeers(final NodeCollection nodes) {
        this.knownPeers = nodes;
    }

    @Override
    public Node getInfo(NodeEndpoint endpoint) {
        ++this.numGetInfoCalls;

        if (shouldTriggerAction(endpoint, this.getInfoErrorTrigger)) {
            triggerGeneralAction(this.getInfoErrorTriggerAction);
            switch (this.getInfoErrorTriggerAction) {
                case CHANGE_ADDRESS:
                    URL url = endpoint.getBaseUrl();
                    endpoint = new NodeEndpoint(url.getProtocol(), url.getHost(), url.getPort() + 1);
                    break;
            }
        }

        return new Node(endpoint, "P", "A");
    }

    @Override
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
        ++numGetKnownPeerCalls;

        if (shouldTriggerAction(endpoint, this.getKnownPeersErrorTrigger))
            triggerGeneralAction(this.getKnownPeersErrorTriggerAction);

        return this.knownPeers;
    }

    private static boolean shouldTriggerAction(final NodeEndpoint endpoint, final String trigger) {
        return endpoint.getBaseUrl().getHost().equals(trigger);
    }

    private static void triggerGeneralAction(final TriggerAction action) {
        switch (action) {
            case INACTIVE:
                throw new InactivePeerException("inactive peer");

            case FATAL:
                throw new FatalPeerException("fatal peer");
        }
    }
}