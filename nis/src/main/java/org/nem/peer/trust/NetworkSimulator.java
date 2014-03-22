package org.nem.peer.trust;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nem.peer.Node;
import org.nem.peer.PeerNetwork;

public class NetworkSimulator {
	private static final Logger LOGGER = Logger.getLogger(NetworkSimulator.class.getName());

	/**
	 * When node A has only little experience with node B (number of data exchanges is below MIN_COMMUNICATION),
	 * node A picks node B as communication partner with a chance of 30%.
	 * Thus new nodes get a chance to participate in the network communication.
	 */
	private final int MIN_COMMUNICATION = 10;
	
	/**
	 * Number of node attributes in the node configuration file.
	 */
	private final int NODE_ATTRIBUTE_COUNT = 7;
	
	/**
	 * Number of communications a node has in each round.
	 */
	private final int COMMUNICATION_PARTNERS = 5;
	
	/**
	 * Local node's address
	 */
	private final String LOCAL_ADDRESS = "127.0.0.1";
	
	private PeerNetwork network=null;
	private boolean initialized=false;
	private long successfulCalls=0;
	private long failedCalls=0;

	/**
	 * Minimum trust we always have in a node.
	 * The higher the value the higher the chance that nodes with low trust value will
	 * will get picked as communication partner.
	 */
	private double minTrust = 0.01;
	
	/**
	 * The trust model used when running the simulation
	 */
	private BasicTrust trustModel = null;
	
	public String getTrustModelName() {
		if (trustModel == null) {
			return "";
		}
		return trustModel.getClass().getName();
	}

	/**
	 * @param trustModel the used trust model
	 * @param minTrust   minimum trust we have in every node.
	 */
	public NetworkSimulator(BasicTrust trustModel, double minTrust) {
		if (trustModel == null) {
			throw new IllegalArgumentException("NetworkSimulator constuctor requires a BasicTrust object not equal to null as first parameter.");
		}
		this.trustModel = trustModel;
		if (minTrust > 0.0 && minTrust <= 1.0) {
			this.minTrust = minTrust;
		}
	}
	
