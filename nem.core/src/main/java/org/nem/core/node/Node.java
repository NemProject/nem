package org.nem.core.node;

import org.nem.core.serialization.*;

/**
 * Represents a node in the NEM network.
 * Each Node is uniquely identified by its endpoint.
 */
public class Node implements SerializableEntity {
	private final NodeIdentity identity;
	private NodeEndpoint endpoint;
	private NodeMetaData metaData;

	/**
	 * Creates a new node without meta data.
	 *
	 * @param identity The identity.
	 * @param endpoint The endpoint.
	 */
	public Node(final NodeIdentity identity, final NodeEndpoint endpoint) {
		this(identity, endpoint, null);
	}

	/**
	 * Creates a new node with meta data.
	 *
	 * @param identity The identity.
	 * @param endpoint The endpoint.
	 * @param metaData The meta data.
	 */
	public Node(
			final NodeIdentity identity,
			final NodeEndpoint endpoint,
			final NodeMetaData metaData) {
		this.identity = identity;
		this.setEndpoint(endpoint);
		this.setMetaData(getMetaData(metaData));
		this.ensureValidity();
	}

	/**
	 * Deserializes a node.
	 *
	 * @param deserializer The deserializer.
	 */
	public Node(final Deserializer deserializer) {
		this.identity = deserializer.readObject("identity", NodeIdentity::deserializeWithPublicKey);
		this.setEndpoint(deserializer.readObject("endpoint", NodeEndpoint::new));
		this.setMetaData(getMetaData(deserializer.readOptionalObject("metaData", NodeMetaData::new)));
		this.ensureValidity();
	}

	private static NodeMetaData getMetaData(final NodeMetaData metaData) {
		return null != metaData ? metaData : new NodeMetaData(null, null);
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("identity", this.identity);
		serializer.writeObject("endpoint", this.endpoint);
		serializer.writeObject("metaData", this.metaData);
	}

	//region Getters and Setters

	/**
	 * Gets the identity.
	 *
	 * @return The identity.
	 */
	public NodeIdentity getIdentity() {
		return this.identity;
	}

	/**
	 * Gets the endpoint.
	 *
	 * @return The endpoint.
	 */
	public NodeEndpoint getEndpoint() {
		return this.endpoint;
	}

	/**
	 * Gets the meta data.
	 *
	 * @return The meta data.
	 */
	public NodeMetaData getMetaData() {
		return this.metaData;
	}

	/**
	 * Sets the endpoint.
	 *
	 * @param endpoint The endpoint.
	 */
	public void setEndpoint(final NodeEndpoint endpoint) {
		if (null == endpoint) {
			throw new IllegalArgumentException("endpoint must be non-null");
		}

		this.endpoint = endpoint;
	}

	/**
	 * Sets the meta data.
	 *
	 * @param metaData The meta data.
	 */
	public void setMetaData(final NodeMetaData metaData) {
		if (null == metaData) {
			throw new IllegalArgumentException("metaData must be non-null");
		}

		this.metaData = metaData;
	}

	//endregion

	private void ensureValidity() {
		if (null == this.identity) {
			throw new IllegalArgumentException("identity must be non-null");
		}
	}

	@Override
	public int hashCode() {
		return this.identity.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Node)) {
			return false;
		}

		final Node rhs = (Node)obj;
		return this.identity.equals(rhs.identity);
	}

	@Override
	public String toString() {
		return String.format("Node [%s] @ [%s]", this.identity, this.endpoint.getBaseUrl().getHost());
	}
}
