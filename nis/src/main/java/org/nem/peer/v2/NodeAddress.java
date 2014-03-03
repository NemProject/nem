package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.net.*;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * The address of a node in the NEM network.
 */
public class NodeAddress implements SerializableEntity {

    private final String protocol;
    private final String address;
    private final int port;
    private final URL url;
    private final Dictionary<NodeApiId, URL> nodeApiToUrlMap;

    /**
     * Creates a new node address.
     *
     * @param address The address.
     * @param protocol The protocol.
     * @param port The port.
     */
    public NodeAddress(final String protocol, final String address, final int port) {
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.url = this.createUrl();
        this.nodeApiToUrlMap = this.createUrlMap();
    }

    /**
     * Deserializes a node address.
     *
     * @param deserializer The deserializer.
     */
    public NodeAddress(final Deserializer deserializer) {
        this.protocol = deserializer.readString("protocol");
        this.address = deserializer.readString("address");
        this.port = deserializer.readInt("port");
        this.url = this.createUrl();
        this.nodeApiToUrlMap = this.createUrlMap();
    }

    /**
     * Gets the url.
     *
     * @return The url.
     */
    public URL getBaseUrl() { return this.url; }

    /**
     * Gets the url for the specified API.
     *
     * @return The url for the specified API.
     */
    public URL getApiUrl(final NodeApiId id) {
        return this.nodeApiToUrlMap.get(id);
    }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeString("protocol", this.protocol);
        serializer.writeString("address", this.address);
        serializer.writeInt("port", this.port);
    }

    private URL createUrl() {
        try {
            URL url = new URL(this.protocol, this.address, this.port, "/");
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(this.address);
            return url;
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("url is malformed");
        } catch (UnknownHostException e) {
            throw new InvalidParameterException("host is unknown");
        }
    }

    private Dictionary<NodeApiId, URL> createUrlMap() {
        try {
            Dictionary<NodeApiId, URL> nodeApiToUrlMap = new Hashtable<>();
            nodeApiToUrlMap.put(NodeApiId.REST_NODE_INFO, new URL(this.url, "node/info"));
            nodeApiToUrlMap.put(NodeApiId.REST_ADD_PEER, new URL(this.url, "peer/new"));
            nodeApiToUrlMap.put(NodeApiId.REST_NODE_PEER_LIST, new URL(this.url, "node/peer-list"));
            nodeApiToUrlMap.put(NodeApiId.REST_CHAIN, new URL(this.url, "chain"));
            return nodeApiToUrlMap;
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("url is malformed");
        }
    }

    @Override
    public int hashCode() {
        return this.url.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NodeAddress))
            return false;

        NodeAddress rhs = (NodeAddress)obj;
        return this.url.equals(rhs.url);
    }
}
