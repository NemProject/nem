package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.NodeId;

import java.util.*;
import java.util.stream.Collectors;

// TODO-CR [08062014][J-M]: i will need to look closer at this too

/**
 * Represents a neighborhood of nodes.
 */
public class Neighborhood {
	private final NeighborhoodRepository repository;
	private final SimilarityStrategy similarityStrategy;
	private final Map<NodeId, Community> communityCache;

	/**
	 * Creates a new neighborhood.
	 *
	 * @param repository The neighborhood repository.
	 * @param similarityStrategy The similarity strategy.
	 */
	public Neighborhood(final NeighborhoodRepository repository, final SimilarityStrategy similarityStrategy) {
		this.repository = repository;
		this.similarityStrategy = similarityStrategy;
		this.communityCache = new HashMap<>();
	}

	/**
	 * Creates a community for the specified node id.
	 *
	 * @param nodeId The node id.
	 * @return The community.
	 */
	private Community createCommunity(final NodeId nodeId) {
		final NodeNeighbors epsilonNeighbors = new NodeNeighbors();
		final NodeNeighbors nonEpsilonNeighbors = new NodeNeighbors();

		for (final NodeId neighborId : this.repository.getNeighbors(nodeId)) { // this is guaranteed to traverse in increasing order
			final double similarity = nodeId.equals(neighborId) ? 1.0 : this.similarityStrategy.calculateSimilarity(nodeId, neighborId);
			(similarity > GraphConstants.EPSILON ? epsilonNeighbors : nonEpsilonNeighbors).addNeighbor(neighborId);
		}

		return new Community(nodeId, epsilonNeighbors, nonEpsilonNeighbors);
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

	/**
	 * Gets all neighboring communities around the given node.
	 *
	 * @param nodeId The node id.
	 * @return A collection of all neighboring communities.
	 */
	public Collection<Community> getNeighboringCommunities(final NodeId nodeId) {
		return this.repository.getNeighbors(nodeId)
				.toList()
				.stream()
				.map(this::getCommunity)
				.collect(Collectors.toList());
	}

	/**
	 * Gets all communities two hops away from the given node.
	 *
	 * @param nodeId The node id.
	 * @return A collection of all communities two hops away.
	 */
	public Collection<Community> getTwoHopAwayCommunities(final NodeId nodeId) {
		final HashSet<Community> twoHopCommunities = new HashSet<>();

		final NodeNeighbors neighbors = this.repository.getNeighbors(nodeId);
		for (final NodeId neighborId : neighbors) {
			final NodeNeighbors twoHopNeighbors = this.repository.getNeighbors(neighborId);
			for (final NodeId twoHopNeighborId : twoHopNeighbors) {
				if (twoHopNeighborId.equals(nodeId) || neighbors.contains(twoHopNeighborId)) {
					continue;
				}

				final Community twoHopCommunity = getCommunity(twoHopNeighborId);
				twoHopCommunities.add(twoHopCommunity);
			}
		}

		return twoHopCommunities;
	}

	/**
	 * Gets all communities two hops away from the given node.
	 *
	 * @param nodeId The node id.
	 * @return The two hops away node neighbors.
	 */
	public NodeNeighbors getTwoHopAwayNeighbors(final NodeId nodeId) {
		final NodeNeighbors neighbors = this.getCommunity(nodeId).getSimilarNeighbors(); //from the paper, the definition of node w is that it is an epsilon neighbor of u

		if (neighbors.size() < 2) {
			return new NodeNeighbors();
		}

		final NodeNeighbors[] twoHopAwayNeighborsArray = new NodeNeighbors[neighbors.size() - 1];

		int index = 0;
		for (final NodeId neighborId : neighbors) {
			if (nodeId.equals(neighborId)) {
				continue;
			}
			twoHopAwayNeighborsArray[index++] = this.repository.getNeighbors(neighborId);
		}

		return new NodeNeighbors().union(twoHopAwayNeighborsArray);
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
