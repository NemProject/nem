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

	private final Set<TimeAwareNode> nodes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final SecureRandom random = new SecureRandom();
	private final int viewSize;

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
			final int viewSize) {
		this.viewSize = viewSize;
		for (int i=1; i<=numberOfNodes; i++) {
			nodes.add(new TimeAwareNode(
					i,
					syncStrategy,
					random.nextInt(timeOffsetSpread) - timeOffsetSpread/2,
					delayCommunication? random.nextInt(100) : 0));
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
	 * Calculates the mean value.
	 *
	 * @return The mean value.
	 */
	public double timeOffsetMean() {
		return nodes.stream().mapToDouble(TimeAwareNode::getTimeOffset).sum() / nodes.size();
	}

	/**
	 * Calculates the standard deviation.
	 *
	 * @return The standard deviation.
	 */
	public double timeOffsetStandardDeviation() {
		final double mean = timeOffsetMean();
		return Math.sqrt(nodes.stream().mapToDouble(n -> Math.pow(n.getTimeOffset() - mean, 2)).sum() / nodes.size());
	}

	/**
	 * Calculates the maximum deviation from the mean value.
	 *
	 * @return The maximum deviation.
	 */
	public double timeOffsetMaxDeviationFromMean() {
		final double mean = timeOffsetMean();
		final OptionalDouble value = nodes.stream().mapToDouble(n -> Math.abs(n.getTimeOffset() - mean)).max();

		return value.isPresent()? value.getAsDouble() : Double.NaN;
	}

	/**
	 * Log mean, standard deviation and maximum deviation from mean.
	 */
	public void statistics() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		final String entry = String.format(
				"mean: %s, standard deviation: %s, max. deviation from mean: %s",
				format.format(timeOffsetMean()),
				format.format(timeOffsetStandardDeviation()),
				format.format(timeOffsetMaxDeviationFromMean()));
		this.log(entry);
	}

	public void outputNodes() {
		final DecimalFormat format = FormatUtils.getDefaultDecimalFormat();
		this.nodes.stream().forEach(n -> log(String.format("%s: time offset=%s", n.getName(), format.format(n.getTimeOffset()))));
	}

	public void log(String entry) {
		// BR: Those red logger lines are killing my eyes...
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			System.out.println(entry);
		} else {
			LOGGER.info(entry);
		}
	}
}
