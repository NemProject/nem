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
	private static final boolean CLOCK_ADJUSTMENT = true;
	private static final int NO_EVIL_NODES = 0;

	/**
	 * Maximal tolerable shift of the mean network time per day in milli seconds.
	 */
	private static final double TOLERABLE_MEAN_TIME_SHIFT_PER_DAY = 50;

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

	/**
	 * Tests of convergence in a friendly, ideal environment.
	 */
	@Test
	public void doesConvergeWithMaxViewSizeInFriendlyAndIdealEnvironment() {
		assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(STANDARD_NETWORK_SIZE, MAX_VIEW_SIZE);
	}

	@Test
	public void doesConvergeWithMediumViewSizeInFriendlyAndIdealEnvironment() {
		assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE);
	}

	@Test
	public void doesConvergeWithSmallViewSizeInFriendlyAndIdealEnvironment() {
		assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(STANDARD_NETWORK_SIZE, SMALL_VIEW_SIZE);
	}

	@Test
	public void doesConvergeWithSmallViewSizeInLargeAndFriendlyAndIdealEnvironment() {
		assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(10 * STANDARD_NETWORK_SIZE, SMALL_VIEW_SIZE);
	}

	private void assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(final int networkSize, final int viewSize) {
		Network.log(String.format("Setting up network: %d nodes, each node queries %d partners.", networkSize, viewSize));
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		final Network network = setupNetwork("network", networkSize, viewSize, settings);
		network.advanceInTime(15 * Network.MINUTE, Network.MINUTE);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
	}

	/**
	 * Tests of convergence in a friendly but more realistic environment.
	 */
	@Test
	public void asymmetricChannelsDoNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 2 * Network.HOUR, 10 * Network.MINUTE);
	}

	@Test
	public void remoteReceiveSendDelayDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 2 * Network.HOUR, 10 * Network.MINUTE);
	}

	@Test
	public void instableClockWithoutPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	@Test
	public void instableClockTogetherWithPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	@Test
	public void realisticFriendlyEnvironmentDoesNotProduceShift() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				INSTABLE_CLOCK,
				CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	private void assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(
			final NodeSettings settings,
			final long timeInterval,
			final long loggingInterval) {
		final Network network = setupNetwork("network", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		network.advanceInTime(timeInterval/2, loggingInterval);
		final double mean = network.calculateMean();
		network.advanceInTime(timeInterval/2, loggingInterval);
		Network.log("Final state of network:");
		network.updateStatistics();
		network.logStatistics();
		final double shiftPerDay = 2 * Math.abs(mean - network.calculateMean()) / timeInterval * Network.DAY;
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		Network.log(String.format("%s time shift per day %sms.", network.getName(), format.format(shiftPerDay)));
		network.outputOutOfRangeNodes(TOLERABLE_MAX_DEVIATION_FROM_MEAN);
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(shiftPerDay) < TOLERABLE_MEAN_TIME_SHIFT_PER_DAY, IsEqual.equalTo(true));
	}

	/**
	 * Tests to assure new friendly nodes joining the network don't disrupt the network time.
	 */
	@Test
	public void smallPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		assertNewNodesDoNotHaveBigInfluenceOnNetworkTime(Network.DAY, Network.HOUR, 10);
	}

	@Test
	public void mediumPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		assertNewNodesDoNotHaveBigInfluenceOnNetworkTime(Network.DAY, Network.HOUR, 50);
	}

	@Test
	public void highPercentageOfNewNodesDoesNotHaveBigInfluenceOnNetworkTime() {
		assertNewNodesDoNotHaveBigInfluenceOnNetworkTime(2 * Network.HOUR, 5 * Network.MINUTE, 300);
	}

	private void assertNewNodesDoNotHaveBigInfluenceOnNetworkTime(
			final long timeInterval,
			final long loggingInterval,
			final int percentageOfNewNodes) {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		final Network network = setupNetwork("network", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		network.advanceInTime(timeInterval/2, 0);
		Network.log("Matured network statistics:");
		network.updateStatistics();
		network.logStatistics();
		final double oldMean = network.calculateMean();
		Network.log(String.format("Adding %d%% new nodes.", percentageOfNewNodes));
		network.grow(percentageOfNewNodes);
		network.advanceInTime(timeInterval/2, loggingInterval);
		Network.log("Final state of network:");
		network.updateStatistics();
		network.logStatistics();
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(oldMean - network.calculateMean()) < TOLERABLE_CHANGE_IN_MEAN, IsEqual.equalTo(true));
	}

	/**
	 * Tests to assure that if there are several sub-networks which have different network times and
	 * those networks finally join, the resulting network will converge to a common network time.
	 */
	@Test
	public void networkTimeConvergesToCommonNetworkTimeWhenTwoNetworksJoin() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		final Network network1 = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		final Network network2 = setupNetwork("network2", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);

		// Make sure the two networks have very different network times.
		network2.randomShiftNetworkTime();
		network1.advanceInTime(6 * Network.HOUR, 0);
		network2.advanceInTime(6 * Network.HOUR, 0);
		Network.log("Matured network statistics:");
		network1.updateStatistics();
		network1.logStatistics();
		network2.updateStatistics();
		network2.logStatistics();
		Network.log("Networks join.");
		network1.join(network2, "global network");

		// Since both networks were already mature, it needs more rounds to converge
		network1.advanceInTime(Network.DAY, 2 * Network.HOUR);
		Network.log("Final state of network:");
		network1.updateStatistics();
		network1.logStatistics();
		Assert.assertThat(network1.hasConverged(), IsEqual.equalTo(true));
	}

	@Test
	public void networkTimeConvergesToCommonNetworkTimeWhenFiveNetworksJoin() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES);
		final List<Network> networks = Arrays.asList(
				setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network2", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network3", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network4", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network5", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings));

		// Make sure the networks have very different network times.
		networks.stream().forEach(Network::randomShiftNetworkTime);
		networks.stream().forEach(network -> network.advanceInTime(6 * Network.HOUR, 0));
		Network.log("Matured network statistics:");
		networks.stream().forEach(network -> { network.updateStatistics(); network.logStatistics(); });
		Network.log("Networks join.");
		networks.stream().forEach(network -> networks.get(0).join(network, "global network"));

		// Since both networks were already mature, it needs more rounds to converge
		networks.get(0).advanceInTime(4 * Network.DAY, 8 * Network.HOUR);
		Network.log("Final state of network:");
		networks.get(0).updateStatistics();
		networks.get(0).logStatistics();
		Assert.assertThat(networks.get(0).hasConverged(), IsEqual.equalTo(true));
	}

	/**
	 * Tests to assure that the network time cannot be influenced by a reasonable amount of attackers.
	 */
	@Test
	public void smallPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(5);
	}

	@Test
	public void mediumPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(15);
	}

	private void assertAttackersDoNotInfluenceNetworkTime(final int percentageEvilNodes) {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!INSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				percentageEvilNodes);
		final Network network = setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		network.advanceInTime(Network.DAY, 4 * Network.HOUR);
		final double mean = network.calculateMean();
		network.advanceInTime(Network.DAY, 4 * Network.HOUR);
		final double shiftPerDay = Math.abs(mean - network.calculateMean());
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		Network.log(String.format("%s time shift per day %sms.", network.getName(), format.format(shiftPerDay)));
		Assert.assertThat(network.hasConverged(), IsEqual.equalTo(true));
		Assert.assertThat(Math.abs(shiftPerDay) < TOLERABLE_MEAN_TIME_SHIFT_PER_DAY, IsEqual.equalTo(true));
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
			final boolean clockAdjustment,
			final int percentageEvilNodes) {
		return new NodeSettings(
				timeOffsetSpread,
				delayCommunication,
				asymmetricChannels,
				instableClock,
				clockAdjustment,
				percentageEvilNodes);
	}
}
