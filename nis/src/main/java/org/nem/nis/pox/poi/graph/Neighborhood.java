package org.nem.nis.pox.poi.graph;

import org.nem.core.model.primitive.NodeId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a neighborhood of nodes.
 */
public class Neighborhood {
	private final NeighborhoodRepository repository;
	private final SimilarityStrategy similarityStrategy;
	private final int mu;
	private final double epsilon;
	private final Map<NodeId, Community> communityCache;

	/**
	 * Creates a new neighborhood.
	 *
	 * @param repository The neighborhood repository.
	 * @param similarityStrategy The similarity strategy.
	 * @param mu The minimum number of neighbors with high structural similarity that a core community must have.
	 * @param epsilon The structural similarity threshold that will cause nodes to be considered highly similar.
	 */
	public Neighborhood(final NeighborhoodRepository repository, final SimilarityStrategy similarityStrategy, final int mu,
			final double epsilon) {
		this.repository = repository;
		this.similarityStrategy = similarityStrategy;
		this.mu = mu;
		this.epsilon = epsilon;
		this.communityCache = new HashMap<>();
	}

	/**
	 * Gets the community for the given node.
	 *
	 * @param nodeId The id of the pivot node that the community should be centered around.
	 * @return A community centered around nodeId.
	 */
	public Community getCommunity(final NodeId nodeId) {
		Community community = this.communityCache.getOrDefault(nodeId, null);
		if (null == community) {
			community = this.createCommunity(nodeId);
			this.communityCache.put(nodeId, community);
		}

		return community;
	}

	private Community createCommunity(final NodeId nodeId) {
		final NodeNeighbors epsilonNeighbors = new NodeNeighbors();
		final NodeNeighbors nonEpsilonNeighbors = new NodeNeighbors();

		// this is guaranteed to traverse in increasing order
		for (final NodeId neighborId : this.repository.getNeighbors(nodeId)) {
			final double similarity = nodeId.equals(neighborId) ? 1.0 : this.similarityStrategy.calculateSimilarity(nodeId, neighborId);
			(similarity > this.epsilon ? epsilonNeighbors : nonEpsilonNeighbors).addNeighbor(neighborId);
		}

		return new Community(nodeId, epsilonNeighbors, nonEpsilonNeighbors, this.mu);
	}

	/**
	 * Gets all neighboring communities around the given node.
	 *
	 * @param nodeId The node id.
	 * @return A collection of all neighboring communities.
	 */
	public Collection<Community> getNeighboringCommunities(final NodeId nodeId) {
		return this.repository.getNeighbors(nodeId).toList().stream().map(this::getCommunity).collect(Collectors.toList());
	}

	/**
	 * Gets all communities two hops away from the given node.
	 *
	 * @param nodeId The node id.
	 * @return The two hops away node neighbors.
	 */
	public NodeNeighbors getTwoHopAwayNeighbors(final NodeId nodeId) {
		// from the paper, the definition of node w is that it is an epsilon neighbor of u
		final NodeNeighbors neighbors = this.getCommunity(nodeId).getSimilarNeighbors();
		final NodeNeighbors[] twoHopAwayNeighborsArray = new NodeNeighbors[neighbors.size()];

		int index = 0;
		for (final NodeId neighborId : neighbors) {
			// ignore one-hop neighbors
			twoHopAwayNeighborsArray[index++] = nodeId.equals(neighborId) ? new NodeNeighbors() : this.repository.getNeighbors(neighborId);
		}

		// don't report the starting node as two-hops away
		return NodeNeighbors.union(twoHopAwayNeighborsArray).difference(new NodeNeighbors(nodeId));
	}

	/**
	 * Gets the size of the neighborhood.
	 *
	 * @return The size of the neighborhood.
	 */
	public int size() {
		return this.repository.getLogicalSize();
	}
}
