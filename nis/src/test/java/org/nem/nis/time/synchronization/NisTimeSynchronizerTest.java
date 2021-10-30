package org.nem.nis.time.synchronization;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.time.*;
import org.nem.core.time.synchronization.*;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NisTimeSynchronizerTest {
	private static final long CLOCK_ADJUSTMENT_THRESHOLD = TimeSynchronizationConstants.CLOCK_ADJUSTMENT_THRESHOLD;

	@Test
	public void synchronizeTimeDelegatesToNodeSelector() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.selector, Mockito.times(1)).selectNodes();
	}

	@Test
	public void synchronizeTimeDelegatesToTimeSynchronizationConnector() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(0));
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(1));
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(2));
	}

	@Test
	public void synchronizeTimeDelegatesToTimeSynchronizationStrategy() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.syncStrategy, Mockito.times(1)).calculateTimeOffset(context.samples, context.age);
	}

	@Test
	public void synchronizeTimeDelegatesToSystemTimeProvider() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.systemTimeProvider, Mockito.times(6)).getNetworkTime();
	}

	@Test
	public void synchronizeTimeUpdatesSystemTimeProviderTimeOffsetIfOffsetIsBigEnough() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.systemTimeProvider, Mockito.times(1)).updateTimeOffset(new TimeOffset(CLOCK_ADJUSTMENT_THRESHOLD + 1));
	}

	@Test
	public void synchronizeTimeDoesNotUpdateSystemTimeProviderTimeOffsetIfOffsetIsTooSmall() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext(CLOCK_ADJUSTMENT_THRESHOLD);

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.systemTimeProvider, Mockito.times(1)).updateTimeOffset(new TimeOffset(0));
	}

	@Test
	public void synchronizeTimeUpdatesTimeSynchronizationResults() {
		// Arrange:
		final TimeSynchronizationContext context = new TimeSynchronizationContext();

		// Act:
		context.synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.networkState, Mockito.times(1)).updateTimeSynchronizationResults(Mockito.any());
	}

	private class TimeSynchronizationContext {
		final NodeSelector selector = Mockito.mock(NodeSelector.class);
		final TimeSynchronizationStrategy syncStrategy = Mockito.mock(TimeSynchronizationStrategy.class);
		final TimeSynchronizationConnector connector = Mockito.mock(TimeSynchronizationConnector.class);
		final SystemTimeProvider systemTimeProvider = Mockito.mock(SystemTimeProvider.class);
		final PeerNetworkState networkState;
		final NisTimeSynchronizer synchronizer;
		final List<Node> nodes;
		final List<TimeSynchronizationSample> samples;
		final NodeAge age = new NodeAge(0);

		private TimeSynchronizationContext() {
			this(CLOCK_ADJUSTMENT_THRESHOLD + 1);
		}

		private TimeSynchronizationContext(final long offset) {
			this.networkState = Mockito.mock(PeerNetworkState.class);
			Mockito.when(this.networkState.getNodeAge()).thenReturn(this.age);
			this.nodes = this.createPartnerNodes();
			final List<CompletableFuture<CommunicationTimeStamps>> timeStampsList = this.createCommunicationTimeStamps();
			this.samples = this.createSamples(this.nodes, timeStampsList);
			Mockito.when(this.selector.selectNodes()).thenReturn(this.nodes);
			Mockito.when(this.systemTimeProvider.getNetworkTime()).thenReturn(new NetworkTimeStamp(0), new NetworkTimeStamp(20),
					new NetworkTimeStamp(10), new NetworkTimeStamp(30), new NetworkTimeStamp(20), new NetworkTimeStamp(40));
			Mockito.when(this.connector.getCommunicationTimeStamps(this.nodes.get(0))).thenReturn(timeStampsList.get(0));
			Mockito.when(this.connector.getCommunicationTimeStamps(this.nodes.get(1))).thenReturn(timeStampsList.get(1));
			Mockito.when(this.connector.getCommunicationTimeStamps(this.nodes.get(2))).thenReturn(timeStampsList.get(2));
			Mockito.when(this.syncStrategy.calculateTimeOffset(this.samples, this.age)).thenReturn(new TimeOffset(offset));
			this.synchronizer = new NisTimeSynchronizer(this.selector, this.syncStrategy, this.connector, this.systemTimeProvider,
					this.networkState);
		}

		private List<Node> createPartnerNodes() {
			final List<Node> nodes = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				nodes.add(Mockito.mock(Node.class));
			}

			return nodes;
		}

		private List<CompletableFuture<CommunicationTimeStamps>> createCommunicationTimeStamps() {
			return Arrays.asList(
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(10), new NetworkTimeStamp(15))),
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(20), new NetworkTimeStamp(25))),
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(30), new NetworkTimeStamp(35))));
		}

		private List<TimeSynchronizationSample> createSamples(final List<Node> nodes,
				final List<CompletableFuture<CommunicationTimeStamps>> timeStampsList) {
			final List<TimeSynchronizationSample> samples = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
				samples.add(new TimeSynchronizationSample(nodes.get(i),
						new CommunicationTimeStamps(new NetworkTimeStamp(10 * i), new NetworkTimeStamp(10 * i + 20)),
						timeStampsList.get(i).join()));
			}

			return samples;
		}
	}
}
