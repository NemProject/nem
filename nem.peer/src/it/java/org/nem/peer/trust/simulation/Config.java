package org.nem.peer.trust.simulation;

import org.nem.core.node.*;
import org.nem.core.test.NodeUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Network simulator configuration.
 */
public class Config {

	private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

	/**
	 * Number of node attributes in the node configuration file.
	 */
	private static final int NODE_ATTRIBUTE_COUNT = 7;

	private final List<Entry> entries;
	private final NodeCollection nodes;
	private final Node localNode;

	public Config(final List<Entry> entries) {
		this.entries = entries;

		int numPreTrustedNodes = 0;
		for (final Entry entry : this.entries) {
			numPreTrustedNodes += entry.isPreTrusted() ? 1 : 0;
		}

		this.nodes = new NodeCollection();
		for (final Entry entry : this.entries) {
			this.nodes.update(entry.getNode(), NodeStatus.ACTIVE);
		}

		final Entry localNodeEntry = new Entry("127.0.0.1", false, false, 1.0, 1.0, false, false);
		this.entries.add(localNodeEntry);
		this.localNode = localNodeEntry.getNode();

		LOGGER.info(String.format("Found %d nodes (%d pre-trusted)", this.entries.size(), numPreTrustedNodes));
	}

	public List<Entry> getEntries() {
		return this.entries;
	}

	public NodeCollection getNodes() {
		return this.nodes;
	}

	public Node getLocalNode() {
		return this.localNode;
	}

	public Set<Node> getPreTrustedNodes() {
		return this.entries.stream().filter(Entry::isPreTrusted).map(Entry::getNode).collect(Collectors.toSet());
	}

	public static class Entry {

		final boolean isPreTrusted;
		final NodeBehavior behavior;
		final Node node;

		public Entry(
				final String address,
				final boolean isEvil,
				final boolean isPreTrusted,
				final double honestDataProbability,
				final double honestFeedbackProbability,
				final boolean isLeech,
				final boolean isCollusive) {

			this.isPreTrusted = isPreTrusted;

			this.behavior = new NodeBehavior(
					isEvil,
					honestDataProbability,
					honestFeedbackProbability,
					isLeech,
					isCollusive);

			this.node = NodeUtils.createNodeWithHost(address);
		}

		public Entry(final String line) {
			final String[] nodeAttributes = line.split(";");
			if (nodeAttributes.length != NODE_ATTRIBUTE_COUNT) {
				throw new IllegalArgumentException(String.format("Malformed data in configuration file [%s]", line));
			}

			this.isPreTrusted = nodeAttributes[2].equals("1");

			final boolean isEvil = nodeAttributes[1].equals("1");
			final double honestDataProbability = Double.parseDouble(nodeAttributes[3]);
			final double honestFeedbackProbability = Double.parseDouble(nodeAttributes[4]);
			final boolean isLeech = nodeAttributes[5].equals("1");
			final boolean isCollusive = nodeAttributes[6].equals("1");
			this.behavior = new NodeBehavior(
					isEvil,
					honestDataProbability,
					honestFeedbackProbability,
					isLeech,
					isCollusive);

			this.node = NodeUtils.createNodeWithHost(nodeAttributes[0]);
		}

		public boolean isPreTrusted() {
			return this.isPreTrusted;
		}

		public NodeBehavior getBehavior() {
			return this.behavior;
		}

		public Node getNode() {
			return this.node;
		}
	}
}
