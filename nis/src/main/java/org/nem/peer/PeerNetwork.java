/**
 * 
 */
package org.nem.peer;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.nem.core.serialization.JsonSerializer;
import org.nem.core.serialization.Serializer;
import org.nem.deploy.CommonStarter;
import org.nem.peer.v2.Config;
import org.nem.peer.v2.NodeAddress;
import org.nem.peer.v2.NodeInfo;
import org.nem.peer.v2.NodeStatus;

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
	private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

	private static final PeerNetwork DEFAULT_NETWORK = createDefaultNetwork();

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
		return DEFAULT_NETWORK;
	}

	private static PeerNetwork createDefaultNetwork() {
		LOGGER.fine("Configure own node.");
		Node localNode = null;

		String tmpStr = null;

		InputStream fin = null;
		fin = PeerNetwork.class.getClassLoader().getResourceAsStream("peers-config.json");
		if (fin == null) {
			LOGGER.log(Level.SEVERE, "Configuration file <peers-config.json> not available.");
			return null;
		}
		LOGGER.info("NIS settings: ");

        Config config = new Config(CommonStarter.APP_NAME, (JSONObject)JSONValue.parse(fin));
		PeerNetwork network = new PeerNetwork(
            config.getNetworkName(),
            new Node(config.getLocalNode()),
            config.getWellKnownPeers());
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
	 * connected. The peerNetwork gets refreshed on a scheduled time frame. This
	 * is handled in a separate thread. Therefore the peer network requires to
	 * get shut down, in order to have a graceful stop.
	 * 
	 * Initially, the number of outgoing connections is not limited.
	 * 
	 * Future enhancement might be that the current active network get stored in
	 * DB for a faster start-up the next time.
	 * 
	 * The method can only called once.
	 */
	public boolean boot() {
		// check the status of the network
		if (booted) {
			// Just do nothing, even no exception
			return booted;
		}

		// First we loop through the set of defined hosts
		try {
			PeerConnector connector = new PeerConnector();
			for (String peerAddr : initialPeerAddr) {
				addPeer(peerAddr, connector);
			}
			booted = true;

			// Schedule the loop for refreshing
			Refresher command = new Refresher(this);
			executor.scheduleWithFixedDelay(command, 2, 10, TimeUnit.SECONDS);
		} catch(RejectedExecutionException e) {
			//Happens for instance of shutdown happens prior to the first run of the refresher
			//Just means we have to go down. But we check the status
			if(executor.isShutdown()) {
				//early shutdown
				LOGGER.info("Shutdown prior to first refresh cycle.");
			} else {
				//weird...???
				LOGGER.log(Level.SEVERE, "Boot received exception but is not shutdown.", e);
			}
		} catch (InterruptedException e) {
			booted = false;
		}

		return booted;
	}

	/**
	 * The peer network requires to get shutdown, in order to stop the separate
	 * thread.
	 * 
	 * Future enhancement might be that the current active network gets stored
	 * in DB for a faster start-up the next time.
	 * 
	 * The method can only called once.
	 */
	public long shutdown() {
		// check the status of the network
		if (!booted) {
			// Just do nothing, even no exception
			return 0L;
		}

		long result = 0;
		// Check whether there is an executor available
		if (executor != null) {
			executor.shutdownNow();
			result = executor.getTaskCount();
		}
		booted = false;

		return result;
	}

	public boolean addPeer(JSONObject nodeJson, PeerConnector connector) throws InterruptedException {
		boolean result = false;
		Object value = nodeJson.get("address");

		if (value == null) {
			return result;
		}

		if (!(value instanceof String)) {
			return result;
		}

		return addPeer((String) value, connector);
	}

	// TODO: local node must not be added to the list of peers, circle!!
	public boolean addPeer(String addrStr, PeerConnector connector) throws InterruptedException {
		boolean result = false;
		LOGGER.fine("Connecting to: " + addrStr);
		Node node = new Node(addrStr);
		try {
			if (node.verifyNEM()) {
				// ok, so put myself into the network of node

				allPeers.add(node);
				connector.postNewPeer(node, localNode);
				result = true;
			} else {
				LOGGER.fine("Ignoring peer, no NEM peer: " + node);
			}

		} catch (URISyntaxException e) {
			LOGGER.warning(node.toString() + e.toString());
			node.setState(NodeStatus.FAIlURE);
			// remove from allPeers
			allPeers.remove(node);

		} catch (TimeoutException e) {
			LOGGER.warning(node.toString() + " timed out.");
			node.setState(NodeStatus.INACTIVE);

		} catch (ExecutionException e) {
			LOGGER.warning(node.toString() + e.toString());
			node.setState(NodeStatus.FAIlURE);
			// remove from all allPeers
			allPeers.remove(node);

		}

		return result;
	}

	/**
	 * Refreshes the list of peers and also the state of peers.
	 * 
	 * Currently, it is sequential, but could be made parallel in separate
	 * threads.
	 * 
	 * @param connector
	 *            , the connector used to access the peer (remote)
	 * @throws InterruptedException
	 */
	public void refreshPeerList(PeerConnector connector) throws InterruptedException {
		LOGGER.info("Start refreshing peer list.");
		JSONObject peerList = null;

		for (Node peer : getAllPeers()) {
			try {
				switch (peer.getState()) {
				case ACTIVE:
					peerList = connector.requestPeerList(peer);
					processPeerList(peerList, connector);
					break;
				case INACTIVE:
					// maybe woke up in the meantime?
					if (peer.verifyNEM()) {
						peerList = connector.requestPeerList(peer);
						processPeerList(peerList, connector);
					}
					break;
				default:
					break;
				}
			} catch (URISyntaxException | TimeoutException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void processPeerList(JSONObject peerList, PeerConnector connector) throws InterruptedException {
		JSONArray array = (JSONArray) peerList.get("active");
		for (int i = 0; i < array.size(); i++) {
			addPeer((JSONObject)array.get(i), connector);
		}

		// Ok, let's do the same for those being inactive
		array = (JSONArray) peerList.get("inactive");
		for (int i = 0; i < array.size(); i++) {
			addPeer((JSONObject)array.get(i), connector);
		}

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
	
	public void serialize(Serializer serializer) {
		List<Node> allInactive = new ArrayList<>();
		List<Node> allActive = new ArrayList<>();
		for (Node peer : allPeers) {
			switch (peer.getState()) {
			case ACTIVE:
				allActive.add(peer);
				break;
			case INACTIVE:
				allInactive.add(peer);
				break;
			default:
				break;
			}
		}

		serializer.writeObjectArray("active", allActive);
		serializer.writeObjectArray("inactive", allInactive);

	}

	public JSONObject generatePeerList() {
		JsonSerializer serializer = new JsonSerializer();
		serialize(serializer);
		return serializer.getObject();
	}
}
