package org.nem.core.node;

import org.nem.core.serialization.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a collection of nodes.
 */
public class NodeCollection implements SerializableEntity {
	private static final ObjectDeserializer<Node> NODE_DESERIALIZER = obj -> new Node(obj);

	private final Map<NodeStatus, Set<Node>> statusNodesMap = new HashMap<NodeStatus, Set<Node>>() {
		{
			for (final NodeStatus value : NodeStatus.values()) {
				this.put(value, createSet());
			}
		}
	};

	Set<Node> penalizedNodesSnapshot;

	/**
	 * Creates a node collection.
	 */
	public NodeCollection() {
		this.penalizedNodesSnapshot = createSet();
	}

	/**
	 * Deserializes a node collection.
	 *
	 * @param deserializer The deserializer.
	 */
	public NodeCollection(final Deserializer deserializer) {
		for (final NodeStatus value : NodeStatus.values()) {
			final String key = value.toString().toLowerCase();
			this.statusNodesMap.get(value).addAll(deserializer.readObjectArray(key, NODE_DESERIALIZER));
		}
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
		return this.statusNodesMap.get(NodeStatus.ACTIVE);
	}

	/**
	 * Gets a collection of inactive nodes.
	 *
	 * @return A collection of inactive nodes.
	 */
	public Collection<Node> getInactiveNodes() {
		return this.statusNodesMap.get(NodeStatus.BUSY);
	}

	/**
	 * Gets a collection of nodes that have the specified status.
	 *
	 * @param status The status.
	 * @return All nodes with the specified status.
	 */
	public Collection<Node> getNodes(final NodeStatus status) {
		return this.statusNodesMap.get(status);
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
	 * Gets a value indicating whether or not the node is currently blacklisted.
	 *
	 * @param node The node.
	 * @return True if the node is blacklisted.
	 */
	public boolean isNodeBlacklisted(final Node node) {
		return this.statusNodesMap.get(NodeStatus.INACTIVE).contains(node)
				|| this.statusNodesMap.get(NodeStatus.FAILURE).contains(node);
	}

	/**
	 * Finds a node with a given endpoint.
	 *
	 * @param endpoint The endpoint.
	 * @return The matching node or null if not found.
	 */
	public Node findNodeByEndpoint(final NodeEndpoint endpoint) {
		for (final Node node : this.getAllNodes()) {
			if (node.getEndpoint().equals(endpoint)) {
				return node;
			}
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
			if (node.getIdentity().equals(identity)) {
				return node;
			}
		}

		return null;
	}

	/**
	 * Gets the status of the specified node.
	 *
	 * @param node The node.
	 * @return The node's status.
	 */
	public NodeStatus getNodeStatus(final Node node) {
		for (final Map.Entry<NodeStatus, Set<Node>> entry : this.statusNodesMap.entrySet()) {
			if (entry.getValue().contains(node)) {
				return entry.getKey();
			}
		}

		return NodeStatus.UNKNOWN;
	}

	/**
	 * Updates this collection to include the specified node with the associated status.
	 * The new node information will replace any previous node information.
	 *
	 * @param node The node.
	 * @param status The node status.
	 */
	public void update(final Node node, final NodeStatus status) {
		if (null == node) {
			throw new NullPointerException("node cannot be null");
		}

		for (final Map.Entry<NodeStatus, Set<Node>> entry : this.statusNodesMap.entrySet()) {
			entry.getValue().remove(node);
		}

		this.statusNodesMap.get(status).add(node);
		if (!this.isNodeBlacklisted(node)) {
			this.penalizedNodesSnapshot.remove(node);
		}
	}

	/**
	 * Takes a snapshot of all inactive nodes and drops the inactive nodes
	 * that have stayed inactive since the last time this function was called.
	 */
	public void pruneInactiveNodes() {
		this.penalizedNodesSnapshot.stream()
				.filter(node -> this.isNodeBlacklisted(node))
				.forEach(node -> this.update(node, NodeStatus.UNKNOWN)); // TODO: should really remove

		this.penalizedNodesSnapshot.clear();
	}

	@Override
	public void serialize(final Serializer serializer) {
		for (final NodeStatus value : NodeStatus.values()) {
			final String key = value.toString().toLowerCase();
			serializer.writeObjectArray(key, this.statusNodesMap.get(value));
		}
	}

	@Override
	public int hashCode() {
		return this.statusNodesMap.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof NodeCollection)) {
			return false;
		}

		final NodeCollection rhs = (NodeCollection)obj;
		return this.statusNodesMap.equals(rhs.statusNodesMap);
	}
}
