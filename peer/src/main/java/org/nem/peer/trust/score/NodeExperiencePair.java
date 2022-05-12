package org.nem.peer.trust.score;

import org.nem.core.node.Node;
import org.nem.core.serialization.*;

/**
 * A Node and NodeExperience pair.
 */
public class NodeExperiencePair implements SerializableEntity {
	private final Node node;
	private final NodeExperience experience;

	/**
	 * Creates a new node experience pair.
	 *
	 * @param node The node.
	 * @param experience The node experience.
	 */
	public NodeExperiencePair(final Node node, final NodeExperience experience) {
		this.node = node;
		this.experience = experience;
	}

	/**
	 * Deserializes a node experience pair.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeExperiencePair(final Deserializer deserializer) {
		this.node = deserializer.readObject("node", Node::new);
		this.experience = deserializer.readObject("experience", NodeExperience::new);
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
	 * Gets the node experience.
	 *
	 * @return The node experience.
	 */
	public NodeExperience getExperience() {
		return this.experience;
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObject("node", this.node);
		serializer.writeObject("experience", this.experience);
	}

	@Override
	public int hashCode() {
		return this.node.hashCode() ^ this.experience.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeExperiencePair)) {
			return false;
		}

		final NodeExperiencePair rhs = (NodeExperiencePair) obj;
		return this.node.equals(rhs.node) && this.experience.equals(rhs.experience);
	}

	@Override
	public String toString() {
		return String.format("[%s] @ [%s]", this.getExperience(), this.getNode());
	}
}
