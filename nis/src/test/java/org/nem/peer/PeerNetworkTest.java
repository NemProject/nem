package org.nem.peer;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.hamcrest.core.IsNot;
import org.junit.Test;
import org.nem.peer.v2.NodeStatus;

public class PeerNetworkTest {

	@Test
	public void testGetDefaultNetwork() {
		// Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();

		// Verify
		assertNotNull(network);
		assertNotNull(network.getLocalNode());
		assertNotNull(network.getLocalNode().getAddress());
		assertNotSame(network.getLocalNode().getState(), NodeStatus.FAILURE);

		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

	@Test
	public void testBoot() {
		// Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();

		// Verify
		assertTrue(network.boot());
		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

	@Test
	public void testShutdown() {
		// Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();
		network.boot();
		
//		try {
//			Thread.sleep(15000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		long tasks = network.shutdown();
		//Verify
		assertTrue(tasks == 0);
	}

}
