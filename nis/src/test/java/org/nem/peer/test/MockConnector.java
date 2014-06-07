package org.nem.peer.test;

import org.nem.core.connect.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.serialization.SerializableList;
import org.nem.core.utils.ExceptionUtils;
import org.nem.peer.connect.*;
import org.nem.peer.node.*;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A mock PeerConnector and SyncConnector implementation.
 */
public class MockConnector implements PeerConnector, SyncConnector {

	private AtomicInteger numGetInfoCalls = new AtomicInteger();
	private AtomicInteger numGetKnownPeerCalls = new AtomicInteger();
	private AtomicInteger numAnnounceCalls = new AtomicInteger();
	private AtomicInteger numGetLocalNodeInfoCalls = new AtomicInteger();

	private Map<String, TriggerAction> getInfoTriggers = new HashMap<>();

	private Map<String, TriggerAction> getLocalNodeInfoTriggers = new HashMap<>();
	private NodeEndpoint getLocalNodeInfoEndpoint;
	private NodeEndpoint lastGetLocalNodeInfoLocalEndpoint;

	private String getKnownPeersErrorTrigger;
	private TriggerAction getKnownPeersErrorTriggerAction;

	private NodeApiId lastAnnounceId;
	private SerializableEntity lastAnnounceEntity;

	private boolean shouldAnnounceDelay;

	private List<Node> knownPeers = new ArrayList<>();

	//region TriggerAction

	/**
	 * Possible actions that can be triggered.
	 */
	public enum TriggerAction {
		/**
		 * No action.
		 */
		NONE,

		/**
		 * Throws an InactivePeerException.
		 */
		INACTIVE,

		/**
		 * Throws a FatalPeerException.
		 */
		FATAL,

		/*
		 * Returns a node with a different address.
		 */
		CHANGE_ADDRESS,

		/*
		 * Returns a node with a different identity.
		 */
		CHANGE_IDENTITY,

		/**
		 * Sleeps the thread for a small period of time and then throws an InactivePeerException.
		 */
		SLEEP_INACTIVE
	}

	//endregion

	/**
	 * Gets the number of times getInfo was called.
	 *
	 * @return The number of times getInfo was called.
	 */
	public int getNumGetInfoCalls() {
		return this.numGetInfoCalls.get();
	}

	/**
	 * Gets the number of times getKnownPeers was called.
	 *
	 * @return The number of times getKnownPeers was called.
	 */
	public int getNumGetKnownPeerCalls() {
		return this.numGetKnownPeerCalls.get();
	}

	/**
	 * Gets the number of times announce was called.
	 *
	 * @return The number of times announce was called.
	 */
	public int getNumAnnounceCalls() {
		return this.numAnnounceCalls.get();
	}

	/**
	 * Gets the number of times getLocalNodeInfo was called.
	 *
	 * @return The number of times getLocalNodeInfo was called.
	 */
	public int getNumGetLocalNodeInfoCalls() {
		return this.numGetLocalNodeInfoCalls.get();
	}

	/**
	 * Gets the last announcement id passed to announce.
	 *
	 * @return The last announcement id passed to announce.
	 */
	public NodeApiId getLastAnnounceId() {
		return this.lastAnnounceId;
	}

	/**
	 * Gets the last entity passed to announce.
	 *
	 * @return The last entity passed to announce.
	 */
	public SerializableEntity getLastAnnounceEntity() {
		return this.lastAnnounceEntity;
	}

	/**
	 * Gets the last local endpoint passed to getLocalNodeInfo.
	 *
	 * @return The last local endpoint passed to getLocalNodeInfo.
	 */
	public NodeEndpoint getLastGetLocalNodeInfoLocalEndpoint() {
		return this.lastGetLocalNodeInfoLocalEndpoint;
	}

	/**
	 * Triggers a specific action in getInfo.
	 *
	 * @param trigger The endpoint hostname that should cause the action.
	 * @param action  The action.
	 */
	public void setGetInfoError(final String trigger, final TriggerAction action) {
		this.getInfoTriggers.put(trigger, action);
	}

	/**
	 * Triggers a specific action in getLocalNodeInfo.
	 *
	 * @param trigger The endpoint hostname that should cause the action.
	 * @param action  The action.
	 */
	public void setGetLocalNodeInfoError(final String trigger, final TriggerAction action) {
		this.getLocalNodeInfoTriggers.put(trigger, action);
	}

	/**
	 * Sets the endpoint that should be returned by getLocalNodeInfo.
	 *
	 * @param endpoint The endpoint that should be returned.
	 */
	public void setGetLocalNodeInfoEndpoint(final NodeEndpoint endpoint) {
		this.getLocalNodeInfoEndpoint = endpoint;
	}