	/**
	 * Set up the PeerNetwork object:
	 * The file contains one node description in each line (attributes for the node).
	 * The attributes are separated by a colon. The attributes are:
	 * address:                     the ip of the node
	 * evil:                        determines if the node is evil.
	 * pretrusted:                  determines if the node belongs to the set of pretrusted nodes.
	 * honest data probability:     probability that the node will send valid data.
	 * honest feedback probability: probability that the node will give valid feedback about another node.
	 * leech:                       determines if the node tries to attack the network by excessive requesting data.
	 * collusive:                   determines if the node is colluding with other evil nodes.
	 * 
	 * @param inputFile path to the file containing the behavior information of the nodes
	 * @return true if successful, false otherwise
	 */
	public boolean initialize(final String inputFile) {
		try {
		    File file = new File(inputFile);
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			int count=1;
			Set<String> pretrustedNodes = new HashSet<String>();
			Set<Node> nodes = new HashSet<Node>();
			try {
				line = in.readLine();
				while ((line=in.readLine()) != null) {
					count++;
					String[] nodeAttributes = line.split(";");
					if (nodeAttributes.length != NODE_ATTRIBUTE_COUNT) {
						LOGGER.warning("Malformed data in configuration file <" + inputFile + "> (Line " + count + ") .");
						in.close();
						return false;
					}
					
					boolean evil = nodeAttributes[1].equals("1")? true : false;
					boolean pretrusted = nodeAttributes[2].equals("1")? true : false;
					double honestDataProbability = Double.parseDouble(nodeAttributes[3]);
					double honestFeedbackProbability = Double.parseDouble(nodeAttributes[4]);
					boolean leech = nodeAttributes[5].equals("1")? true : false;
					boolean collusive = nodeAttributes[6].equals("1")? true : false;
					Node node = new Node(nodeAttributes[0]);
					node.setBehavior(new NodeBehavior(evil, honestDataProbability, honestFeedbackProbability, leech, collusive));
					nodes.add(node);
					if (pretrusted) {
						pretrustedNodes.add(nodeAttributes[0]);
					}
				}
				in.close();
				LOGGER.info("Found " + (count-1) + " nodes in configuration file <" + inputFile + "> (" + pretrustedNodes.size() + " pretrusted nodes).");
				Node localNode = new Node(LOCAL_ADDRESS);
				localNode.setBehavior(new NodeBehavior(false, 1.0, 1.0, false, false));
				network = new PeerNetwork("Simulation network", localNode, pretrustedNodes);
				for (Node node : nodes) {
					network.getAllPeers().add(node);
				}
			} catch (IOException e) {
				LOGGER.warning("IO-Exception while reading configuration file <" + inputFile + ">. Reason: " + e.toString());
				return false;
			}
		} catch(FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, "Configuration file <" + inputFile + "> not available.");
			return false;
		}
		initialized = true;
		return true;
	}
	
	/**
	 * Runs the network simulation.
	 * After each round the global trust for the local peer is calculated. 
	 * 
	 * @param outputFile        path to the output file (contains trusts in nodes)
	 * @param numIterations	    number of rounds for the simulation
	 * @return                  return true if successful, false otherwise
	 */
	public boolean run(final String outputFile, final int numIterations) {
		if (!initialized) {
			return false;
		}
		try {
			File file = new File(outputFile);
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			trustModel.analyze(network);
			writeTrustValues(out, 0);
			
			successfulCalls = 0;
			failedCalls = 0;
			// We convert the peers in the network to an array since having a special node like localNode
			// sucks when it comes to simulations.
			Node[] peers = network.getAllPeers().toArray(new Node[network.getAllPeers().size()+1]);
			peers[peers.length-1] = network.getLocalNode();
			for (int i=0; i<numIterations; i++) {
				doCommunications(peers);
				for (Node node : peers) {
					trustModel.computeLocalTrust(node, network.getInitialPeerAddr());
					trustModel.computeFeedbackCredibility(node, peers);
				}
				trustModel.analyze(network);
				if (i % 100 == 9) {
					writeTrustValues(out, i+1);
				}
			}
			
			out.close();
		} catch (IOException e) {
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
	 * @param peers    array of peers
	 */
	private void doCommunications(final Node[] peers) {
		for (Node node : peers) {
			try
			{
				// Communicate with other nodes
				for (int i=0; i<COMMUNICATION_PARTNERS; i++) {
					Node partner = getCommunicationPartner(node, peers);
					if (partner == null) {
						continue;
					}
					if (node.getNodeBehavior().isCollusive() &&
						partner.getNodeBehavior().isCollusive()) {
						// Communication between collusive evil nodes
						node.getNodeExperience(partner).incSuccessfulCalls();
					}
					else {
						// Nodes might fake data and feedback. Depending on the probability to give honest/dishonest feedback,
						// the node inverts his behavior with this probability.
						boolean honestData = Math.random() < partner.getNodeBehavior().getHonestDataProbability();
						boolean honestFeedback = Math.random() < node.getNodeBehavior().getHonestFeedbackProbability();
						if (!(node.getNodeBehavior().isEvil())) {
							if (honestData) {
								successfulCalls++;
							}
							else {
								failedCalls++;
							}
						}
						if ((honestData && honestFeedback) || (!honestData && !honestFeedback)) {
							node.getNodeExperience(partner).incSuccessfulCalls();
						}
						else {
							node.getNodeExperience(partner).incFailedCalls();
						}
					}
				}
			} catch (Exception e) {
				LOGGER.warning("Exception in doCommunications, reason: " + e.toString());
			}
		}
	}
	
	/**
	 * Given a node, the method chooses a partner node.
	 * Nodes with which the given node has little experience get chosen quite often.
	 * This ensures that unknown nodes get a chance to prove themselves.
	 * When choosing from the set of already known nodes, the chance for a node to be chosen is roughly 
	 * proportional to the trust in it.  
	 * 
	 * @param node        the node that needs a partner
	 * @param peers       array of peers to choose from
	 * @return            chosen node or null if none was chosen
	 */
	private Node getCommunicationPartner(final Node node, final Node[] peers) {
		// Pick a partner according to the trust in that node and luck
		double min = Double.MAX_VALUE;
		Node partner=null;
		
		for (Node tmpNode : peers) {
			if (tmpNode == node) {
				continue;
			}
			long numCalls = node.getNodeExperience(tmpNode).getSuccessfulCalls() + node.getNodeExperience(tmpNode).getFailedCalls();
			if (numCalls < MIN_COMMUNICATION) {
				// Since we have only very little experience with this node, we give him a 30% chance
				if (Math.random() > 0.3) {
					partner = tmpNode;
					break;
				} 
				else {
					continue;
				}
			}
			else {
				// You can play with different pattern to choose a node here
				//double value = Math.random()/Math.log(1+10*node.getNodeExperience(tmpNode.getAddress()).getTrust());
				//double value = Math.random()/Math.exp(minTrust/network.getAllPeers().size()+10*node.getNodeExperience(tmpNode.getAddress()).getTrust());
				double value = Math.random()/(minTrust/network.getAllPeers().size() + network.getLocalNode().getNodeExperience(tmpNode).getGlobalTrust());
				if (value < min) {
					partner = tmpNode;
					min = value;
				}
			}
		}
		
		return partner;
	}
	
	/**
	 * Writes trust values to an output file.
	 * The percentage of failed calls is written too.
	 * 
	 * @param out             writer object
	 * @param round           the number of rounds already elapsed
	 * @throws IOException
	 */
	private void writeTrustValues(final BufferedWriter out, final int round) throws IOException {
		
		DecimalFormat f = new DecimalFormat("#0.00000"); 
		out.write("Local node's experience with other nodes after round " + round + ":");
		out.newLine();
		for (Map.Entry<Node, NodeExperience> entry : network.getLocalNode().getNodeExperiences().entrySet()) {
			if (entry.getKey().equals(network.getLocalNode())) {
				//continue;
			}
			out.write("Node " + entry.getKey().getAddress() + ": ");
			out.write("Successful calls = " + entry.getValue().getSuccessfulCalls());
			out.write(", Failed calls = " + entry.getValue().getFailedCalls());
			out.write(", Global trust = " + f.format(entry.getValue().getGlobalTrust()));
			out.newLine();
		}
		out.write("Percentage failed calls = " + (successfulCalls+failedCalls == 0? 0 : ((failedCalls*100)/(successfulCalls+failedCalls))) + "%");
		out.newLine();
		out.newLine();
	}	
}
