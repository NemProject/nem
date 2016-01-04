package org.nem.peer.trust.simulation;

import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.peer.PeerNetworkState;
import org.nem.peer.trust.*;
import org.nem.peer.trust.score.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.logging.Logger;

public class NetworkSimulator {
	private static final Logger LOGGER = Logger.getLogger(NetworkSimulator.class.getName());

	/**
	 * When node A has only little experience with node B (number of data exchanges is below MIN_COMMUNICATION),
	 * node A picks node B as communication partner with a chance of 30%.
	 * Thus new nodes get a chance to participate in the network communication.
	 */
	private static final int MIN_COMMUNICATION = 30;

	/**
	 * Number of communications a node has in each round.
	 */
	private static final int COMMUNICATION_PARTNERS = 5;

	/**
	 * The trust context used when running the simulation.
	 */
	private final TrustContext trustContext;

	/**
	 * The simulation configuration.
	 */
	private final Config config;

	/**
	 * The trust provider.
	 */
	private final TrustProvider trustProvider;

	/**
	 * The last set of global trust values.
	 */
	private ColumnVector globalTrustVector;

	private long successfulCalls;
	private long failedCalls;

	/**
	 * Creates a new network simulation.
	 *
	 * @param config The simulator configuration.
	 * @param trustProvider The trust provider to use.
	 * @param minTrust The minimum trust we have in every node.
	 */
	public NetworkSimulator(final Config config, final TrustProvider trustProvider, final double minTrust) {
		if (minTrust <= 0.0 || minTrust > 1.0) {
			throw new IllegalArgumentException("min trust must be in the range (0, 1]");
		}

		this.config = config;
		this.trustProvider = trustProvider;

		final org.nem.peer.Config peerConfig = new org.nem.peer.Config(
				config.getLocalNode(),
				new PreTrustedNodes(config.getPreTrustedNodes()),
				null,
				"0.0.0",
				0,
				new NodeFeature[] {});

		this.trustContext = new PeerNetworkState(peerConfig, new NodeExperiences(), config.getNodes()).getTrustContext();
	}

	public double getFailedPercentage() {
		final long totalCalls = this.successfulCalls + this.failedCalls;
		return 0 == totalCalls ? 0.0 : this.failedCalls * 100.0 / totalCalls;
	}

	/**
	 * Runs the network simulation.
	 * After each round the global trust for the local peer is calculated.
	 *
	 * @param outputFile path to the output file (contains trusts in nodes)
	 * @param numIterations number of rounds for the simulation
	 * @return return true if successful, false otherwise
	 */
	public boolean run(final String outputFile, final int numIterations) {
		try {
			final File file = new File(outputFile);
			final BufferedWriter out = new BufferedWriter(new FileWriter(file));
			this.globalTrustVector = this.trustProvider.computeTrust(this.trustContext).getTrustValues();
			this.writeTrustValues(out, 0);

			this.successfulCalls = 0;
			this.failedCalls = 0;
			// We convert the peers in the network to an array since having a special node like localNode
			// sucks when it comes to simulations.
			final Node[] peers = this.trustContext.getNodes();
			for (int i = 0; i < numIterations; i++) {
				this.doCommunications(peers);
				final TrustProvider trustProvider = new LowComTrustProvider(this.trustProvider, MIN_COMMUNICATION);
				this.globalTrustVector = trustProvider.computeTrust(this.trustContext).getTrustValues();
				if (i % 100 == 99) {
					this.writeTrustValues(out, i + 1);
				}
			}

			out.close();
		} catch (final IOException e) {
			LOGGER.warning("IO-Exception while writing to file <" + outputFile + ">. Reason: " + e.toString());
			return false;
		}

		return true;
	}

