package org.nem.peer.node;

import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.serialization.Deserializer;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.Serializer;

/**
 * Information about a node that is returned by NIS.
 */
public class NisNodeInfo implements SerializableEntity {
	private Node node;
	private ApplicationMetaData appMetaData;

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
}
