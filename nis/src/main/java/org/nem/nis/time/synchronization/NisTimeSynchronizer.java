package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.time.SystemTimeProvider;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NisTimeSynchronizer implements TimeSynchronizer {

	private final NodeSelector selector;
	private final SynchronizationStrategy syncStrategy;
	private final TimeSyncConnector connector;
	private final SystemTimeProvider systemTimeProvider;
	private final NodeAge age;

	public NisTimeSynchronizer(
			final NodeSelector selector,
			final SynchronizationStrategy syncStrategy,
			final TimeSyncConnector connector,
			final SystemTimeProvider systemTimeProvider,
			final NodeAge age) {
		this.selector = selector;
		this.syncStrategy = syncStrategy;
		this.connector = connector;
		this.systemTimeProvider = systemTimeProvider;
		this.age = age;
	}

	@Override
	public void synchronizeTime() {
		List<SynchronizationSample> samples = new ArrayList<>();
		List<Node> nodes = this.selector.selectNodes();
		final List<CompletableFuture> futures = nodes.stream()
				.map(n -> {
					final NetworkTimeStamp sendTimeStamp = this.systemTimeProvider.getNetworkTime();
					return connector.getCommunicationTimeStamps(n)
							.thenApply(c -> {
								final NetworkTimeStamp receiveTimeStamp = this.systemTimeProvider.getNetworkTime();
								final SynchronizationSample sample = new SynchronizationSample(n, new CommunicationTimeStamps(sendTimeStamp, receiveTimeStamp), c);
								samples.add(sample);
								return sample;
							});
				})
				.collect(Collectors.toList());
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.whenComplete((o, e) -> {
					final TimeOffset timeOffset = syncStrategy.calculateTimeOffset(samples,this.age);
					systemTimeProvider.updateTimeOffset(timeOffset);
				});
	}
}
