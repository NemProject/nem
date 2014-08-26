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

	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private final int viewSize;
	private final boolean clockAdjustment;
	private double mean;
	private double standardDeviation;
	private double maxDeviationFromMean;
	private boolean hasConverged;

	/**
	 * Creates a network for simulation purposes.
	 *
	 * @param numberOfNodes The number of nodes in the network.
	 */
	public Network(
			final int numberOfNodes,
			final SynchronizationStrategy syncStrategy,
			final int timeOffsetSpread,
			final boolean delayCommunication,
			final boolean asymmetricChannels,
			final boolean instableClock,
			final int viewSize,
			final boolean clockAdjustment) {
		this.viewSize = viewSize;
		this.clockAdjustment = clockAdjustment;
		long cumulativeInaccuracy = 0;
		for (int i=1; i<=numberOfNodes; i++) {
			TimeAwareNode node = new TimeAwareNode(
					i,
					syncStrategy,
					random.nextInt(timeOffsetSpread) - timeOffsetSpread/2,
					delayCommunication? random.nextInt(100) : 0,
					asymmetricChannels? random.nextDouble() : 0.5,
					instableClock? random.nextInt(21) - 10 : 0);
			nodes.add(node);
			cumulativeInaccuracy += node.getClockInaccuary();
		}
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		log(String.format("network mean clock inaccuracy per round: %s ms.", format.format((double)cumulativeInaccuracy/(double)nodes.size())));
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
		if (this.clockAdjustment) {
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
				"mean: %s, standard deviation: %s, max. deviation from mean: %s",
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
