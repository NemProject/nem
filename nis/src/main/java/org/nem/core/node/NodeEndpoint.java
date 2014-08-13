package org.nem.core.node;

import org.nem.core.serialization.*;

import java.net.*;
import java.util.*;

/**
 * The endpoint of a node in the NEM network.
 */
public class NodeEndpoint implements SerializableEntity {
	public final static ObjectDeserializer<NodeEndpoint> DESERIALIZER = deserializer -> new NodeEndpoint(deserializer);

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
	public URL getBaseUrl() {
		return this.url;
	}

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
		if (null == host || host.isEmpty()) {
			host = "localhost";
		}

		try {
			final InetAddress address = InetAddress.getByName(host);
			return address.getHostAddress();
		} catch (final UnknownHostException e) {
			throw new IllegalArgumentException("host is unknown");
		}
	}

	private URL createUrl() {
		try {
			return new URL(this.protocol, this.host, this.port, "/");
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("url is malformed");
		}
	}

	private Dictionary<NodeApiId, URL> createUrlMap() {
		try {
			final Dictionary<NodeApiId, URL> nodeApiToUrlMap = new Hashtable<>();
			nodeApiToUrlMap.put(NodeApiId.REST_BLOCK_AT, new URL(this.url, "block/at"));
			nodeApiToUrlMap.put(NodeApiId.REST_CHAIN_BLOCKS_AFTER, new URL(this.url, "chain/blocks-after"));
			nodeApiToUrlMap.put(NodeApiId.REST_CHAIN_HASHES_FROM, new URL(this.url, "chain/hashes-from"));
			nodeApiToUrlMap.put(NodeApiId.REST_CHAIN_LAST_BLOCK, new URL(this.url, "chain/last-block"));
			nodeApiToUrlMap.put(NodeApiId.REST_CHAIN_SCORE, new URL(this.url, "chain/score"));
			nodeApiToUrlMap.put(NodeApiId.REST_TRANSACTIONS_UNCONFIRMED, new URL(this.url, "transactions/unconfirmed"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_CAN_YOU_SEE_ME, new URL(this.url, "node/cysm"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_EXTENDED_INFO, new URL(this.url, "node/extended-info"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_INFO, new URL(this.url, "node/info"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_PEER_LIST, new URL(this.url, "node/peer-list/all"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_PEER_LIST_ACTIVE, new URL(this.url, "node/peer-list/active"));
			nodeApiToUrlMap.put(NodeApiId.REST_NODE_PING, new URL(this.url, "node/ping"));
			nodeApiToUrlMap.put(NodeApiId.REST_PUSH_BLOCK, new URL(this.url, "push/block"));
			nodeApiToUrlMap.put(NodeApiId.REST_PUSH_TRANSACTION, new URL(this.url, "push/transaction"));
			return nodeApiToUrlMap;
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("url is malformed");
		}
	}

	@Override
	public int hashCode() {
		return this.url.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof NodeEndpoint)) {
			return false;
		}

		final NodeEndpoint rhs = (NodeEndpoint)obj;
		return this.url.equals(rhs.url);
	}

	@Override
	public String toString() {
		return this.url.toString();
	}
}
