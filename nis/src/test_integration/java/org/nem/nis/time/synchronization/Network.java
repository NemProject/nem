package org.nem.nis.time.synchronization;

import org.mockito.Mockito;
import org.nem.core.model.primitive.*;
import org.nem.core.utils.FormatUtils;
import org.nem.nis.poi.*;
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
	public static final long SECOND = 1000;
	public static final long MINUTE = 60 * SECOND;
	public static final long HOUR = 60 * MINUTE;
	public static final long DAY = 24 * HOUR;
	private static final long TICK_INTERVALL = MINUTE;
	private static final long CLOCK_ADJUSTMENT_INTERVALL = DAY;
	private static final BlockHeight HEIGHT = new BlockHeight(10);

	private String name;
	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private int nodeId = 1;
	private final int viewSize;
	private final NodeSettings nodeSettings;
	private SynchronizationStrategy syncStrategy;
	private PoiFacade poiFacade;
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
		this.poiFacade = new PoiFacade(Mockito.mock(PoiImportanceGenerator.class));
		this.syncStrategy = createSynchronizationStrategy(poiFacade);
		long cumulativeInaccuracy = 0;
		int numberOfEvilNodes = 0;
		for (int i=1; i<=networkSize; i++) {
			TimeAwareNode node = createNode();
			this.nodes.add(node);
			cumulativeInaccuracy += node.getClockInaccuary().getRaw();
			if (node.isEvil()) {
				numberOfEvilNodes++;
			}
		}
		this.initializeFacade(poiFacade);
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
	public Set<TimeAwareNode> getNodes() {
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

	private SynchronizationStrategy createSynchronizationStrategy(final PoiFacade facade) {
		final SynchronizationFilter filter = new AggregateSynchronizationFilter(Arrays.asList(new ClampingFilter(), new AlphaTrimmedMeanFilter()));
		return new DefaultSynchronizationStrategy(filter, facade);
	}

	/**
	 * Creates a new time aware node.
	 *
	 * @return The node.
	 */
	private TimeAwareNode createNode() {
		TimeAwareNode node = new TimeAwareNode(
				this.nodeId++,
				new NodeAge(0),
				this.syncStrategy,
				new TimeOffset(this.random.nextInt(nodeSettings.getTimeOffsetSpread() + 1) - this.nodeSettings.getTimeOffsetSpread()/2),
				this.nodeSettings.doesDelayCommunication()? new TimeOffset(this.random.nextInt(100)) : new TimeOffset(0),
				this.nodeSettings.hasAsymmetricChannels()? this.random.nextDouble() : 0.5,
				this.nodeSettings.hasUnstableClock()? new TimeOffset(this.random.nextInt(201) - 100) : new TimeOffset(0),
				this.random.nextInt(100) < this.nodeSettings.getPercentageEvilNodes()? TimeAwareNode.NODE_TYPE_EVIL : TimeAwareNode.NODE_TYPE_FRIENDLY);

		return node;
	}

	/**
	 * Creates a new time aware node from an existing one.
	 *
	 * @param oldNode The existing node.
	 * @return The node.
	 */
	private TimeAwareNode createNode(TimeAwareNode oldNode) {
		TimeAwareNode node = new TimeAwareNode(
				this.nodeId++,
				oldNode.getAge(),
				this.syncStrategy,
				oldNode.getTimeOffset(),
				oldNode.getCommunicationDelay(),
				oldNode.getChannelAsymmetry(),
				oldNode.getClockInaccuary(),
				oldNode.isEvil()? TimeAwareNode.NODE_TYPE_EVIL : TimeAwareNode.NODE_TYPE_FRIENDLY);

		return node;
	}

	/**
	 * Advances in time doing all needed updates for the nodes.
	 *
	 * @param timeInterval The time interval.
	 * @param loggingInterval The logging interval.
	 */
	public void advanceInTime(final long timeInterval, final long loggingInterval) {
		final int numberOfTicks = (int)(timeInterval / TICK_INTERVALL);
		final int ticksUntilLog = (int)(loggingInterval / TICK_INTERVALL);
		final int ticksUntilClockAdjustment = (int)(CLOCK_ADJUSTMENT_INTERVALL / nodes.size() / TICK_INTERVALL);
		final int ticksUntilClockInaccuracy = (int)(HOUR / TICK_INTERVALL);
		for (int i=0; i<numberOfTicks; i++) {
			if (loggingInterval > 0 && i % ticksUntilLog == 0) {
				updateStatistics();
				logStatistics();
			}
			if (nodeSettings.hasClockAdjustment() && i % ticksUntilClockAdjustment == 0) {
				clockAdjustment();
			}
			if (nodeSettings.hasUnstableClock() && i % ticksUntilClockInaccuracy == 0) {
				this.nodes.stream().forEach(TimeAwareNode::applyClockInaccuracy);
			}
			tick();
		}
	}

	public void tick() {
		this.realTime += TICK_INTERVALL;
		final List<TimeAwareNode> nodesToUpdate = getNodesToUpdate(TICK_INTERVALL);
		nodesToUpdate.stream().forEach(n -> {
			Set<TimeAwareNode> partners = selectSyncPartnersForNode(n);
			List<SynchronizationSample> samples = createSynchronizationSamples(n, partners);
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
				n.setUpdateCounter(getUpdateInterval(n.getAge()));
				nodesToUpdate.add(n);
			}
		});

		return nodesToUpdate;
	}

	private void testUpdateIntervall() {
		for (int i=0; i<20; i++) {
			log(String.format("Age %d: %ds", i, getUpdateInterval(new NodeAge(i))/1000));
		}
	}
	/**
	 * Gets the update interval for a node based on its age.
	 *
	 * @param age The age of the node.
	 * @return The update interval in milli seconds.
	 */
	private long getUpdateInterval(NodeAge age) {
		if (SynchronizationConstants.START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND > age.getRaw()) {
			return SynchronizationConstants.UPDATE_INTERVAL_START;
		}
		final long ageToUse = age.getRaw() - SynchronizationConstants.START_UPDATE_INTERVAL_ELONGATION_AFTER_ROUND;
		return (long)Math.min(SynchronizationConstants.UPDATE_INTERVAL_START +
				(SynchronizationConstants.UPDATE_INTERVAL_MAXIMUM - SynchronizationConstants.UPDATE_INTERVAL_START) *
						ageToUse * SynchronizationConstants.UPDATE_INTERVAL_ELONGATION_STRENGTH, SynchronizationConstants.UPDATE_INTERVAL_MAXIMUM);
	}

	private PoiFacade resetFacade() {
		this.poiFacade = new PoiFacade(Mockito.mock(PoiImportanceGenerator.class));
		this.syncStrategy = createSynchronizationStrategy(poiFacade);
		final Set<TimeAwareNode> oldNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		oldNodes.addAll(this.nodes);
		this.nodes.clear();
		this.nodeId = 1;
		oldNodes.stream().forEach(n -> this.nodes.add(createNode(n)));
		return poiFacade;
	}

	/**
	 * Updates the POI facade with the nodes importance.
	 *
	 * @param facade The POI facade.
	 */
	private void initializeFacade(final PoiFacade facade) {
		// We assume that evil nodes have significant lower cumulative importance than friendly nodes.
		final int numberOfEvilNodes = (this.nodes.size() * this.nodeSettings.getPercentageEvilNodes()) / 100;
		for (TimeAwareNode node : this.nodes) {
			final double importance = node.isEvil() ?
					this.nodeSettings.getEvilNodesCumulativeImportance() / numberOfEvilNodes :
					(1.0 - this.nodeSettings.getEvilNodesCumulativeImportance()) / (this.nodes.size() - numberOfEvilNodes);
			PoiAccountState state = facade.findStateByAddress(node.getNode().getIdentity().getAddress());
			state.getImportanceInfo().setImportance(HEIGHT, importance);
		}
		this.setFacadeLastPoiVectorSize(facade, this.nodes.size());
	}

	private void setFacadeLastPoiVectorSize(PoiFacade facade, final int lastPoiVectorSize) {
		try {
			Field field = PoiFacade.class.getDeclaredField("lastPoiVectorSize");
			field.setAccessible(true);
			field.set(facade, lastPoiVectorSize);
		} catch(IllegalAccessException | NoSuchFieldException e) {
			throw new RuntimeException("Exception in setFacadeLastPoiVectorSize");
		}
	}

	/**
	 * Grows the network by a given percentage.
	 *
	 * @param percentage The percentage to grow the network.
	 */
	public void grow(final double percentage) {
		this.resetFacade();
		final int numberOfNewNodes = (int)(nodes.size() * percentage / 100);
		for (int i=1; i<=numberOfNewNodes; i++) {
			this.nodes.add(createNode());
		}
		this.initializeFacade(this.poiFacade);
	}

	/**
	 * Incorporates the nodes from network into this network.
	 *
	 * @param network The network that joins this network.
	 * @param newName The new name for the this network.
	 */
	public void join(final Network network, final String newName) {
		this.resetFacade();
		network.getNodes().stream().forEach(n -> this.nodes.add(createNode(n)));
		this.name = newName;
		this.initializeFacade(this.poiFacade);
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
	public Set<TimeAwareNode> selectSyncPartnersForNode(final TimeAwareNode node) {
		int numberOfEvilNodes = 0;
		final Set<TimeAwareNode> partners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		final TimeAwareNode[] nodeArray = this.nodes.toArray(new TimeAwareNode[this.nodes.size()]);
		final int maxTries = 1000;
		int tries = 0;
		int hits = 0;
		while (tries < maxTries && hits < this.viewSize) {
			final int index = random.nextInt(this.nodes.size());
			final PoiAccountState state = this.poiFacade.findStateByAddress(nodeArray[index].getNode().getIdentity().getAddress());
			if (!nodeArray[index].equals(node) &&
				SynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE < state.getImportanceInfo().getImportance(HEIGHT)) {
				hits++;
				partners.add(nodeArray[index]);
				if (nodeArray[index].isEvil()) {
					numberOfEvilNodes++;
				}
			}
			tries++;
		}

		if (partners.isEmpty()) {
			// There might be just too many evil nodes. Let's assume we use one pretrusted node if we meet this case in a real environment.
			for (int i=0; i<this.nodes.size(); i++) {
				final PoiAccountState state = this.poiFacade.findStateByAddress(nodeArray[i].getNode().getIdentity().getAddress());
				if (!nodeArray[i].equals(node) &&
					SynchronizationConstants.REQUIRED_MINIMUM_IMPORTANCE < state.getImportanceInfo().getImportance(HEIGHT)) {
					partners.add(nodeArray[i]);
					break;
				}
			}
		}

		return partners;
	}

	/**
	 * Creates for all partners of node a synchronization sample.
	 *
	 * @param node The node to create the sample for.
	 * @param partners The node's partners.
	 * @return The list of samples.
	 */
	public List<SynchronizationSample> createSynchronizationSamples(final TimeAwareNode node, final Set<TimeAwareNode> partners) {
		final List<SynchronizationSample> samples = new ArrayList<>();
		for (final TimeAwareNode partner : partners) {
			final int roundTripTime = random.nextInt(1000);
			final NetworkTimeStamp localSend = node.getNetworkTime();
			final NetworkTimeStamp localReceive = new NetworkTimeStamp(node.getNetworkTime().getRaw() + partner.getCommunicationDelay().getRaw() + roundTripTime);
			samples.add(new SynchronizationSample(
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
	public void clockAdjustment() {
		if (this.nodeSettings.hasClockAdjustment()) {
			final TimeAwareNode[] nodeArray = nodes.toArray(new TimeAwareNode[nodes.size()]);
			nodeArray[random.nextInt(nodes.size())].adjustClock();
		}
	}

	/**
	 * Calculates the mean value for the time offsets.
	 *
	 * @return The mean value.
	 */
	public double calculateMean() {
		 return nodes.stream().mapToDouble(n -> n.getTimeOffset().getRaw()).sum() / nodes.size();
	}

	/**
	 * Calculates the standard deviation for the time offsets.
	 *
	 * @return The standard deviation.
	 */
	public double calculateStandardDeviation() {
		return Math.sqrt(nodes.stream().mapToDouble(n -> Math.pow(n.getTimeOffset().getRaw() - this.mean, 2)).sum() / nodes.size());
	}

	/**
	 * Calculates the maximum deviation from the mean value.
	 *
	 * @return The maximum deviation from the mean value.
	 */
	public double calculateMaxDeviationFromMean() {
		final OptionalDouble value = nodes.stream().mapToDouble(n -> Math.abs(n.getTimeOffset().getRaw() - this.mean)).max();
		return value.isPresent()? value.getAsDouble() : Double.NaN;
	}

	/**
	 * Updates the statistical values.
	 */
	public void updateStatistics() {
		this.mean = calculateMean();
		this.standardDeviation = calculateStandardDeviation();
		this.maxDeviationFromMean = calculateMaxDeviationFromMean();
		this.hasConverged = this.standardDeviation < TOLERABLE_MAX_STANDARD_DEVIATION;
	}

	/**
	 * Log mean, standard deviation and maximum deviation from mean.
	 */
	public void logStatistics() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final String entry = String.format(
				"%s : %s mean offset to real time: %sms, standard deviation: %sms, max. deviation from mean: %sms",
				getRealTimeString(),
				this.getName(),
				format.format(this.mean),
				format.format(this.standardDeviation),
				format.format(this.maxDeviationFromMean));
		log(entry);
	}

	private String getRealTimeString() {
		final long day = this.realTime/86400000;
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
			outOfRangeNodes.stream().forEach(n -> log(String.format("%s: time offset from mean=%s", n.getName(), format.format(this.mean - n.getTimeOffset().getRaw()))));
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
