/**
 * 
 */
package org.nem.peer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

import org.nem.NEM;
import org.nem.util.NEMLogger;

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
	// Do not known right now for what purposes but it is always good to have a
	// name...
	private String name;
	// We keep the port configurable for now
	private Node localNode;

	// Initial list of the peers provided statically
	private Set<String> initialPeerAddr;

	// Set of active connections to peers
	private Set<Node> allPeers;

	public Set<Node> getAllPeers() {
		return allPeers;
	}

	private boolean booted;

	public PeerNetwork(String name, Node localNode, Set<String> initialHosts) {
		super();

		this.name = name;
		this.localNode = localNode;
		this.initialPeerAddr = initialHosts;

		booted = false;
		allPeers = new HashSet<Node>();
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
		Node node = null;
		for (String peerAddr : initialPeerAddr) {
			node = new Node(peerAddr, NEM.NEM_PORT);
			if (node.verifyNEM()) {
				// ok, so put myself into the network of node
				try {
					node.extendNetworkBy(localNode);
					allPeers.add(node);
				} catch (MalformedURLException e) {
					// Is unlikely that this would happen, but anyhow we have to
					// be robust
					NEMLogger.LOG.log(Level.WARNING, node.toString(), e);
					node.setState(NodeStatus.FAILURE);

				} catch (IOException e) {
					NEMLogger.LOG.log(Level.WARNING, node.toString(), e);
					node.setState(NodeStatus.INACTIVE);
				}
			}
		}
		booted = true;
	}
	
	private long countActive() {
		long result = 0;
		for (Node node : allPeers) {
			if(node.getState() == NodeStatus.ACTIVE) {
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
}
