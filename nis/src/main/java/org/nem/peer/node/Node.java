package org.nem.peer.node;

import org.nem.core.serialization.*;

/**
 * Represents a node in the NEM network.
 * Each Node is uniquely identified by its endpoint.
 */
public class Node implements SerializableEntity {

	private NodeEndpoint endpoint;
	private final String platform;
	private final String version;
	private final String application;

	/**
	 * Creates a new node.
	 *
	 * @param endpoint    The endpoint.
	 * @param platform    The platform.
	 * @param application The application.
	 */
	public Node(
			final NodeEndpoint endpoint,
			final String platform,
			final String application,
			final String version) {
		this.endpoint = endpoint;
		this.platform = platform;
		this.application = application;
		this.version = version;
		this.ensureValidity();
	}

	/**
	 * Creates a new node given a host name.
	 *
	 * @param host The host.
	 * @return The node.
	 */
	public static Node fromHost(final String host) {
		return fromEndpoint(NodeEndpoint.fromHost(host));
	}

	/**
	 * Creates a new node given an endpoint.
	 *
	 * @param endpoint The endpoint.
	 * @return The node.
	 */
	public static Node fromEndpoint(final NodeEndpoint endpoint) {
		return new Node(endpoint, null, null, null);
	}

	/**
	 * Deserializes a node.
	 *
	 * @param deserializer The deserializer.
	 */
	public Node(final Deserializer deserializer) {
		this.endpoint = deserializer.readObject("endpoint", NodeEndpoint.DESERIALIZER);
		this.platform = deserializer.readString("platform");
		this.version = deserializer.readString("version");
		this.application = deserializer.readString("application");
		System.out.println(this.toString());
		this.ensureValidity();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("endpoint", this.endpoint);
		serializer.writeString("platform", this.platform);
		serializer.writeString("version", this.version);
		serializer.writeString("application", this.application);
	}

	//region Getters and Setters

	/**
	 * Gets the endpoint.
	 *
	 * @return The endpoint.
	 */
	public NodeEndpoint getEndpoint() {
		return this.endpoint;
	}

	/**
	 * Sets the endpoint.
	 *
	 * @param endpoint The endpoint.
	 */
	public void setEndpoint(final NodeEndpoint endpoint) {
		if (null == endpoint)
			throw new IllegalArgumentException("endpoint must be non-null");
	
		this.endpoint = endpoint;
	}

	/**
	 * Gets the platform.
	 *
	 * @return The platform.
	 */
	public String getPlatform() {
		return this.platform;
	}

	/**
	 * Gets the version.
	 *
	 * @return The version.
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Gets the application.
	 *
	 * @return The application.
	 */
	public String getApplication() {
		return this.application;
	}

	//endregion

	private void ensureValidity() {
		if (null == this.endpoint)
			throw new IllegalArgumentException("endpoint must be non-null");
	}

	@Override
	public int hashCode() {
		return this.endpoint.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Node))
			return false;

		Node rhs = (Node)obj;
		return this.endpoint.equals(rhs.endpoint);
	}

	@Override
	public String toString() {
		return String.format("Node %s", this.endpoint.getBaseUrl().getHost());
	}
}
