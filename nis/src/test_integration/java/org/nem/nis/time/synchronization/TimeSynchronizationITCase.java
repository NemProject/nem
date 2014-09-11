package org.nem.nis.time.synchronization;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.utils.FormatUtils;

import java.text.DecimalFormat;
import java.util.*;

public class TimeSynchronizationITCase {
	private static final int STANDARD_NETWORK_SIZE = 500;
	private static final int INITIAL_TIME_SPREAD = 50000;
	private static final boolean REMOTE_RECEIVE_SEND_DELAY = true;
	private static final boolean ASYMMETRIC_CHANNELS = true;
	private static final boolean UNSTABLE_CLOCK = true;
	private static final int MAX_VIEW_SIZE = STANDARD_NETWORK_SIZE;
	private static final int MEDIUM_VIEW_SIZE = 60;
	private static final int SMALL_VIEW_SIZE = 5;
	private static final boolean CLOCK_ADJUSTMENT = true;
	private static final int NO_EVIL_NODES = 0;
	private static final double EVIL_NODES_ZERO_IMPORTANCE = 0.0;
	private static final double DEFAULT_EVIL_NODES_CUMULATIVE_IMPORTANCE = 0.05;

	/**
	 * Maximal tolerable shift of the mean network time per day in milli seconds.
	 */
	private static final double TOLERABLE_MEAN_TIME_SHIFT_PER_DAY = 50;

	/**
	 * Value indicating how much a node's network time is allowed to deviate
	 * from the mean network time before it is considered as faulty.
	 */
	private static final long TOLERABLE_MAX_DEVIATION_FROM_MEAN = 5000;

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
		assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(7 * STANDARD_NETWORK_SIZE, SMALL_VIEW_SIZE);
	}

	private void assertNetworkTimeConvergesInFriendlyAndIdealEnvironment(final int networkSize, final int viewSize) {
		Network.log(String.format("Setting up network: %d nodes, each node queries %d partners.", networkSize, viewSize));
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
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
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 2 * Network.HOUR, 10 * Network.MINUTE);
	}

	@Test
	public void remoteReceiveSendDelayDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 2 * Network.HOUR, 10 * Network.MINUTE);
	}

	@Test
	public void unstableClockWithoutPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	@Test
	public void unstableClockTogetherWithPeriodicClockAdjustmentDoesNotProduceShiftInFriendlyEnvironment() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				UNSTABLE_CLOCK,
				CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	@Test
	public void realisticFriendlyEnvironmentDoesNotProduceShift() {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				REMOTE_RECEIVE_SEND_DELAY,
				ASYMMETRIC_CHANNELS,
				UNSTABLE_CLOCK,
				CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(settings, 30 * Network.DAY, Network.DAY);
	}

	private void assertNetworkTimeConvergesAndDoesNotShiftInFriendlyButRealisticEnvironment(
			final NodeSettings settings,
			final long timeInterval,
			final long loggingInterval) {
		final Network network = setupNetwork("network", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		network.advanceInTime(timeInterval / 2, loggingInterval);
		final double mean = network.calculateMean();
		network.advanceInTime(timeInterval / 2, loggingInterval);
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
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		final Network network = setupNetwork("network", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
		network.advanceInTime(timeInterval / 2, 0);
		Network.log("Matured network statistics:");
		network.updateStatistics();
		network.logStatistics();
		final double oldMean = network.calculateMean();
		Network.log(String.format("Adding %d%% new nodes.", percentageOfNewNodes));
		network.grow(percentageOfNewNodes);
		network.advanceInTime(timeInterval / 2, loggingInterval);
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
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
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
		network1.advanceInTime(2 * Network.DAY, 4 * Network.HOUR);
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
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				NO_EVIL_NODES,
				EVIL_NODES_ZERO_IMPORTANCE);
		final List<Network> networks = Arrays.asList(
				setupNetwork("network1", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network2", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network3", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network4", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings),
				setupNetwork("network5", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings));

		// Make sure the networks have very different network times.
		Network.log("Setting up 5 networks...");
		networks.stream().forEach(Network::randomShiftNetworkTime);
		networks.stream().forEach(network -> network.advanceInTime(6 * Network.HOUR, 0));
		Network.log("Matured network statistics:");
		networks.stream().forEach(network -> {
			network.updateStatistics();
			network.logStatistics();
		});
		Network.log("Networks join...");
		networks.stream().forEach(network -> networks.get(0).join(network, "global network"));

		// Since both networks were already mature, it needs more rounds to converge
		networks.get(0).advanceInTime(4 * Network.DAY, 8 * Network.HOUR);
		Network.log("Final state of network:");
		networks.get(0).updateStatistics();
		networks.get(0).logStatistics();
		Assert.assertThat(networks.get(0).hasConverged(), IsEqual.equalTo(true));
	}

	/**
	 * Tests to assure that the network time cannot be influenced by attackers that control a reasonable amount of NEM.
	 */
	@Test
	public void verySmallPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(5);
	}

	@Test
	public void smallPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(15);
	}

	@Test
	public void mediumPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(30);
	}

	@Test
	public void highPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(60);
	}

	@Test
	public void veryHighPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(90);
	}

	@Test
	public void insanelyHighPercentageOfAttackersDoesNotInfluenceNetworkTime() {
		assertAttackersDoNotInfluenceNetworkTime(99);
	}

	private void assertAttackersDoNotInfluenceNetworkTime(final int percentageEvilNodes) {
		final NodeSettings settings = createNodeSettings(
				INITIAL_TIME_SPREAD,
				!REMOTE_RECEIVE_SEND_DELAY,
				!ASYMMETRIC_CHANNELS,
				!UNSTABLE_CLOCK,
				!CLOCK_ADJUSTMENT,
				percentageEvilNodes,
				DEFAULT_EVIL_NODES_CUMULATIVE_IMPORTANCE);
		final Network network = setupNetwork("network", STANDARD_NETWORK_SIZE, MEDIUM_VIEW_SIZE, settings);
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
		return new Network(name, numberOfNodes, viewSize, nodeSettings);
	}

	private NodeSettings createNodeSettings(
			final int timeOffsetSpread,
			final boolean delayCommunication,
			final boolean asymmetricChannels,
			final boolean unstableClock,
			final boolean clockAdjustment,
			final int percentageEvilNodes,
			final double evilNodesCumulativeImportance) {
		return new NodeSettings(
				timeOffsetSpread,
				delayCommunication,
				asymmetricChannels,
				unstableClock,
				clockAdjustment,
				percentageEvilNodes,
				evilNodesCumulativeImportance);
	}
}
