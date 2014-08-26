package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.NetworkTimeStamp;
import org.nem.core.utils.FormatUtils;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Network {
	private static final Logger LOGGER = Logger.getLogger(Network.class.getName());
	private static final int TOLERABLE_MAX_DEVIATION_FROM_MEAN = 100;

	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private final int viewSize;
	private double mean;
	private double standardDeviation;
	private double maxDeviationFromMean;
	private boolean hasConverged;
	private boolean hasShifted;

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
			final int viewSize) {
		this.viewSize = viewSize;
		for (int i=1; i<=numberOfNodes; i++) {
			nodes.add(new TimeAwareNode(
					i,
					syncStrategy,
					random.nextInt(timeOffsetSpread) - timeOffsetSpread/2,
					delayCommunication? random.nextInt(100) : 0,
					asymmetricChannels? random.nextDouble() : 0.5));
		}
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
	 * Gets a value indicating if the network time mean value has shifted.
	 *
	 * @return true if it has shifted, false otherwise.
	 */
	public boolean hasShifted() {
		return this.hasShifted;
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
		this.hasShifted = Math.abs(oldMean - this.mean) > 0;
		this.hasConverged = Math.abs(oldMean - this.mean) == 0 && this.maxDeviationFromMean < TOLERABLE_MAX_DEVIATION_FROM_MEAN;
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

	public void outputNodes() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		this.nodes.stream().forEach(n -> log(String.format("%s: time offset=%s", n.getName(), format.format(n.getTimeOffset()))));
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
