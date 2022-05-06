package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.TimeOffset;
import org.nem.core.node.Node;
import org.nem.core.time.*;
import org.nem.core.time.synchronization.*;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * NIS time synchronization with other NIS nodes.
 */
public class NisTimeSynchronizer implements TimeSynchronizer {
	private static final Logger LOGGER = Logger.getLogger(NisTimeSynchronizer.class.getName());

	private final NodeSelector selector;
	private final TimeSynchronizationStrategy syncStrategy;
	private final TimeSynchronizationConnector connector;
	private final TimeProvider timeProvider;
	private final PeerNetworkState networkState;

	public NisTimeSynchronizer(final NodeSelector selector, final TimeSynchronizationStrategy syncStrategy,
			final TimeSynchronizationConnector connector, final TimeProvider timeProvider, final PeerNetworkState networkState) {
		this.selector = selector;
		this.syncStrategy = syncStrategy;
		this.connector = connector;
		this.timeProvider = timeProvider;
		this.networkState = networkState;
	}

	@Override
	public CompletableFuture<Void> synchronizeTime() {
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		final List<Node> nodes = this.selector.selectNodes();
		LOGGER.info(String.format("Time synchronization: found %d nodes to synchronize with.", nodes.size()));
		final CompletableFuture<?>[] futures = new CompletableFuture<?>[nodes.size()];
		int i = 0;
		for (final Node n : nodes) {
			final NetworkTimeStamp sendTimeStamp = this.timeProvider.getNetworkTime();
			final CompletableFuture<TimeSynchronizationSample> future = this.connector.getCommunicationTimeStamps(n).thenApply(c -> {
				final NetworkTimeStamp receiveTimeStamp = this.timeProvider.getNetworkTime();
				final TimeSynchronizationSample sample = new TimeSynchronizationSample(n,
						new CommunicationTimeStamps(sendTimeStamp, receiveTimeStamp), c);
				samples.add(sample);
				return sample;
			});
			futures[i++] = future;
		}

		return CompletableFuture.allOf(futures).whenComplete((o, e) -> {
			TimeOffset timeOffset = this.syncStrategy.calculateTimeOffset(samples, this.networkState.getNodeAge());
			timeOffset = TimeSynchronizationConstants.CLOCK_ADJUSTMENT_THRESHOLD < Math.abs(timeOffset.getRaw())
					? timeOffset
					: new TimeOffset(0);
			this.networkState.updateTimeSynchronizationResults(this.timeProvider.updateTimeOffset(timeOffset));
		});
	}
}
