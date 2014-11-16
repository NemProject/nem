package org.nem.core.node;

import org.nem.core.serialization.*;
import org.nem.core.utils.StringUtils;

import java.net.*;

/**
 * The endpoint of a node in the NEM network.
 */
public class NodeEndpoint implements SerializableEntity {
	public final static ObjectDeserializer<NodeEndpoint> DESERIALIZER = deserializer -> new NodeEndpoint(deserializer);

	private final String protocol;
	private final String host;
	private final String normalizedHost;
	private final int port;
	private final URL url;

	/**
	 * Creates a new node endpoint.
	 *
	 * @param protocol The protocol.
	 * @param host The host.
	 * @param port The port.
	 */
	public NodeEndpoint(final String protocol, final String host, final int port) {
		this.protocol = protocol;
		this.host = getNonNormalizedHost(host);
		this.normalizedHost = getNormalizedHost(this.host);
		this.port = port;
		this.url = this.createUrl();
	}

	/**
	 * Creates a new node endpoint given a host name.
	 *
	 * @param host The host.
	 * @return The node endpoint.
	 */
	public static NodeEndpoint fromHost(final String host) {
		return new NodeEndpoint("http", host, 7890);
	}

	/**
	 * Deserializes a node endpoint.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeEndpoint(final Deserializer deserializer) {
		this.protocol = deserializer.readString("protocol");
		this.host = getNonNormalizedHost(deserializer.readString("host"));
		this.normalizedHost = getNormalizedHost(this.host);
		this.port = deserializer.readInt("port");
		this.url = this.createUrl();
	}

	/**
	 * Gets the url.
	 *
	 * @return The url.
	 */
	public URL getBaseUrl() {
		return this.url;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeString("protocol", this.protocol);
		serializer.writeString("host", this.host);
		serializer.writeInt("port", this.port);
	}

	private static String getNonNormalizedHost(final String host) {
		return StringUtils.isNullOrWhitespace(host) ? "localhost" : host;
	}

	private static String getNormalizedHost(final String host) {
		try {
			final InetAddress address = InetAddress.getByName(host);
			return address.getHostAddress();
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException("host is unknown", e);
		}
	}

	private URL createUrl() {
		try {
			return new URL(this.protocol, this.host, this.port, "/");
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("url is malformed", e);
		}
	}

	@Override
	public int hashCode() {
		return this.protocol.hashCode() ^ this.normalizedHost.hashCode() ^ this.port;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeEndpoint)) {
			return false;
		}

		final NodeEndpoint rhs = (NodeEndpoint)obj;
		return this.protocol.equals(rhs.protocol)
				&& this.normalizedHost.equals(rhs.normalizedHost)
				&& this.port == rhs.port;
	}

	@Override
	public String toString() {
		return String.format("%s://%s:%d/", this.protocol, this.host, this.port);
	}
}
