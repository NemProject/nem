package org.nem.peer.node;

import org.nem.core.serialization.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a collection of nodes.
 */
public class NodeCollection implements SerializableEntity {

	private static ObjectDeserializer<Node> NODE_DESERIALIZER = Node::new;

	final Set<Node> activeNodes;
	final Set<Node> inactiveNodes;
	Set<Node> inactiveNodesSnapshot;

	/**
	 * Creates a node collection.
	 */
	public NodeCollection() {
		this.activeNodes = createSet();
		this.inactiveNodes = createSet();
		this.inactiveNodesSnapshot = createSet();
	}

	/**
	 * Deserializes a node collection.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeCollection(final Deserializer deserializer) {
		this.activeNodes = createSet();
		this.activeNodes.addAll(deserializer.readObjectArray("active", NODE_DESERIALIZER));
		this.inactiveNodes = createSet();
		this.inactiveNodes.addAll(deserializer.readObjectArray("inactive", NODE_DESERIALIZER));
	}

	private static Set<Node> createSet() {
		return Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	/**
	 * Gets a collection of active nodes.
	 *
	 * @return A collection of active nodes.
	 */
	public Collection<Node> getActiveNodes() {
		return this.activeNodes;
	}

	/**
	 * Gets a collection of inactive nodes.
	 *
	 * @return A collection of inactive nodes.
	 */
	public Collection<Node> getInactiveNodes() {
		return this.inactiveNodes;
	}

	/**
	 * Gets a collection of all nodes (both active and inactive).
	 *
	 * @return A collection of all nodes.
	 */
	public Collection<Node> getAllNodes() {
		final List<Node> allNodes = new ArrayList<>();
		allNodes.addAll(this.getActiveNodes());
		allNodes.addAll(this.getInactiveNodes());
		return allNodes;
	}

	/**
	 * Finds a node with a given endpoint.
	 *
	 * @param endpoint The endpoint.
	 * @return The matching node or null if not found.
	 */
	public Node findNodeByEndpoint(final NodeEndpoint endpoint) {
		for (final Node node : this.getAllNodes()) {
			if (node.getEndpoint().equals(endpoint))
				return node;
		}

		return null;
	}

	/**
	 * Finds a node with a given identity.
	 *
	 * @param identity The identity.
	 * @return The matching node or null if not found.
	 */
	public Node findNodeByIdentity(final NodeIdentity identity) {
		for (final Node node : this.getAllNodes()) {
			if (node.getIdentity().equals(identity))
				return node;
		}

		return null;
	}

	/**
	 * Gets the status of the specified node.
	 *
	 * @param node The node.
	 *
	 * @return The node's status.
	 */
	public NodeStatus getNodeStatus(final Node node) {
		if (this.activeNodes.contains(node))
			return NodeStatus.ACTIVE;

		if (this.inactiveNodes.contains(node))
			return NodeStatus.INACTIVE;

		return NodeStatus.FAILURE;
	}

	/**
	 * Updates this collection to include the specified node with the associated status.
	 * The new node information will replace any previous node information.
	 *
	 * @param node   The node.
	 * @param status The node status.
	 */
	public void update(final Node node, final NodeStatus status) {
		if (null == node)
			throw new NullPointerException("node cannot be null");

		this.activeNodes.remove(node);
		this.inactiveNodes.remove(node);

		final Set<Node> nodes;
		switch (status) {
			case ACTIVE:
				nodes = this.activeNodes;
				this.inactiveNodesSnapshot.remove(node);
				break;

			case INACTIVE:
				nodes = this.inactiveNodes;
				break;

			case FAILURE:
			default:
				return;
		}

		nodes.add(node);
	}

	/**
	 * Takes a snapshot of all inactive nodes and drops the inactive nodes
	 * that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		this.getInactiveNodes().stream()
				.filter(this.inactiveNodesSnapshot::contains)
				.forEach(node -> this.update(node, NodeStatus.FAILURE));

		this.inactiveNodesSnapshot = createSet();
		this.inactiveNodesSnapshot.addAll(this.getInactiveNodes());
	}

	@Override
	public void serialize(final Serializer serializer) {
		serializer.writeObjectArray("active", new ArrayList<>(this.activeNodes));
		serializer.writeObjectArray("inactive", new ArrayList<>(this.inactiveNodes));
	}
}
