package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A cluster represents a set of nodes that share certain similarity (i.e. are closely connected).
 */
public class Cluster {
	/**
	 * The unique id of the cluster.
	 */
	private final ClusterId id;

	/**
	 * The set of member ids of nodes in the cluster.
	 */
	private final Collection<NodeId> memberIds;

	/**
	 * Creates a new cluster.
	 *
	 * @param id The unique id of the cluster's pivot.
	 * @param memberIds The ids of nodes in this cluster.
	 */
	public Cluster(final ClusterId id, final Collection<NodeId> memberIds) {
		if (null == memberIds) {
			throw new IllegalArgumentException("memberIds cannot be null");
		}

		this.id = id;
		this.memberIds = new HashSet<>(memberIds);
	}

	/**
	 * Creates a new empty cluster.
	 *
	 * @param id The unique id.
	 */
	public Cluster(final ClusterId id) {
		this(id, new ArrayList<>());
	}

	/**
	 * Creates a new cluster containing a single node (the cluster id is derived from the node id).
	 *
	 * @param id The node id.
	 */
	public Cluster(final NodeId id) {
		this(new ClusterId(id.getRaw()));
		this.memberIds.add(id);
	}

	/**
	 * Gets the id of the cluster.
	 *
	 * @return The cluster id.
	 */
	public ClusterId getId() {
		return this.id;
	}

	/**
	 * Gets the ids of all members.
	 *
	 * @return The member ids.
	 */
	public Collection<NodeId> getMemberIds() {
		return this.memberIds;
	}

	/**
	 * Adds a new member to the cluster
	 *
	 * @param memberId The id of the new member
	 */
	public void add(final NodeId memberId) {
		this.memberIds.add(memberId);
	}

	/**
	 * Merges the members of the given cluster into the current (this) cluster.
	 *
	 * @param cluster cluster to merge into this cluster
	 */
	public void merge(final Cluster cluster) {
		this.memberIds.addAll(cluster.getMemberIds());
	}

	// region hashCode / equals

	@Override
	public int hashCode() {
		return this.memberIds.stream().collect(Collectors.summingInt(NodeId::getRaw));
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof Cluster)) {
			return false;
		}

		// The id is not relevant for a cluster
		final Cluster rhs = (Cluster) obj;
		return this.memberIds.equals(rhs.memberIds);
	}

	// endregion

	/**
	 * Gets the total number of nodes in this cluster.
	 *
	 * @return Total number of nodes in this cluster.
	 */
	public int size() {
		return this.memberIds.size();
	}

	@Override
	public String toString() {
		return String.format("Cluster Id: %s; Member Ids: %s", this.id, this.memberIds);
	}
}
