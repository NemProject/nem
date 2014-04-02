package org.nem.peer;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.nem.peer.scheduling.SchedulerFactory;
import org.nem.peer.test.*;

public class PeerNetworkServicesTest {

	@Test
	public void peerNetworkServicesExposesAllConstructorParameters() {
		// Arrange:
		final PeerConnector peerConnector = new MockConnector();
		final SyncConnector syncConnector = new MockConnector();
		final SchedulerFactory<Node> schedulerFactory = new MockNodeSchedulerFactory();
		final BlockSynchronizer blockSynchronizer = new MockBlockSynchronizer();

		// Act:
		final PeerNetworkServices services = new PeerNetworkServices(
				peerConnector,
				syncConnector,
				schedulerFactory,
				blockSynchronizer);

		// Assert:
		Assert.assertThat(services.getPeerConnector(), IsSame.sameInstance(peerConnector));
		Assert.assertThat(services.getSyncConnector(), IsSame.sameInstance(syncConnector));
		Assert.assertThat(services.getSchedulerFactory(), IsSame.sameInstance(schedulerFactory));
		Assert.assertThat(services.getBlockSynchronizer(), IsSame.sameInstance(blockSynchronizer));
	}
}
