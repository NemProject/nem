package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.time.TimeProvider;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NisTimeSynchronizer implements TimeSynchronizer {

	private final NodeSelector selector;
	private final SynchronizationStrategy syncStrategy;
	private final TimeSyncConnector connector;
	private final TimeProvider timeProvider;
	private final PeerNetworkState networkState;

	public NisTimeSynchronizer(
			final NodeSelector selector,
			final SynchronizationStrategy syncStrategy,
			final TimeSyncConnector connector,
			final TimeProvider timeProvider,
			final PeerNetworkState networkState) {
		this.selector = selector;
		this.syncStrategy = syncStrategy;
		this.connector = connector;
		this.timeProvider = timeProvider;
		this.networkState = networkState;
	}

	@Override
	public void synchronizeTime() {
		List<SynchronizationSample> samples = new ArrayList<>();
		List<Node> nodes = this.selector.selectNodes();
		final List<CompletableFuture> futures = nodes.stream()
				.map(n -> {
					final NetworkTimeStamp sendTimeStamp = this.timeProvider.getNetworkTime();
					return this.connector.getCommunicationTimeStamps(n)
							.thenApply(c -> {
								final NetworkTimeStamp receiveTimeStamp = this.timeProvider.getNetworkTime();
								final SynchronizationSample sample = new SynchronizationSample(n, new CommunicationTimeStamps(sendTimeStamp, receiveTimeStamp), c);
								samples.add(sample);
								return sample;
							});
				})
				.collect(Collectors.toList());
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.whenComplete((o, e) -> {
					final TimeOffset timeOffset = this.syncStrategy.calculateTimeOffset(samples, this.networkState.getNodeAge());
					this.timeProvider.updateTimeOffset(timeOffset);
					this.networkState.incrementAge();
				});
	}
}
