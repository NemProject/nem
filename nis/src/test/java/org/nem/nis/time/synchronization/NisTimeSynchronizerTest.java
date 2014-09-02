package org.nem.nis.time.synchronization;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.time.SystemTimeProvider;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.trust.NodeSelector;

import java.util.*;
import java.util.concurrent.*;

public class NisTimeSynchronizerTest {

	@Test
	public void synchronizeTimeDelegatesToNodeSelector() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.selector, Mockito.times(1)).selectNodes();
	}

	@Test
	public void synchronizeTimeDelegatesToTimeSynchronizationConnector() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(0));
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(1));
		Mockito.verify(context.connector, Mockito.times(1)).getCommunicationTimeStamps(context.nodes.get(2));
	}

	@Test
	public void synchronizeTimeDelegatesToTimeSynchronizationStrategy() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.syncStrategy, Mockito.times(1)).calculateTimeOffset(context.samples, context.age);
	}

	@Test
	public void synchronizeTimeDelegatesToSystemTimeProvider() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.systemTimeProvider, Mockito.times(6)).getNetworkTime();
	}

	@Test
	public void synchronizeTimeUpdatesSystemTimeProviderTimeOffset() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.systemTimeProvider, Mockito.times(1)).updateTimeOffset(new TimeOffset(100));
	}

	@Test
	public void synchronizeTimeIncrementsNodeAge() throws ExecutionException, InterruptedException {
		// Arrange:
		TimeSynchronizationContext context = new TimeSynchronizationContext();
		NisTimeSynchronizer synchronizer = new NisTimeSynchronizer(
				context.selector,
				context.syncStrategy,
				context.connector,
				context.systemTimeProvider,
				context.networkState);

		// Act:
		synchronizer.synchronizeTime();

		// Assert:
		Mockito.verify(context.networkState, Mockito.times(1)).incrementAge();
	}

	private class TimeSynchronizationContext {
		final NodeSelector selector = Mockito.mock(NodeSelector.class);
		final TimeSynchronizationStrategy syncStrategy = Mockito.mock(TimeSynchronizationStrategy.class);
		final TimeSynchronizationConnector connector = Mockito.mock(TimeSynchronizationConnector.class);
		final SystemTimeProvider systemTimeProvider = Mockito.mock(SystemTimeProvider.class);
		final PeerNetworkState networkState;
		final List<Node> nodes;
		final List<TimeSynchronizationSample> samples;
		NodeAge age = new NodeAge(0);

		private TimeSynchronizationContext() throws ExecutionException, InterruptedException {
			this.networkState = Mockito.mock(PeerNetworkState.class);
			Mockito.when(networkState.getNodeAge()).thenReturn(this.age);
			nodes = createPartnerNodes();
			List<CompletableFuture<CommunicationTimeStamps>> timeStampsList = createCommunicationTimeStamps();
			samples = createSamples(nodes, timeStampsList);
			Mockito.when(selector.selectNodes()).thenReturn(nodes);
			Mockito.when(systemTimeProvider.getNetworkTime()).thenReturn(
					new NetworkTimeStamp(0),
					new NetworkTimeStamp(20),
					new NetworkTimeStamp(10),
					new NetworkTimeStamp(30),
					new NetworkTimeStamp(20),
					new NetworkTimeStamp(40));
			Mockito.when(connector.getCommunicationTimeStamps(nodes.get(0))).thenReturn(timeStampsList.get(0));
			Mockito.when(connector.getCommunicationTimeStamps(nodes.get(1))).thenReturn(timeStampsList.get(1));
			Mockito.when(connector.getCommunicationTimeStamps(nodes.get(2))).thenReturn(timeStampsList.get(2));
			Mockito.when(syncStrategy.calculateTimeOffset(samples, age)).thenReturn(new TimeOffset(100));
		}

		private List<Node> createPartnerNodes() {
			final List<Node> nodes = new ArrayList<>();
			for (int i=0; i<3; i++) {
				nodes.add(Mockito.mock(Node.class));
			}

			return nodes;
		}

		private List<CompletableFuture<CommunicationTimeStamps>> createCommunicationTimeStamps() {
			return Arrays.asList(
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(10), new NetworkTimeStamp(15))),
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(20), new NetworkTimeStamp(25))),
					CompletableFuture.completedFuture(new CommunicationTimeStamps(new NetworkTimeStamp(30), new NetworkTimeStamp(35)))
			);
		}

		private List<TimeSynchronizationSample> createSamples(List<Node> nodes, List<CompletableFuture<CommunicationTimeStamps>> timeStampsList) throws ExecutionException, InterruptedException {
			final List<TimeSynchronizationSample> samples = new ArrayList<>();
			for (int i=0; i<3; i++) {
				samples.add(new TimeSynchronizationSample(
						nodes.get(i),
						new CommunicationTimeStamps(new NetworkTimeStamp(10 * i), new NetworkTimeStamp(10 * i + 20)),
						timeStampsList.get(i).get()));
			}

			return samples;
		}
	}
}
