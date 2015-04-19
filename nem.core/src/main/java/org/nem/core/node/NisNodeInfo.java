package org.nem.core.node;

import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.serialization.*;

/**
 * Information about a node that is returned by NIS.
 */
public class NisNodeInfo implements SerializableEntity {
	private final Node node;
	private final ApplicationMetaData appMetaData;

	/**
	 * Creates a new node info.
	 *
	 * @param node The node.
	 * @param appMetaData The application meta data.
	 */
	public NisNodeInfo(final Node node, final ApplicationMetaData appMetaData) {
		this.node = node;
		this.appMetaData = appMetaData;
	}

	/**
	 * Deserializes a node info.
	 *
	 * @param deserializer The deserializer.
	 */
	public NisNodeInfo(final Deserializer deserializer) {
		this.node = deserializer.readObject("node", Node::new);
		this.appMetaData = deserializer.readObject("nisInfo", ApplicationMetaData::new);
	}

	/**
	 * Gets the node.
	 *
	 * @return The node.
	 */
	public Node getNode() {
		return this.node;
	}

	/**
	 * Gets the application meta data.
	 *
	 * @return The application meta data.
	 */
	public ApplicationMetaData getAppMetaData() {
		return this.appMetaData;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("node", this.node);
		serializer.writeObject("nisInfo", this.appMetaData);
	}

	@Override
	public int hashCode() {
		return this.node.hashCode() ^ this.appMetaData.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NisNodeInfo)) {
			return false;
		}

		final NisNodeInfo rhs = (NisNodeInfo)obj;
		return this.node.equals(rhs.node) &&
				this.appMetaData.equals(rhs.appMetaData);
	}
}