	/**
	 * Triggers a specific action in getKnownPeers.
	 *
	 * @param trigger The endpoint hostname that should cause the action.
	 * @param action  The action.
	 */
	public void setGetKnownPeersError(final String trigger, final TriggerAction action) {
		this.getKnownPeersErrorTrigger = trigger;
		this.getKnownPeersErrorTriggerAction = action;
	}

	/**
	 * Sets the list that should be returned by getKnownPeers.
	 *
	 * @param nodes The list that should be returned by getKnownPeers.
	 */
	public void setKnownPeers(final List<Node> nodes) {
		this.knownPeers = nodes;
	}

	/**
	 * Sets a value indicating whether or not announce should simulate a delay by sleeping for a small period of time.
	 *
	 * @param delay true if a delay should be set.
	 */
	public void setAnnounceDelay(final boolean delay) {
		this.shouldAnnounceDelay = delay;
	}

	@Override
	public Block getLastBlock(final Node node) {
		return null;
	}

	@Override
	public Block getBlockAt(final Node node, final BlockHeight height) {
		return null;
	}

	@Override
	public List<Block> getChainAfter(final Node node, final BlockHeight height) {
		return null;
	}

	@Override
	public HashChain getHashesFrom(final Node node, final BlockHeight height) {
		return null;
	}

	@Override
	public BlockChainScore getChainScore(final Node node) {
		return null;
	}
	
	@Override
	public CompletableFuture<Node> getInfo(final Node node) {
		this.numGetInfoCalls.incrementAndGet();

		return CompletableFuture.supplyAsync(() -> {
			final NodeEndpoint endpoint = node.getEndpoint();
			NodeEndpoint endpointAfterChange = endpoint;
			NodeIdentity identityAfterChange = node.getIdentity();
			final TriggerAction action = this.getInfoTriggers.get(endpoint.getBaseUrl().getHost());
			if (null != action) {
				triggerGeneralAction(action);
				switch (action) {
					case CHANGE_IDENTITY:
						identityAfterChange = new NodeIdentity(new KeyPair());
						break;

					case CHANGE_ADDRESS:
						final URL url = endpoint.getBaseUrl();
						endpointAfterChange = new NodeEndpoint(url.getProtocol(), url.getHost(), url.getPort() + 1);
						break;
				}
			}

			return new Node(identityAfterChange, endpointAfterChange);
		});
	}

	@Override
	public CompletableFuture<SerializableList<Node>> getKnownPeers(final Node node) {
		this.numGetKnownPeerCalls.incrementAndGet();

		return CompletableFuture.supplyAsync(() -> {
			if (shouldTriggerAction(node.getEndpoint(), this.getKnownPeersErrorTrigger))
				triggerGeneralAction(this.getKnownPeersErrorTriggerAction);

			return new SerializableList<>(this.knownPeers);
		});
	}

	@Override
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(final Node node, final NodeEndpoint localEndpoint) {
		this.numGetLocalNodeInfoCalls.incrementAndGet();

		return CompletableFuture.supplyAsync(() -> {
			final TriggerAction action = this.getLocalNodeInfoTriggers.get(node.getEndpoint().getBaseUrl().getHost());
			if (null != action)
				triggerGeneralAction(action);

			this.lastGetLocalNodeInfoLocalEndpoint = localEndpoint;
			return this.getLocalNodeInfoEndpoint;
		});
	}

	@Override
	public CompletableFuture announce(final Node node, final NodeApiId announceId, final SerializableEntity entity) {
		this.numAnnounceCalls.incrementAndGet();

		return CompletableFuture.supplyAsync(() -> {
			if (this.shouldAnnounceDelay)
				pauseThread();

			this.lastAnnounceId = announceId;
			this.lastAnnounceEntity = entity;
			return null;
		});
	}

	private static boolean shouldTriggerAction(final NodeEndpoint endpoint, final String trigger) {
		return endpoint.getBaseUrl().getHost().equals(trigger);
	}

	private static void triggerGeneralAction(final TriggerAction action) {
		if (action == TriggerAction.SLEEP_INACTIVE)
			pauseThread();

		switch (action) {
			case SLEEP_INACTIVE:
			case INACTIVE:
				throw new InactivePeerException("inactive peer");

			case FATAL:
				throw new FatalPeerException("fatal peer");
		}
	}

	private static void pauseThread() {
		ExceptionUtils.propagateVoid(() -> Thread.sleep(300));
	}
}