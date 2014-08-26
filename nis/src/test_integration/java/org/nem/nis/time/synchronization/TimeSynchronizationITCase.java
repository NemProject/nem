package org.nem.nis.time.synchronization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.nis.time.synchronization.filter.*;

import java.util.*;

public class TimeSynchronizationITCase {
	private static final int STANDARD_NETWORK_SIZE = 500;
	private static final int INITIAL_TIME_SPREAD = 50000;
	private static final boolean REMOTE_RECEIVE_SEND_DELAY = true;
	private static final boolean ASYMMETRIC_CHANNELS = true;
	private static final int MAX_VIEW_SIZE = STANDARD_NETWORK_SIZE;
	private static final int MEDIUM_VIEW_SIZE = STANDARD_NETWORK_SIZE / 10;
	private static final int SMALL_VIEW_SIZE = STANDARD_NETWORK_SIZE / 100;
	private static final int DEFAULT_NUMBER_OF_ROUNDS = 20;

	@Test
	public void doesConvergeWithMaxViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				MAX_VIEW_SIZE);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithMediumViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				MEDIUM_VIEW_SIZE);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithSmallViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				SMALL_VIEW_SIZE);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void asymmetricChannelsDoNotProduceShiftInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				MEDIUM_VIEW_SIZE);
		synchronize(network, 5 * DEFAULT_NUMBER_OF_ROUNDS);
		Assert.assertThat(network.hasShifted(), IsEqual.equalTo(false));
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	private void synchronize(final Network network, final int numberOfRounds) {
		for (int i=0; i<numberOfRounds; i++) {
			network.log("Synchronization round " + i);
			//network.outputNodes();
			network.updateStatistics();
			network.logStatistics();
			synchronizationRound(network);
		}
	}

	private void synchronizationRound(final Network network) {
		network.getNodes().stream()
				.forEach(n -> {
					//network.log("Synchronizing " + n.getName());
					Set<TimeAwareNode> partners = network.selectSyncPartnersForNode(n);
					List<SynchronizationSample> samples = network.createSynchronizationSamples(n, partners);
					n.updateNetworkTime(samples);
				});
	}

	private Network setupNetwork(
			final int numberOfNodes,
			final int initialTimeSpread,
			final boolean remoteReceiveSendDelay,
			final boolean asymmetricChannels,
			final int viewSize) {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
		final SynchronizationStrategy syncStrategy = new DefaultSynchronizationStrategy(filter);
		return new Network(numberOfNodes, syncStrategy, initialTimeSpread, remoteReceiveSendDelay, asymmetricChannels, viewSize);
	}
}
