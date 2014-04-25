package org.nem.peer.trust.score;

import org.nem.core.serialization.*;
import org.nem.peer.node.Node;

import java.util.List;

/**
 * A Node and List<NodeExperiencePair> pair
 */
public class NodeExperiencesPair implements SerializableEntity {

	private final Node node;
	private final List<NodeExperiencePair> experiences;

	/**
	 * Creates a new node experiences pair.
	 *
	 * @param node        The node.
	 * @param experiences The node experiences.
	 */
	public NodeExperiencesPair(final Node node, final List<NodeExperiencePair> experiences) {
		this.node = node;
		this.experiences = experiences;
	}

	/**
	 * Deserializes a node experience pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeExperiencesPair(final Deserializer deserializer) {
		this.node = deserializer.readObject("node", new ObjectDeserializer<Node>() {
			@Override
			public Node deserialize(final Deserializer deserializer) {
				return new Node(deserializer);
			}
		});

		this.experiences = deserializer.readObjectArray("experiences", new ObjectDeserializer<NodeExperiencePair>() {
			@Override
			public NodeExperiencePair deserialize(final Deserializer deserializer) {
				return new NodeExperiencePair(deserializer);
			}
		});
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
	 * Gets the node experiences.
	 *
	 * @return The node experiences.
	 */
	public List<NodeExperiencePair> getExperiences() {
		return this.experiences;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("node", this.node);
		serializer.writeObjectArray("experiences", this.experiences);
	}
}
