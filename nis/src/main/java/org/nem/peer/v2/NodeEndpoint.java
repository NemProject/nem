package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.net.*;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * The endpoint of a node in the NEM network.
 */
public class NodeEndpoint implements SerializableEntity {

    public static ObjectDeserializer<NodeEndpoint> DESERIALIZER = new ObjectDeserializer<NodeEndpoint>() {
        @Override
        public NodeEndpoint deserialize(final Deserializer deserializer) {
            return new NodeEndpoint(deserializer);
        }
    };

    private final String protocol;
    private final String host;
    private final int port;
    private final URL url;
    private final Dictionary<NodeApiId, URL> nodeApiToUrlMap;

    /**
     * Creates a new node endpoint.
     *
     * @param protocol The protocol.
     * @param host The host.
     * @param port The port.
     */
    public NodeEndpoint(final String protocol, final String host, final int port) {
        this.protocol = protocol;
        this.host = normalizeHost(host);
        this.port = port;
        this.url = this.createUrl();
        this.nodeApiToUrlMap = this.createUrlMap();
    }

    /**
     * Deserializes a node endpoint.
     *
     * @param deserializer The deserializer.
     */
    public NodeEndpoint(final Deserializer deserializer) {
        this.protocol = deserializer.readString("protocol");
        this.host = normalizeHost(deserializer.readString("host"));
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
        serializer.writeString("host", this.host);
        serializer.writeInt("port", this.port);
    }

    private static String normalizeHost(String host) {
        if (null == host || 0 == host.length())
            host = "localhost";

        try {
            final InetAddress address = InetAddress.getByName(host);
            return address.getHostAddress();
        }
        catch (UnknownHostException e) {
            throw new InvalidParameterException("host is unknown");
        }
    }

    private URL createUrl() {
        try {
            return new URL(this.protocol, this.host, this.port, "/");
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("url is malformed");
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
        if (obj == null || !(obj instanceof NodeEndpoint))
            return false;

        NodeEndpoint rhs = (NodeEndpoint)obj;
        return this.url.equals(rhs.url);
    }
}
