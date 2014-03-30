package org.nem.peer.trust;

/**
 * Interface for selecting a node.
 */
public interface NodeSelector {

    /**
     * Selects a node.
     *
     * @return Information about the selected node.
     */
    public NodeInfo selectNode();
}
