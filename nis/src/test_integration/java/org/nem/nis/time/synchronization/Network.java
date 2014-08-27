package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.utils.FormatUtils;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Network {
	private static final Logger LOGGER = Logger.getLogger(Network.class.getName());
	private static final double TOLERABLE_MAX_STANDARD_DEVIATION = 2000;

	private String name;
	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private final int viewSize;
	private final SynchronizationStrategy syncStrategy;
	private final NodeSettings nodeSettings;
	private double mean;
	private double standardDeviation;
	private double maxDeviationFromMean;
	private boolean hasConverged;

	/**
	 * Creates a network for simulation purposes.
	 *
	 * @param name The name of the network.
	 * @param numberOfNodes The number of nodes in the network.
	 * @param syncStrategy The synchronization strategy of the network.
	 * @param viewSize The view size of the nodes in the network.
	 * @param nodeSettings The node settings.
	 */
	public Network(
			final String name,
			final int numberOfNodes,
			final SynchronizationStrategy syncStrategy,
			final int viewSize,
			final NodeSettings nodeSettings) {
		this.name = name;
		this.syncStrategy = syncStrategy;
		this.viewSize = viewSize;
		this.nodeSettings = nodeSettings;
		long cumulativeInaccuracy = 0;
		for (int i=1; i<=numberOfNodes; i++) {
			TimeAwareNode node = new TimeAwareNode(
					i,
					syncStrategy,
					this.random.nextInt(nodeSettings.getTimeOffsetSpread() + 1) - this.nodeSettings.getTimeOffsetSpread()/2,
					this.nodeSettings.doesDelayCommunication()? this.random.nextInt(100) : 0,
					this.nodeSettings.hasAsymmetricChannels()? this.random.nextDouble() : 0.5,
					this.nodeSettings.hasInstableClock()? this.random.nextInt(21) - 10 : 0);
			this.nodes.add(node);
			cumulativeInaccuracy += node.getClockInaccuary();
		}
		if (this.nodeSettings.hasInstableClock()) {
			final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
			log(String.format(
					"%s: mean clock inaccuracy per round: %s ms.",
					this.getName(),
					format.format((double)cumulativeInaccuracy / (double)this.nodes.size())));
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

	/**
	 * Grows the network by a given percentage.
	 *
	 * @param percentage The percentage to grow the network.
	 */
	public void grow(final double percentage) {
		final int numberOfNewNodes = (int)(nodes.size() * percentage / 100);
		for (int i=1; i<=numberOfNewNodes; i++) {
			TimeAwareNode node = new TimeAwareNode(
					i,
					this.syncStrategy,
					this.random.nextInt(this.nodeSettings.getTimeOffsetSpread() + 1) - this.nodeSettings.getTimeOffsetSpread() / 2,
					this.nodeSettings.doesDelayCommunication() ? this.random.nextInt(100) : 0,
					this.nodeSettings.hasAsymmetricChannels() ? this.random.nextDouble() : 0.5,
					this.nodeSettings.hasInstableClock() ? this.random.nextInt(21) - 10 : 0);
			this.nodes.add(node);
		}
	}

	/**
	 * Incorporates the nodes from network into this network.
	 *
	 * @param network The network that joins this network.
	 * @param newName The new name for the this network.
	 */
	public void join(final Network network, final String newName) {
		this.nodes.addAll(network.getNodes());
		this.name = newName;
	}

	/**
	 * Shifts the network time of all nodes by a common random offset.
	 */
	public void randomShiftNetworkTime() {
		final int offset = random.nextInt(40001);
		this.nodes.stream().forEach(n -> n.shiftTimeOffset(offset));
	}

	/**
	 * Selects a set of nodes as communication partners  for a given node.
	 *
	 * @param node The node to select partners for.
	 * @return The set of communication partners.
	 */
	public Set<TimeAwareNode> selectSyncPartnersForNode(final TimeAwareNode node) {
		final Set<TimeAwareNode> partners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		final TimeAwareNode[] nodeArray = nodes.toArray(new TimeAwareNode[nodes.size()]);
		for (int i=0; i<this.viewSize; i++) {
			final int index = random.nextInt(nodes.size());
			if (!nodeArray[index].equals(node)) {
				partners.add(nodeArray[index]);
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
			final NetworkTimeStamp localReceive = new NetworkTimeStamp(node.getNetworkTime().getRaw() + partner.getCommunicationDelay() + roundTripTime);
			samples.add(new SynchronizationSample(
					partner.getEndpoint(),
					new CommunicationTimeStamps(localSend, localReceive),
					partner.createCommunicationTimeStamps(roundTripTime)));
		}

		return samples;
	}

	/**
	 * It's reasonable to assume that the computers in the network adjust their clock via NTP every now and then.
	 * We assume here that this happens about every 1-2 days.
	 */
	public void clockAdjustment() {
		if (this.nodeSettings.hasClockAdjustment()) {
			final int clocksToAdjust = nodes.size() < 500? 1 : nodes.size() / 500;
			final TimeAwareNode[] nodeArray = nodes.toArray(new TimeAwareNode[nodes.size()]);
			for (int i = 0; i < clocksToAdjust; i++) {
				TimeAwareNode node = nodeArray[random.nextInt(nodes.size())];
				//log("adjusting clock of " + node.getName() + " by " + node.getCumulativeInaccuary() + "ms.");
				node.adjustClock();
			}
		}
	}

	/**
	 * Calculates the mean value for the time offsets.
	 *
	 * @return The mean value.
	 */
	public double calculateMean() {
		 return nodes.stream().mapToDouble(TimeAwareNode::getTimeOffset).sum() / nodes.size();
	}

	/**
	 * Calculates the standard deviation for the time offsets.
	 *
	 * @return The standard deviation.
	 */
	public double calculateStandardDeviation() {
		return Math.sqrt(nodes.stream().mapToDouble(n -> Math.pow(n.getTimeOffset() - this.mean, 2)).sum() / nodes.size());
	}

	/**
	 * Calculates the maximum deviation from the mean value.
	 *
	 * @return The maximum deviation from the mean value.
	 */
	public double calculateMaxDeviationFromMean() {
		final OptionalDouble value = nodes.stream().mapToDouble(n -> Math.abs(n.getTimeOffset() - this.mean)).max();
		return value.isPresent()? value.getAsDouble() : Double.NaN;
	}

	/**
	 * Updates the statistical values.
	 */
	public void updateStatistics() {
		final double oldMean = this.mean;
		this.mean = calculateMean();
		this.standardDeviation = calculateStandardDeviation();
		this.maxDeviationFromMean = calculateMaxDeviationFromMean();
		this.hasConverged = Math.abs(oldMean - this.mean) < 100 && this.standardDeviation < TOLERABLE_MAX_STANDARD_DEVIATION;
	}

	/**
	 * Log mean, standard deviation and maximum deviation from mean.
	 */
	public void logStatistics() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final String entry = String.format(
				"%s mean: %s, standard deviation: %s, max. deviation from mean: %s",
				this.getName(),
				format.format(this.mean),
				format.format(this.standardDeviation),
				format.format(this.maxDeviationFromMean));
		this.log(entry);
	}

	public void outputOutOfRangeNodes(final long maxTolerableDeviationFromMean) {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final List<TimeAwareNode> outOfRangeNodes = this.nodes.stream()
				.filter(n -> Math.abs(this.mean - n.getTimeOffset()) > maxTolerableDeviationFromMean)
				.collect(Collectors.toList());
		if (!outOfRangeNodes.isEmpty()) {
			outOfRangeNodes.stream().forEach(n -> log(String.format("%s: time offset=%s", n.getName(), format.format(n.getTimeOffset()))));
		}
	}

	public void log(final String entry) {
		// BR: Those red logger lines are killing my eyes...
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			System.out.println(entry);
		} else {
			LOGGER.info(entry);
		}
	}
}
