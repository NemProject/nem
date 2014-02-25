/**
 * 
 */
package org.nem.peer;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Reflects a peer network. NEM might end up with parallel multiple peer
 * networks. It knows about the active peers, does the weighting of peers, saves
 * the state of active peers to come up after restart. It also takes the initial
 * list of defined peers.
 * 
 * @author Thies1965
 * 
 */
public class PeerNetwork {
	private static final Logger logger = Logger.getLogger(PeerNetwork.class.getName());

	private static PeerNetwork DEFAULT_NETWORK;

	// Do not known right now for what purposes but it is always good to have a
	// name...
	private String name;
	// We keep the port configurable for now
	private Node localNode;

	// Initial list of the peers provided statically
	private Set<String> initialPeerAddr;

	// Set of active connections to peers
	private Set<Node> allPeers;

	private boolean booted;
	private ScheduledThreadPoolExecutor executor;

	public static PeerNetwork getDefaultNetwork() {
		if (DEFAULT_NETWORK == null) {
			synchronized (PeerNetwork.class) {
				if (DEFAULT_NETWORK == null) {
					DEFAULT_NETWORK = createDefaultNetwork();
				}
			}
		}
		return DEFAULT_NETWORK;
	}

	private static PeerNetwork createDefaultNetwork() {
		logger.fine("Configure own node.");
		Node localNode = null;

		String tmpStr = null;

		InputStream fin = null;
		fin = PeerNetwork.class.getClassLoader().getResourceAsStream("peers-config.json");
		if (fin == null) {
			logger.log(Level.SEVERE, "Configuration file <peers-config.json> not available.");
			return null;
		}
		logger.info("NIS settings: ");

		JSONObject config = (JSONObject) JSONValue.parse(fin);

		tmpStr = (String) config.get("myAddress");
		if (tmpStr != null) {
			tmpStr = tmpStr.trim();
		}
		if (tmpStr == null || tmpStr.length() == 0) {
			tmpStr = "localhost";
		}
		logger.info("  \"myAddress\" = \"" + tmpStr + "\"");
		localNode = new Node(tmpStr);

		tmpStr = (String) config.get("myPlatform");
		if (tmpStr == null) {
			tmpStr = "PC";

		} else {
			tmpStr = tmpStr.trim();
		}
		localNode.setPlatform(tmpStr);
		logger.info("  \"myPlatform\" = \"" + tmpStr + "\"");

		JSONArray knownPeers = (JSONArray) config.get("knownPeers");
		Set<String> wellKnownPeers;
		if (knownPeers != null) {
			Set<String> hosts = new HashSet<String>();
			for (Iterator<Object> i = knownPeers.iterator(); i.hasNext();) {
				String hostEntry = (String) i.next();
				hostEntry = hostEntry.trim();
				if (hostEntry.length() > 0) {
					hosts.add(hostEntry);
				}
			}
			wellKnownPeers = Collections.unmodifiableSet(hosts);

		} else {
			wellKnownPeers = Collections.emptySet();
			logger.warning("No wellKnownPeers defined, it is unlikely to work");
		}

		PeerNetwork network = new PeerNetwork("Default network", localNode, wellKnownPeers);
		network.boot();

		return network;
	}

	public PeerNetwork(String name, Node localNode, Set<String> initialHosts) {
		super();

		this.name = name;
		this.localNode = localNode;
		this.initialPeerAddr = initialHosts;

		booted = false;
		allPeers = new HashSet<Node>();
		executor = new ScheduledThreadPoolExecutor(1);
	}

	public Set<Node> getAllPeers() {
		return allPeers;
	}

	public Node getLocalNode() {
		return localNode;
	}

	/**
	 * The peer network is being set-up. The known peers are tried to get
	 * connected. Each peer connection will be separated in thread, that
	 * requires that the peer network has to be shutdown, in order to have a
	 * graceful stop.
	 * 
	 * Initially, the number of outgoing connections is not limited.
	 * 
	 * Future enhancement might be that the current active network get stored in
	 * DB for a faster start-up the next time.
	 * 
	 * The method can only called once. If the network does not come up, a
	 * reboot has to be initiated (@see reboot).
	 */
	public void boot() {
		// check the status of the network
		if (booted) {
			// Just do nothing, even no exception
			return;
		}

		// First we loop through the set of defined hosts
		PeerConnector connector = new PeerConnector();
		Node node = null;
		for (String peerAddr : initialPeerAddr) {
			logger.fine("Connecting to: " + peerAddr);
			node = new Node(peerAddr);
			if (node.verifyNEM()) {
				// ok, so put myself into the network of node
				try {
					allPeers.add(node);
					connector.putNewPeer(node, localNode);
				} catch (URISyntaxException e) {
					logger.warning(node.toString() + e.toString());
					node.setState(NodeStatus.FAILURE);
					//remove from all allPeers
					allPeers.remove(node);

				} catch (TimeoutException e) {
					logger.warning(node.toString() + " timed out.");
					node.setState(NodeStatus.INACTIVE);

				} catch (ExecutionException e) {
					logger.warning(node.toString() + e.toString());
					node.setState(NodeStatus.FAILURE);
					//remove from all allPeers
					allPeers.remove(node);

				} catch (InterruptedException e) {
					logger.warning("Interrupted execution.");
				}

			} else {
				logger.fine("Ignoring peer, no NEM peer: " + peerAddr);
			}
		}
		booted = true;
		
		//Schedule the loop for refreshing
		Refresher command = new Refresher(allPeers);
		executor.scheduleWithFixedDelay(command, 2, 10, TimeUnit.SECONDS);
	}

	private long countActive() {
		long result = 0;
		for (Node node : allPeers) {
			if (node.getState() == NodeStatus.ACTIVE) {
				result++;
			}
		}

		return result;
	}

	public String toString() {
		StringBuilder strB = new StringBuilder(name).append("<PeerNetwork> [");
		strB.append(allPeers.size()).append(", ");
		strB.append(countActive()).append("]");

		return strB.toString();
	}

	public JSONObject generatePeerList() {
		JSONObject result = new JSONObject();
		JSONArray allInactive = new JSONArray();
		JSONArray allActive = new JSONArray();
		for (Node peer : allPeers) {
			switch (peer.getState()) {
			case ACTIVE:
				allActive.add(peer.generateNodeInfo());
				break;
			case INACTIVE:
				allInactive.add(peer.generateNodeInfo());
				break;
			default:
				break;
			}
		}
		result.put("active", allActive);
		result.put("inactive", allInactive);
		return result;
	}
}
