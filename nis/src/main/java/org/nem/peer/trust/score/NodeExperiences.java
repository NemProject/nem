package org.nem.peer.trust.score;

import org.nem.core.math.*;
import org.nem.core.node.Node;
import org.nem.core.utils.AbstractTwoLevelMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains experiences for a set of nodes.
 */
public class NodeExperiences {

	private final AbstractTwoLevelMap<Node, NodeExperience> nodeExperiences = new AbstractTwoLevelMap<Node, NodeExperience>() {

		@Override
		protected NodeExperience createValue() {
			return new NodeExperience();
		}
	};

	/**
	 * Gets the NodeExperience source has with peer.
	 *
	 * @param source The node reporting experience.
	 * @param peer The node being reported about.
	 * @return The experience source has with peer.
	 */
	public NodeExperience getNodeExperience(final Node source, final Node peer) {
		return this.nodeExperiences.getItem(source, peer);
	}

	private Map<Node, NodeExperience> getNodeExperiencesInternal(final Node source) {
		return this.nodeExperiences.getItems(source);
	}

	/**
	 * Gets a shared experience matrix.
	 * Matrix(r, c) contains 1 if node and node(r) have both interacted with node(c).
	 *
	 * @param node The node.
	 * @param nodes The other nodes.
	 * @return The shared experiences matrix.
	 */
	public Matrix getSharedExperienceMatrix(final Node node, final Node[] nodes) {
		final Matrix matrix = new DenseMatrix(nodes.length, nodes.length);
		for (int i = 0; i < nodes.length; ++i) {
			// skip the node (since the node will not have interacted with itself)
			if (node.equals(nodes[i])) {
				continue;
			}

			for (int j = 0; j < nodes.length; ++j) {
				if (nodes[i].equals(nodes[j])) {
					continue;
				}

				final NodeExperience experience1 = this.getNodeExperience(node, nodes[j]);
				final NodeExperience experience2 = this.getNodeExperience(nodes[i], nodes[j]);
				if (experience1.totalCalls() > 0 && experience2.totalCalls() > 0) {
					matrix.setAt(i, j, 1.0);
				}
			}
		}

		return matrix;
	}

	/**
	 * Gets all experience information for node.
	 *
	 * @param node The node for which to get experience information.
	 * @return All experience information for node.
	 */
	public List<NodeExperiencePair> getNodeExperiences(final Node node) {
		final List<NodeExperiencePair> pairs = new ArrayList<>();
		final Map<Node, NodeExperience> experiences = this.getNodeExperiencesInternal(node);
		pairs.addAll(
				experiences.entrySet().stream()
						.map(entry -> new NodeExperiencePair(entry.getKey(), entry.getValue()))
						.collect(Collectors.toList()));

		return pairs;
	}

	/**
	 * Sets experience information for node.
	 *
	 * @param node The node for which to set experience information.
	 * @param pairs The experience information for node.
	 */
	public void setNodeExperiences(final Node node, final List<NodeExperiencePair> pairs) {
		final Map<Node, NodeExperience> experiences = this.getNodeExperiencesInternal(node);
		for (final NodeExperiencePair pair : pairs) {
			experiences.put(pair.getNode(), pair.getExperience());
		}
	}
}