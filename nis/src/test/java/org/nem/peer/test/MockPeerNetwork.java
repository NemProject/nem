package org.nem.peer.test;


import org.mockito.Mockito;
import org.nem.core.connect.NodeApiId;
import org.nem.core.connect.SyncConnectorPool;
import org.nem.core.serialization.SerializableEntity;
import org.nem.peer.*;
import org.nem.peer.trust.score.NodeExperiences;

import java.util.concurrent.CompletableFuture;

/**
 * A mock PeerNetwork implementation.
 */
public class MockPeerNetwork extends PeerNetwork {

	private final Object refreshMonitor;
	private int numRefreshCalls;
	private int numBroadcastCalls;
	private int numSynchronizeCalls;
	private NodeApiId lastBroadcastId;
	private SerializableEntity lastBroadcastEntity;

	/**
	 * Creates a new mock peer network.
	 */
	public MockPeerNetwork() {
		this((Object)null);
	}

	/**
	 * Creates a new mock peer network.
	 *
	 * @param refreshMonitor The refresh monitor to use for synchronization.
	 */
	public MockPeerNetwork(final Object refreshMonitor) {
		super(ConfigFactory.createDefaultTestConfig(), createMockPeerNetworkServices());
		this.refreshMonitor = refreshMonitor;
	}

	/**
	 * Creates a new mock peer network around the specified NodeExperiences.
	 *
	 * @param nodeExperiences The node experiences.
	 */
	public MockPeerNetwork(final NodeExperiences nodeExperiences) {
		super(ConfigFactory.createDefaultTestConfig(), createMockPeerNetworkServices(), nodeExperiences);
		this.refreshMonitor = null;
	}

	/**
	 * Gets the number of times refresh was called.
	 *
	 * @return The number of times refresh was called.
	 */
	public int getNumRefreshCalls() {
		return this.numRefreshCalls;
	}

	/**
	 * Gets the number of times broadcast was called.
	 *
	 * @return The number of times broadcast was called.
	 */
	public int getNumBroadcastCalls() {
		return this.numBroadcastCalls;
	}

	/**
	 * Gets the last broadcastId passed to broadcast.
	 *
	 * @return The broadcastId passed to broadcast
	 */
	public NodeApiId getLastBroadcastId() {
		return this.lastBroadcastId;
	}

	/**
	 * Gets the last entity passed to broadcast.
	 *
	 * @return The entity passed to broadcast
	 */
	public SerializableEntity getLastBroadcastEntity() {
		return this.lastBroadcastEntity;
	}

	/**
	 * Gets the number of times synchronize was called.
	 *
	 * @return The number of times synchronize was called.
	 */
	public int getNumSynchronizeCalls() {
		return this.numSynchronizeCalls;
	}

	@Override
	public CompletableFuture refresh() {
		if (null != this.refreshMonitor)
			org.nem.core.test.Utils.monitorWait(this.refreshMonitor);

		++this.numRefreshCalls;
		return new CompletableFuture();
	}

	@Override
	public CompletableFuture broadcast(final NodeApiId broadcastId, final SerializableEntity entity) {
		++this.numBroadcastCalls;
		this.lastBroadcastId = broadcastId;
		this.lastBroadcastEntity = entity;
		return new CompletableFuture();
	}

	@Override
	public void synchronize() {
		++this.numSynchronizeCalls;
	}

	private static PeerNetworkServices createMockPeerNetworkServices() {
		return new PeerNetworkServices(
				new MockConnector(),
				Mockito.mock(SyncConnectorPool.class),
				new MockBlockSynchronizer());
	}
}