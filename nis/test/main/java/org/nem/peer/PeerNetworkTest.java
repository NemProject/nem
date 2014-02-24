package org.nem.peer;

import static org.junit.Assert.*;

import org.junit.Test;

public class PeerNetworkTest {

	@Test
	public void testGetDefaultNetwork() {
		//Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();
		
		//Verify
		assertNotNull(network);
		assertNotNull(network.getLocalNode());
		assertNotNull(network.getLocalNode().getAddress());
		assertNotSame(network.getLocalNode().getState(), NodeStatus.FAILURE);

		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

	@Test
	public void testBoot() {
		//Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();
		network.boot();
		
		//Verify
		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

	@Test
	public void testBoot2() {
		//Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();
		network.boot();

		//second call
		network.boot();
		
		//Verify
		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

	@Test
	public void testRefresh() {
		//Prep work
		PeerNetwork network = PeerNetwork.getDefaultNetwork();
		network.boot();

		//second call
		network.boot();
		
		//Verify
		assertNotNull(network.getAllPeers());
		assertNotNull(network.generatePeerList());
	}

}
