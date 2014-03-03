package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.net.*;
import java.security.InvalidParameterException;

/**
 * The address of a node in the NEM network.
 */
public class NodeAddress implements SerializableEntity {

    private final String protocol;
    private final String address;
    private final int port;
    private final URL url;

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
    }

    /**
     * Gets the url.
     *
     * @return The url.
     */
    public URL getBaseUrl() { return this.url; }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeString("protocol", this.protocol);
        serializer.writeString("address", this.address);
        serializer.writeInt("port", this.port);
    }

    private URL createUrl() {
        try {
            URL url = new URL(protocol, address, port, "/");
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(address);
            return url;
        } catch (MalformedURLException e) {
            throw new InvalidParameterException("url is malformed");
        } catch (UnknownHostException e) {
            throw new InvalidParameterException("host is unknown");
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
