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

	/**
	 * Value indicating how much the network time is allowed to change
	 * when new nodes join before it is considered as faulty.
	 */
	private static final double TOLERABLE_CHANGE_IN_MEAN = 500;

	private double meanTimeShiftPerRound = 0;

	/**
	 * Basic tests of convergence in a friendly, ideal environment.
	 */
	@Test
	public void doesConvergeWithMaxViewSizeInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MAX_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithMediumViewSizeInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithSmallViewSizeInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, SMALL_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void doesConvergeWithSmallViewSizeInLargeAndFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE * 10, SMALL_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	/**
	 * Basic tests of convergence in a friendly but more realistic environment.
	 */
	@Test
	public void asymmetricChannelsDoNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, 10 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void remoteReceiveSendDelayDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, 10 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void instableClockWithoutPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void instableClockTogetherWithPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	@Test
	public void realisticFriendlyEnvironmentDoesNotProduceShift() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, 100 * DEFAULT_NUMBER_OF_ROUNDS, 10 * DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(this.meanTimeShiftPerRound) < TOLERABLE_MEAN_TIME_SHIFT_PER_ROUND, IsEqual.equalTo(true));
	}

	/**
	 * Basic tests to assure new friendly nodes joining the network don't disrupt the network time.
	 */
	@Test
	public void smallPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		final double oldMean = network.calculateMean();
		network.grow(1);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(oldMean - network.calculateMean()) < TOLERABLE_CHANGE_IN_MEAN, IsEqual.equalTo(true));
	}

	@Test
	public void mediumPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		final double oldMean = network.calculateMean();
		network.grow(10);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(oldMean - network.calculateMean()) < TOLERABLE_CHANGE_IN_MEAN, IsEqual.equalTo(true));
	}

	@Test
	public void highPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		final double oldMean = network.calculateMean();
		network.grow(50);
		synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network.logStatistics();
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(oldMean - network.calculateMean()) < TOLERABLE_CHANGE_IN_MEAN, IsEqual.equalTo(true));
	}

	/**
	 * Basic tests to assure that if there are several sub-networks which have different network times and
	 * those networks finally join, the resulting network will converge to a common network time.
	 */
	@Test
	public void networkTimeConvergesToCommonNetworkTimeWhenTwoNetworksJoin() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final Network network1 = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		final Network network2 = setupNetwork("network2", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);

		// Make sure the two networks have very different network times.
		network2.randomShiftNetworkTime();
		synchronize(network1, DEFAULT_NUMBER_OF_ROUNDS, 0);
		synchronize(network2, DEFAULT_NUMBER_OF_ROUNDS, 0);
		network1.join(network2, "global network");

		// Since both networks were already mature, it needs more rounds to converge
		synchronize(network1, 5 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(network1.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void networkTimeConvergesToCommonNetworkTimeWhenFiveNetworksJoin() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT);
		final List<Network> networks = Arrays.asList(
				setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network2", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network3", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network4", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network5", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings));

		// Make sure the networks have very different network times.
		networks.stream().forEach(Network::randomShiftNetworkTime);
		networks.stream().forEach(network -> synchronize(network, DEFAULT_NUMBER_OF_ROUNDS, 0));
		networks.stream().forEach(network -> networks.get(0).join(network, "global network"));

		// Since both networks were already mature, it needs more rounds to converge
		synchronize(networks.get(0), 5 * DEFAULT_NUMBER_OF_ROUNDS, DEFAULT_LOG_AFTER_HOW_MANY_ROUNDS);
		Assert.assertThat(networks.get(0).hasConverged(), IsEqual.equalTo(true));
	}

	private void synchronize(
			final Network network, 
			final int numberOfRounds,
			final int logAfterHowManyRounds) {
		double meanAfterHundredRounds = 0;
		for (int i=0; i<numberOfRounds; i++) {
			//network.outputNodes();
			network.updateStatistics();
			if (logAfterHowManyRounds > 0 && i % logAfterHowManyRounds == 0) {
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
			network.log(String.format("%s: mean time shift per round is %s ms.", network.getName(), format.format(meanTimeShiftPerRound)));
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
			final String name,
			final int numberOfNodes,
			final int viewSize,
			final NodeSettings nodeSettings) {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
		final SynchronizationStrategy syncStrategy = new DefaultSynchronizationStrategy(filter);
		return new Network(name, numberOfNodes, syncStrategy, viewSize, nodeSettings);
	}

	private NodeSettings createNodeSettings(
			final int timeOffsetSpread,
			final boolean delayCommunication,
			final boolean asymmetricChannels,
			final boolean instableClock,
			final boolean clockAdjustment) {
		return new NodeSettings(
				timeOffsetSpread,
				delayCommunication,
				asymmetricChannels,
				instableClock,
				clockAdjustment);
	}
}