	/**
	 * In each round all peers communicate with other peers.
	 * The communication can be successful or not.
	 * Evil peers might fake the feedback.
	 *
	 * @param peers array of peers
	 */
	private void doCommunications(final Node[] peers) {
		for (final Node node : peers) {
			try {
				final NodeBehavior nodeBehavior = this.getNodeBehavior(node);

				// Communicate with other nodes
				for (int i = 0; i < COMMUNICATION_PARTNERS; i++) {
					final Node partner = this.getCommunicationPartner(node, peers);
					if (partner == null) {
						continue;
					}

					final NodeExperience experience = this.getNodeExperience(node, partner);
					final NodeBehavior partnerBehavior = this.getNodeBehavior(partner);
					if (nodeBehavior.isCollusive() && partnerBehavior.isCollusive()) {
						// Communication between collusive evil nodes
						experience.successfulCalls().increment();
					} else {
						// Nodes might fake data and feedback. Depending on the probability to give honest/dishonest feedback,
						// the node inverts his behavior with this probability.
						final boolean honestData = Math.random() < partnerBehavior.getHonestDataProbability();
						final boolean honestFeedback = Math.random() < nodeBehavior.getHonestFeedbackProbability();
						if (!nodeBehavior.isEvil()) {
							if (honestData) {
								this.successfulCalls++;
							} else {
								this.failedCalls++;
							}
						}
						if ((honestData && honestFeedback) || (!honestData && !honestFeedback)) {
							experience.successfulCalls().increment();
						} else {
							experience.failedCalls().increment();
						}
					}
				}
			} catch (final Exception e) {
				LOGGER.warning("Exception in doCommunications, reason: " + e.toString());
			}
		}
	}

	private NodeBehavior getNodeBehavior(final Node node) {
		for (final Config.Entry entry : this.config.getEntries()) {
			if (node.equals(entry.getNode())) {
				return entry.getBehavior();
			}
		}

		throw new IllegalArgumentException(String.format("%s could not be found in the configuration", node));
	}

	private NodeExperience getNodeExperience(final Node a, final Node b) {
		return this.trustContext.getNodeExperiences().getNodeExperience(a, b);
	}

	/**
	 * Given a node, the method chooses a partner node.
	 * Nodes with which the given node has little experience get chosen quite often.
	 * This ensures that unknown nodes get a chance to prove themselves.
	 * When choosing from the set of already known nodes, the chance for a node to be chosen is roughly
	 * proportional to the trust in it.
	 *
	 * @param node the node that needs a partner
	 * @param peers array of peers to choose from
	 * @return chosen node or null if none was chosen
	 */
	private Node getCommunicationPartner(final Node node, final Node[] peers) {

		final NodeCollection nodeCollection = new NodeCollection();
		for (final Node peer : peers) {
			nodeCollection.update(peer, NodeStatus.ACTIVE);
		}

		final TrustContext trustContext = new TrustContext(
				peers,
				node,
				this.trustContext.getNodeExperiences(),
				this.trustContext.getPreTrustedNodes(),
				this.trustContext.getParams());

		final NodeSelector basicNodeSelector = new BasicNodeSelector(10, this.globalTrustVector, trustContext.getNodes());

		return basicNodeSelector.selectNode();
	}

	/**
	 * Writes trust values to an output file.
	 * The percentage of failed calls is written too.
	 *
	 * @param out writer object
	 * @param round the number of rounds already elapsed
	 * @throws IOException
	 */
	private void writeTrustValues(final BufferedWriter out, final int round) throws IOException {

		int index = 0;
		final DecimalFormat f = new DecimalFormat("#0.00000");
		final Node localNode = this.trustContext.getLocalNode();
		out.write("Local node's experience with other nodes after round " + round + ":");
		out.newLine();
		for (final Node node : this.trustContext.getNodes()) {
			if (node.equals(localNode)) {
				continue;
			}

			final NodeExperience experience = this.getNodeExperience(localNode, node);
			out.write("Node " + node + ": ");
			out.write("Successful calls = " + experience.successfulCalls().get());
			out.write(", Failed calls = " + experience.failedCalls().get());
			out.write(", Global trust = " + f.format(this.globalTrustVector.getAt(index++)));
			out.newLine();
		}
		out.write("Percentage failed calls = " + this.getFailedPercentage() + "%");
		out.newLine();
		out.newLine();
	}
}
