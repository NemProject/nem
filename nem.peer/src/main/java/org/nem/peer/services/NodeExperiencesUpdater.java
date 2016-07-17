package org.nem.peer.services;

import org.nem.core.node.Node;
import org.nem.core.time.TimeProvider;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.connect.PeerConnector;
import org.nem.peer.trust.NodeSelector;
import org.nem.peer.trust.score.NodeExperiencesPair;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Class that pulls node experiences from remote nodes and updates the local node's node experiences.
 */
public class NodeExperiencesUpdater {
	private static final Logger LOGGER = Logger.getLogger(NodeExperiencesUpdater.class.getName());

	private final PeerConnector connector;
	private final TimeProvider timeProvider;
	private final PeerNetworkState state;

	/**
	 * Creates a new node experiences updater.
	 *
	 * @param connector The peer connector to use.
	 * @param state The network state.
	 */
	public NodeExperiencesUpdater(
			final PeerConnector connector,
			final TimeProvider timeProvider,
			final PeerNetworkState state) {
		this.connector = connector;
		this.timeProvider = timeProvider;
		this.state = state;
	}

	public CompletableFuture<Boolean> update(final NodeSelector selector) {
		final Node partnerNode = selector.selectNode();
		if (null == partnerNode) {
			LOGGER.warning("no suitable peers found to pull node experiences from");
			return CompletableFuture.completedFuture(false);
		}

		LOGGER.info(String.format("pulling node experiences from %s", partnerNode));
		final CompletableFuture<NodeExperiencesPair> future = this.connector.getNodeExperiences(partnerNode);
		return future.thenApply(pair -> {
			this.state.setRemoteNodeExperiences(pair, this.timeProvider.getCurrentTime());
			return true;
		});
	}
}
