package org.nem.nis.time.synchronization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.time.synchronization.filter.*;

import java.text.DecimalFormat;
import java.util.*;

public class TimeSynchronizationITCase {
	private static final int STANDARD_NETWORK_SIZE = 500;
	private static final int INITIAL_TIME_SPREAD = 50000;
	private static final boolean REMOTE_RECEIVE_SEND_DELAY = true;
	private static final boolean ASYMMETRIC_CHANNELS = true;
	private static final boolean INSTABLE_CLOCK = true;
	private static final int MAX_VIEW_SIZE = STANDARD_NETWORK_SIZE;
	private static final int MEDIUM_VIEW_SIZE = 60;
	private static final int SMALL_VIEW_SIZE = 5;
	private static final int DEFAULT_NUMBER_OF_ROUNDS = 20;
	private static final boolean CLOCK_ADJUSTMENT = true;
	private static final int DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS = 10;

	/**
	 * Maximal tolerable shift of the mean network time per round in milli seconds.
	 */
	private static final double TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND = 0.05;

	/**
	 * Value indicating how much a node's network time is allowed to deviate
	 * from the mean network time before it is considered as faulty.
	 */
	private static final long TOLERABLE_MAX_DEVIATION_FROM_MEAN = 3000;

	private double meanTimeShiftPerRound = 0;

	/**
	 * Basic tests of convergence in a friendly, ideal environment.
	 */
	@Test
	public void doesConvergeWithMaxViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				MAX_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithMediumViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithSmallViewSizeInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				SMALL_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithSmallViewSizeInLargeAndFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE * 10,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				SMALL_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	/**
	 * Basic tests of convergence in a friendly but more realistic environment.
	 */
	@Test
	public void asymmetricChannelsDoNotProduceShiftInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, 10 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void remoteReceiveSendDelayDoesNotProduceShiftInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, 10 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void instableClockWithoutPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				!CLOCK_ADJUSTMENT);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void instableClockTogetherWithPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				CLOCK_ADJUSTMENT);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void realisticFriendlyEnvironmentDoesNotProduceShift() {
		final Network network = setupNetwork(
				STANDARD_NETWORK_SIZE,
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				MEDIUM_VIEW_SIZE,
				CLOCK_ADJUSTMENT);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	private void synchronize(
			final Network network, 
			final int numberOfRounds,
			final int logAfterHowManyRounds) {
		double meanAfterHundredRounds = 0;
		for (int i=0; i<numberOfRounds; i++) {
			//network.outputNodes();
			network.updateStatistics();
			if (i % logAfterHowManyRounds == 0) {
				network.log(String.format("Synchronization round %d", i));
				network.logStatistics();
			}
			if (i == 99) {
				meanAfterHundredRounds = network.calculateMean();
			}
			synchronizationRound(network);
		}
		network.log(String.format("Result after %d rounds", numberOfRounds));
		network.logStatistics();
		meanTimeShiftPerRound = (network.calculateMean() - meanAfterHundredRounds)/(numberOfRounds - 100);
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		if (numberOfRounds > 100) {
			network.log(String.format("mean time shift per round is %s ms.", format.format(meanTimeShiftPerRound)));
		}
		network.outputOutOfRangeNodes(TOLERABLE_MAX_DEVIATION_FROM_MEAN);
	}

	private void synchronizationRound(final Network network) {
		network.getNodes().stream()
				.forEach(n -> {
					//network.log("Synchronizing " + n.getName());
					Set<TimeAwareNode> partners = network.selectSyncPartnersForNode(n);
					List<SynchronizationSample> samples = network.createSynchronizationSamples(n, partners);
					n.updateNetworkTime(samples);
				});
		network.clockAdjustment();
	}

	private Network setupNetwork(
			final int numberOfNodes,
			final int initialTimeSpread,
			final boolean remoteReceiveSendDelay,
			final boolean asymmetricChannels,
			final boolean instableClock,
			final int viewSize,
			final boolean clockAdjustment) {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
		final SynchronizationStrategy syncStrategy = new DefaultSynchronizationStrategy(filter);
		return new Network(
				numberOfNodes,
				syncStrategy,
				initialTimeSpread,
				remoteReceiveSendDelay,
				asymmetricChannels,
				instableClock,
				viewSize,
				clockAdjustment);
	}
}
