package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.*;
import org.nem.core.time.NetworkTimeStamp;
import org.nem.core.time.synchronization.*;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.cache.*;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.NisUtils;
import org.nem.nis.time.synchronization.filter.*;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Network {
	private static final Logger LOGGER = Logger.getLogger(Network.class.getName());
	private static final double TOLERABLE_MAX_STANDARD_DEVIATION = 2000;
	private static final long SECOND = 1000;
	public static final long MINUTE = 60 * SECOND;
	public static final long HOUR = 60 * MINUTE;
	public static final long DAY = 24 * HOUR;
	private static final long TICK_INTERVAL = MINUTE;
	private static final long CLOCK_ADJUSTMENT_INTERVAL = DAY;
	private static final BlockHeight HEIGHT = new BlockHeight(10);

	/**
	 * Start value for the update interval of clocks in milli seconds.
	 */
	private static final long UPDATE_INTERVAL_START = 60000;

	/**
	 * The maximal value for the update interval of clocks in milli seconds.
	 */
	private static final long UPDATE_INTERVAL_MAXIMUM = 180 * UPDATE_INTERVAL_START;

	/**
	 * Value that indicates after which round the update interval elongation starts.
	 */
	private static final long START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND = 5;

	/**
	 * Value that indicates how fast the update interval grows.
	 */
	private static final double UPDATE_INTERVAL_ELONGATION_STRENGTH = 0.1;

	private String name;
	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private int nodeId = 1;
	private final int viewSize;
	private final NodeSettings nodeSettings;
	private TimeSynchronizationStrategy syncStrategy;
	private AccountStateCache accountStateCache;
	private PoxFacade poxFacade;
	private long realTime = 0;
	private double mean;
	private double standardDeviation;
	private double maxDeviationFromMean;
	private boolean hasConverged;

	/**
	 * Creates a network for simulation purposes.
	 *
	 * @param name The name of the network.
	 * @param networkSize The number of nodes in the network.
	 * @param viewSize The view size of the nodes in the network.
	 * @param nodeSettings The node settings.
	 */
	public Network(
			final String name,
			final int networkSize,
			final int viewSize,
			final NodeSettings nodeSettings) {
		this.name = name;
		this.viewSize = viewSize;
		this.nodeSettings = nodeSettings;
		this.accountStateCache = new DefaultAccountStateCache().copy();
		this.poxFacade = new DefaultPoxFacade(NisUtils.createImportanceCalculator());
		this.syncStrategy = this.createSynchronizationStrategy();
		long cumulativeInaccuracy = 0;
		int numberOfEvilNodes = 0;
		for (int i = 1; i <= networkSize; i++) {
			final TimeAwareNode node = this.createNode();
			this.nodes.add(node);
			cumulativeInaccuracy += node.getClockInaccuary().getRaw();
			if (node.isEvil()) {
				numberOfEvilNodes++;
			}
		}
		this.initialize();
		if (this.nodeSettings.hasUnstableClock()) {
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			log(String.format(
					"%s: mean clock inaccuracy per day: %s ms.",
					this.getName(),
					format.format(24.0 * (double)cumulativeInaccuracy / (double)this.nodes.size())));
		}
		if (numberOfEvilNodes > 0) {
			log(String.format("%d%% of all nodes are evil.", (100 * numberOfEvilNodes) / this.nodes.size()));
		}
	}

	/**
	 * Gets the name of the network.
	 *
	 * @return The name of the network.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the set of nodes.
	 *
	 * @return The set of nodes.
	 */
	private Set<TimeAwareNode> getNodes() {
		return this.nodes;
	}

	/**
	 * Gets a value indicating if the network time of all nodes has converged.
	 *
	 * @return true if it has converged, false otherwise.
	 */
	public boolean hasConverged() {
		return this.hasConverged;
	}

	private TimeSynchronizationStrategy createSynchronizationStrategy() {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(
				new ResponseDelayDetectionFilter(),
				new ClampingFilter(),
				new AlphaTrimmedMeanFilter()));
		return new DefaultTimeSynchronizationStrategy(filter, this.poxFacade, this.accountStateCache, (o, s) -> { });
	}

	/**
	 * Creates a new time aware node.
	 *
	 * @return The node.
	 */
	private TimeAwareNode createNode() {
		return new TimeAwareNode(
				this.nodeId++,
				new NodeAge(0),
				this.syncStrategy,
				new TimeOffset(this.random.nextInt(this.nodeSettings.getTimeOffsetSpread() + 1) - this.nodeSettings.getTimeOffsetSpread() / 2),
				this.nodeSettings.doesDelayCommunication() ? new TimeOffset(this.random.nextInt(100)) : new TimeOffset(0),
				this.nodeSettings.hasAsymmetricChannels() ? this.random.nextDouble() : 0.5,
				this.nodeSettings.hasUnstableClock() ? new TimeOffset(this.random.nextInt(201) - 100) : new TimeOffset(0),
				this.random.nextInt(100) < this.nodeSettings.getPercentageEvilNodes() ? TimeAwareNode.NODE_TYPE_EVIL : TimeAwareNode.NODE_TYPE_FRIENDLY);
	}

	/**
	 * Creates a new time aware node from an existing one.
	 *
	 * @param oldNode The existing node.
	 * @return The node.
	 */
	private TimeAwareNode createNode(final TimeAwareNode oldNode) {
		return new TimeAwareNode(
				this.nodeId++,
				oldNode.getAge(),
				this.syncStrategy,
				oldNode.getTimeOffset(),
				oldNode.getCommunicationDelay(),
				oldNode.getChannelAsymmetry(),
				oldNode.getClockInaccuary(),
				oldNode.isEvil() ? TimeAwareNode.NODE_TYPE_EVIL : TimeAwareNode.NODE_TYPE_FRIENDLY);
	}

	/**
	 * Advances in time doing all needed updates for the nodes.
	 *
	 * @param timeInterval The time interval.
	 * @param loggingInterval The logging interval.
	 */
	public void advanceInTime(final long timeInterval, final long loggingInterval) {
		final int numberOfTicks = (int)(timeInterval / TICK_INTERVAL);
		final int ticksUntilLog = (int)(loggingInterval / TICK_INTERVAL);
		final int ticksUntilClockAdjustment = (int)(CLOCK_ADJUSTMENT_INTERVAL / this.nodes.size() / TICK_INTERVAL);
		final int ticksUntilClockInaccuracy = (int)(HOUR / TICK_INTERVAL);
		for (int i = 0; i < numberOfTicks; i++) {
			if (loggingInterval > 0 && i % ticksUntilLog == 0) {
				this.updateStatistics();
				this.logStatistics();
			}
			if (this.nodeSettings.hasClockAdjustment() && i % ticksUntilClockAdjustment == 0) {
				this.clockAdjustment();
			}
			if (this.nodeSettings.hasUnstableClock() && i % ticksUntilClockInaccuracy == 0) {
				this.nodes.stream().forEach(TimeAwareNode::applyClockInaccuracy);
			}
			this.tick();
		}
	}

	private void tick() {
		this.realTime += TICK_INTERVAL;
		final List<TimeAwareNode> nodesToUpdate = this.getNodesToUpdate(TICK_INTERVAL);
		nodesToUpdate.stream().forEach(n -> {
			Set<TimeAwareNode> partners = this.selectSyncPartnersForNode(n);
			List<TimeSynchronizationSample> samples = this.createSynchronizationSamples(n, partners);
			n.updateNetworkTime(samples);
		});
	}

	/**
	 * Gets the list of nodes that have to update they network time.
	 *
	 * @param timePassed The time that has passed since the last call.
	 * @return The list of nodes.
	 */
	private List<TimeAwareNode> getNodesToUpdate(final long timePassed) {
		final List<TimeAwareNode> nodesToUpdate = new ArrayList<>();
		this.nodes.stream().forEach(n -> {
			if (n.decrementUpdateCounter(timePassed) < 0) {
				n.setUpdateCounter(this.getUpdateInterval(n.getAge()));
				nodesToUpdate.add(n);
			}
		});

		return nodesToUpdate;
	}

	/**
	 * Gets the update interval for a node based on its age.
	 *
	 * @param age The age of the node.
	 * @return The update interval in milli seconds.
	 */
	private long getUpdateInterval(final NodeAge age) {
		if (START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND > age.getRaw()) {
			return UPDATE_INTERVAL_START;
		}
		final long ageToUse = age.getRaw() - START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND;
		return (long)Math.min(UPDATE_INTERVAL_START +
				(UPDATE_INTERVAL_MAXIMUM - UPDATE_INTERVAL_START) *
						ageToUse * UPDATE_INTERVAL_ELONGATION_STRENGTH, UPDATE_INTERVAL_MAXIMUM);
	}

	private void resetCache() {
		this.accountStateCache = new DefaultAccountStateCache().copy();
		this.poxFacade = new DefaultPoxFacade(NisUtils.createImportanceCalculator());
		this.syncStrategy = this.createSynchronizationStrategy();
		final Set<TimeAwareNode> oldNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		oldNodes.addAll(this.nodes);
		this.nodes.clear();
		this.nodeId = 1;
		oldNodes.stream().forEach(n -> this.nodes.add(this.createNode(n)));
	}

	/**
	 * Updates the POI facade with the nodes importance.
	 */
	private void initialize() {
		// We assume that evil nodes have significant lower cumulative importance than friendly nodes.
		final int numberOfEvilNodes = (this.nodes.size() * this.nodeSettings.getPercentageEvilNodes()) / 100;
		for (final TimeAwareNode node : this.nodes) {
			final double importance = node.isEvil() ?
					this.nodeSettings.getEvilNodesCumulativeImportance() / numberOfEvilNodes :
					(1.0 - this.nodeSettings.getEvilNodesCumulativeImportance()) / (this.nodes.size() - numberOfEvilNodes);
			final AccountState state = this.accountStateCache.findStateByAddress(node.getNode().getIdentity().getAddress());
			state.getImportanceInfo().setImportance(HEIGHT, importance);
		}
		this.setFacadeLastVectorSize(this.poxFacade, this.nodes.size());
	}

	private void setFacadeLastVectorSize(final PoxFacade facade, final int lastVectorSize) {
		try {
			final Field field = DefaultPoxFacade.class.getDeclaredField("lastVectorSize");
			field.setAccessible(true);
			field.set(facade, lastVectorSize);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException("Exception in setFacadeLastVectorSize");
		}
	}

	/**
	 * Grows the network by a given percentage.
	 *
	 * @param percentage The percentage to grow the network.
	 */
	public void grow(final double percentage) {
		this.resetCache();
		final int numberOfNewNodes = (int)(this.nodes.size() * percentage / 100);
		for (int i = 1; i <= numberOfNewNodes; i++) {
			this.nodes.add(this.createNode());
		}
		this.initialize();
	}

	/**
	 * Incorporates the nodes from network into this network.
	 *
	 * @param network The network that joins this network.
	 * @param newName The new name for the this network.
	 */
	public void join(final Network network, final String newName) {
		this.resetCache();
		network.getNodes().stream().forEach(n -> this.nodes.add(this.createNode(n)));
		this.name = newName;
		this.initialize();
	}

	/**
	 * Shifts the network time of all nodes by a common random offset.
	 */
	public void randomShiftNetworkTime() {
		final TimeOffset offset = new TimeOffset(this.random.nextInt(40001));
		this.nodes.stream().forEach(n -> n.shiftTimeOffset(offset));
	}

	/**
	 * Selects a set of nodes as communication partners  for a given node.
	 * The algorithm is different from the one used by ImportanceAwareNodeSelector.
	 * It does not used trust values, only importances.
	 *
	 * @param node The node to select partners for.
	 * @return The set of communication partners.
	 */
	private Set<TimeAwareNode> selectSyncPartnersForNode(final TimeAwareNode node) {
		final Set<TimeAwareNode> partners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		final TimeAwareNode[] nodeArray = this.nodes.toArray(new TimeAwareNode[this.nodes.size()]);
		final int maxTries = 1000;
		int tries = 0;
		int hits = 0;
		while (tries < maxTries && hits < this.viewSize) {
			final int index = this.random.nextInt(this.nodes.size());
			final AccountState state = this.accountStateCache.findStateByAddress(nodeArray[index].getNode().getIdentity().getAddress());
			if (!nodeArray[index].equals(node) && this.isEligiblePartner(state)) {
				hits++;
				partners.add(nodeArray[index]);
			}
			tries++;
		}

		if (partners.isEmpty()) {
			// There might be just too many evil nodes. Let's assume we use one pretrusted node if we meet this case in a real environment.
			for (int i = 0; i < this.nodes.size(); i++) {
				final AccountState state = this.accountStateCache.findStateByAddress(nodeArray[i].getNode().getIdentity().getAddress());
				if (!nodeArray[i].equals(node) && this.isEligiblePartner(state)) {
					partners.add(nodeArray[i]);
					break;
				}
			}
		}

		return partners;
	}

	private boolean isEligiblePartner(final AccountState state) {
		final double adjustedMinimumImportance = TimeSynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE * 500 / this.nodes.size();
		return adjustedMinimumImportance < state.getImportanceInfo().getImportance(HEIGHT);
	}

	/**
	 * Creates for all partners of node a synchronization sample.
	 *
	 * @param node The node to create the sample for.
	 * @param partners The node's partners.
	 * @return The list of samples.
	 */
	private List<TimeSynchronizationSample> createSynchronizationSamples(final TimeAwareNode node, final Set<TimeAwareNode> partners) {
		final List<TimeSynchronizationSample> samples = new ArrayList<>();
		for (final TimeAwareNode partner : partners) {
			final int roundTripTime = this.random.nextInt(1000);
			final NetworkTimeStamp localSend = node.getNetworkTime();
			final NetworkTimeStamp localReceive = new NetworkTimeStamp(
					node.getNetworkTime().getRaw() + partner.getCommunicationDelay().getRaw() + roundTripTime);
			samples.add(new TimeSynchronizationSample(
					partner.getNode(),
					new CommunicationTimeStamps(localSend, localReceive),
					partner.createCommunicationTimeStamps(roundTripTime)));
		}

		return samples;
	}

	/**
	 * It's reasonable to assume that the computers in the network adjust their clock via NTP every now and then.
	 * We assume here that this happens about every day.
	 */
	private void clockAdjustment() {
		if (this.nodeSettings.hasClockAdjustment()) {
			final TimeAwareNode[] nodeArray = this.nodes.toArray(new TimeAwareNode[this.nodes.size()]);
			nodeArray[this.random.nextInt(this.nodes.size())].adjustClock();
		}
	}

	/**
	 * Calculates the mean value for the time offsets.
	 *
	 * @return The mean value.
	 */
	public double calculateMean() {
		return this.nodes.stream().mapToDouble(n -> n.getTimeOffset().getRaw()).sum() / this.nodes.size();
	}

	/**
	 * Calculates the standard deviation for the time offsets.
	 *
	 * @return The standard deviation.
	 */
	private double calculateStandardDeviation() {
		return Math.sqrt(this.nodes.stream().mapToDouble(n -> Math.pow(n.getTimeOffset().getRaw() - this.mean, 2)).sum() / this.nodes.size());
	}

	/**
	 * Calculates the maximum deviation from the mean value.
	 *
	 * @return The maximum deviation from the mean value.
	 */
	private double calculateMaxDeviationFromMean() {
		final OptionalDouble value = this.nodes.stream().mapToDouble(n -> Math.abs(n.getTimeOffset().getRaw() - this.mean)).max();
		return value.isPresent() ? value.getAsDouble() : Double.NaN;
	}

	/**
	 * Updates the statistical values.
	 */
	public void updateStatistics() {
		this.mean = this.calculateMean();
		this.standardDeviation = this.calculateStandardDeviation();
		this.maxDeviationFromMean = this.calculateMaxDeviationFromMean();
		this.hasConverged = this.standardDeviation < TOLERABLE_MAX_STANDARD_DEVIATION;
	}

	/**
	 * Log mean, standard deviation and maximum deviation from mean.
	 */
	public void logStatistics() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final String entry = String.format(
				"%s : %s mean offset to real time: %sms, standard deviation: %sms, max. deviation from mean: %sms",
				this.getRealTimeString(),
				this.getName(),
				format.format(this.mean),
				format.format(this.standardDeviation),
				format.format(this.maxDeviationFromMean));
		log(entry);
	}

	private String getRealTimeString() {
		final long day = this.realTime / 86400000;
		final long hour = (this.realTime - day * 86400000) / 3600000;
		final long minute = (this.realTime - day * 86400000 - hour * 3600000) / 60000;
		final long second = (this.realTime - day * 86400000 - hour * 3600000 - minute * 60000) / 1000;
		return String.format("Day %d %02d:%02d:%02d", day, hour, minute, second);
	}

	public void outputOutOfRangeNodes(final long maxTolerableDeviationFromMean) {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final List<TimeAwareNode> outOfRangeNodes = this.nodes.stream()
				.filter(n -> Math.abs(this.mean - n.getTimeOffset().getRaw()) > maxTolerableDeviationFromMean)
				.collect(Collectors.toList());
		if (!outOfRangeNodes.isEmpty()) {
			log("Detected nodes with out of allowed range network time:");
			outOfRangeNodes.stream().forEach(n -> log(String.format("%s: time offset from mean=%s",
					n.getName(),
					format.format(this.mean - n.getTimeOffset().getRaw()))));
		}
	}

	public static void log(final String entry) {
		// BR: Those red logger lines are killing my eyes...
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			System.out.println(entry);
		} else {
			LOGGER.info(entry);
		}
	}
}
