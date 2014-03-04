package org.nem.peer.v2;

/**
 * The address of a local NEM node.
 */
public class LocalNodeAddress extends NodeAddress {

    /**
     * Creates a local node address.
     *
     * @param hostname The name of the local host.
     */
    public LocalNodeAddress(final String hostname) {
        super("http", hostname, 80);
    }
}
