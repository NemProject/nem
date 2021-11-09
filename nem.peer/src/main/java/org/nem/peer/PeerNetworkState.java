package org.nem.peer;

import org.nem.core.model.primitive.NodeAge;
import org.nem.core.node.*;
import org.nem.core.time.*;
import org.nem.peer.trust.TrustContext;
import org.nem.peer.trust.score.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Encapsulates information about the state of a peer network.
 */
public class PeerNetworkState {
	private static final Logger LOGGER = Logger.getLogger(PeerNetworkState.class.getName());

	private final Config config;
	private final Node localNode;
	private final NodeCollection nodes;
	private final NodeExperiences nodeExperiences;
	private final AtomicInteger chainSynchronization = new AtomicInteger(0);
	private NodeAge nodeAge;
	private final List<TimeSynchronizationResult> timeSynchronizationResults = new ArrayList<>();

	/**
	 * Creates new peer network state.
	 *
	 * @param config The network configurations.
	 * @param nodeExperiences The node experiences.
	 * @param nodeCollection The node collection.
	 */
	public PeerNetworkState(final Config config, final NodeExperiences nodeExperiences, final NodeCollection nodeCollection) {
		this.config = config;
		this.localNode = config.getLocalNode();
		this.nodeExperiences = nodeExperiences;
		this.nodes = nodeCollection;
		this.nodeAge = new NodeAge(0);

		for (final Node node : this.config.getPreTrustedNodes().getNodes()) {
			this.nodes.update(node, NodeStatus.ACTIVE);
		}
	}

	/**
	 * Gets the configuration.
	 *
	 * @return The configuration.
	 */
	public Config getConfiguration() {
		return this.config;
	}

	/**
	 * Gets the local node.
	 *
	 * @return The local node.
	 */
	public Node getLocalNode() {
		return this.localNode;
	}

	/**
	 * Gets all nodes known to the network.
	 *
	 * @return All nodes known to the network.
	 */
	public NodeCollection getNodes() {
		return this.nodes;
	}

	/**
	 * Gets the local node and information about its current experiences.
	 *
	 * @return The local node and information about its current experiences.
	 */
	public NodeExperiencesPair getLocalNodeAndExperiences() {
		final Node localNode = this.getLocalNode();
		return new NodeExperiencesPair(localNode, this.nodeExperiences.getNodeExperiences(localNode));
	}

	/**
	 * Sets the experiences for the specified remote node.
	 *
	 * @param pair A node and experiences pair for a remote node.
	 * @param timeStamp The timeStamp when the pair was acquired.
	 */
	public void setRemoteNodeExperiences(final NodeExperiencesPair pair, final TimeInstant timeStamp) {
		if (this.getLocalNode().equals(pair.getNode())) {
			throw new IllegalArgumentException("cannot set local node experiences");
		}

		this.nodeExperiences.setNodeExperiences(pair.getNode(), pair.getExperiences(), timeStamp);
	}

	/**
	 * Updates the local node's experience with the specified node.
	 *
	 * @param node The remote node that was interacted with.
	 * @param result The interaction result.
	 */
	public void updateExperience(final Node node, final NodeInteractionResult result) {
		if (NodeInteractionResult.NEUTRAL == result || node.equals(this.localNode)) {
			return;
		}

		final NodeExperience experience = this.nodeExperiences.getNodeExperience(this.localNode, node);
		final boolean isSuccess = NodeInteractionResult.SUCCESS == result;
		(isSuccess ? experience.successfulCalls() : experience.failedCalls()).increment();

		// only log failures to avoid bloating logs
		if (!isSuccess) {
			LOGGER.info(String.format("Updating experience with %s: %s", node, result));
		}
	}

	/**
	 * Prunes the node experiences according to the given timestamp.
	 *
	 * @param currentTime The current time.
	 */
	public void pruneNodeExperiences(final TimeInstant currentTime) {
		this.nodeExperiences.prune(currentTime);
	}

	/**
	 * Creates a trust context.
	 *
	 * @return The trust context.
	 */
	public TrustContext getTrustContext() {
		// create a new trust context each iteration in order to allow
		// nodes to change in-between iterations.
		return new TrustContext(toNodeArray(this.nodes, this.getLocalNode()), this.getLocalNode(), this.nodeExperiences,
				this.config.getPreTrustedNodes(), this.config.getTrustParameters());
	}

	private static Node[] toNodeArray(final NodeCollection nodes, final Node localNode) {
		final int numNodes = nodes.getAllNodes().size() + 1;
		final Node[] nodeArray = new Node[numNodes];

		int index = 0;
		for (final Node node : nodes.getAllNodes()) {
			nodeArray[index++] = node;
		}

		nodeArray[index] = localNode;
		return nodeArray;
	}

	/**
	 * Gets a value indication whether or not the local chain is synchronized with the rest of the network.
	 *
	 * @return true if synchronized, false otherwise.
	 */
	public boolean isChainSynchronized() {
		return this.chainSynchronization.get() > 0;
	}

	/**
	 * Set a value indicating if the local chain is synchronized with the rest of the network.
	 *
	 * @param isChainSynchronized true if the local chain is synchronized, false otherwise.
	 */
	public void setChainSynchronized(final boolean isChainSynchronized) {
		if (isChainSynchronized) {
			this.chainSynchronization.set(4);
		} else {
			this.chainSynchronization.decrementAndGet();
		}
	}

	/**
	 * Gets the local node's age.
	 *
	 * @return the node's age.
	 */
	public NodeAge getNodeAge() {
		return this.nodeAge;
	}

	/**
	 * Increments local the node's age by one.
	 */
	private void incrementAge() {
		this.nodeAge = this.nodeAge.increment();
	}

	/**
	 * Gets the list of the most recent time synchronization results.
	 *
	 * @return The list of time synchronization results.
	 */
	public Collection<TimeSynchronizationResult> getTimeSynchronizationResults() {
		return this.timeSynchronizationResults;
	}

	/**
	 * Adds a time synchronization result to the list. Removes the oldest result if the size of the is exceeding 100.
	 *
	 * @param result The time result to be added.
	 */
	public void updateTimeSynchronizationResults(final TimeSynchronizationResult result) {
		this.incrementAge();
		this.timeSynchronizationResults.add(result);
		if (this.timeSynchronizationResults.size() > 100) {
			this.timeSynchronizationResults.remove(0);
		}
	}
}
