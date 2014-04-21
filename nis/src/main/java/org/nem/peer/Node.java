package org.nem.peer;

import org.nem.core.connect.NodeEndpoint;
import org.nem.core.serialization.*;

/**
 * Represents a node in the NEM network.
 * Each Node is uniquely identified by its endpoint.
 */
public class Node implements SerializableEntity {

	private final static int DEFAULT_VERSION = 2;
	private final static String DEFAULT_PLATFORM = "PC";

	private final NodeEndpoint endpoint;
	private final String platform;
	private final Integer version;
	private final String application;

	/**
	 * Creates a new node.
	 *
	 * @param endpoint    The endpoint.
	 * @param platform    The platform.
	 * @param application The application.
	 */
	public Node(final NodeEndpoint endpoint, final String platform, final String application) {
		this.endpoint = endpoint;
		this.platform = null == platform ? DEFAULT_PLATFORM : platform;
		this.application = application;
		this.version = DEFAULT_VERSION;
		this.ensureValidity();
	}

	/**
	 * Deserializes a node.
	 *
	 * @param deserializer The deserializer.
	 */
	public Node(final Deserializer deserializer) {
		this.endpoint = deserializer.readObject("endpoint", NodeEndpoint.DESERIALIZER);

		this.platform = deserializer.readString("platform");

		final Integer version = deserializer.readInt("version");
		this.version = null == version ? DEFAULT_VERSION : version;

		this.application = deserializer.readString("application");
		this.ensureValidity();
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("endpoint", this.endpoint);
		serializer.writeString("platform", this.platform);
		serializer.writeInt("version", this.version);
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
	public int getVersion() {
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
