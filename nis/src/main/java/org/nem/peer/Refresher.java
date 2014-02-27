/**
 * 
 */
package org.nem.peer;

import net.minidev.json.JSONObject;

import java.util.logging.Logger;

/**
 * Walks through the list of nodes and refreshes list of peers
 * 
 * @author thies1965
 * 
 */
public class Refresher implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(Refresher.class.getName());

	private PeerNetwork network = null;
	private PeerConnector connector = null;

	public Refresher(PeerNetwork network) {
		this.network = network;
		connector = new PeerConnector();
	}

	@Override
	public void run() {
		JSONObject peerList = null;
		//
		LOGGER.info("Start refreshing peer list.");
		try {
			network.refreshPeerList(connector);
		} catch (InterruptedException e) {
			// Nothing serious, the threads was just called to terminate
			// We log that for debugging purposes
			LOGGER.fine("Received InterruptedExecution, stopping refreshing loop.");
		}
	}
}
