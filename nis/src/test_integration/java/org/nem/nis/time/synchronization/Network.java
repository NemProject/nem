package org.nem.nis.time.synchronization;

import org.nem.core.model.primitive.NetworkTimeStamp;

import java.security.SecureRandom;
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
			final boolean delayCommunication,
			final int viewSize) {
		this.viewSize = viewSize;
		for (int i=1; i<=numberOfNodes; i++) {
			nodes.add(new TimeAwareNode(i, syncStrategy, delayCommunication? random.nextInt(100) : 0));
		}
	}

	/**
	 * Selects a set of nodes as communication partners  for a given node.
	 *
	 * @param node The node to select partners for.
	 * @return The set of communication partners.
	 */
	public Set<TimeAwareNode> selectSyncPartnersForNode(final TimeAwareNode node) {
		final Set<TimeAwareNode> partners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		final TimeAwareNode[] nodeArray = (TimeAwareNode[])nodes.toArray();
		for (int i=0; i<this.viewSize; i++) {
			final int index = random.nextInt(nodes.size());
			if (!nodeArray[index].equals(node)) {
				partners.add(nodeArray[index]);
			}
		}

		return partners;
	}

	public List<SynchronizationSample> createSynchronizationSamples(final TimeAwareNode node, final Set<TimeAwareNode> partners) {
		final List<SynchronizationSample> samples = new ArrayList<>();
		for (final TimeAwareNode partner : partners) {
			final int roundTripTime = random.nextInt(1000);
			final NetworkTimeStamp localSend = node.getNetworkTime();
			final NetworkTimeStamp localReceive = new NetworkTimeStamp(node.getNetworkTime().getRaw() + partner.getCommunicationDelay() + roundTripTime);
			samples.add(new SynchronizationSample(
					partner.getEndpoint(),
					new CommunicationTimeStamps(localSend, localReceive),
					partner.createCommunicationTimeStamps()));
		}

		return samples;
	}

	public void statistics() {

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
