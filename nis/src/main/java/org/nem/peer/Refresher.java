/**
 * 
 */
package org.nem.peer;

import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Walks through the list of nodes and refreshes list of peers
 * @author thies1965
 *
 */
public class Refresher implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(PeerNetwork.class.getName());

	private Set<Node> allPeers = null;
	private ClientConnector connector = null;
	
	public Refresher(Set<Node> allPeers) {
		this.allPeers = allPeers;
		connector = new ClientConnector();
	}

	@Override
	public void run() {
		// 
		LOGGER.info("Start refreshing peer list.");
		
		for (Node peer : allPeers) {
			try {
				connector.requestPeerList(peer);
			} catch (URISyntaxException | InterruptedException | TimeoutException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
