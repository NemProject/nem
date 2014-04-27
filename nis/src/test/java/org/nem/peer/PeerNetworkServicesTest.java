package org.nem.peer;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.connect.*;
import org.nem.peer.test.*;

public class PeerNetworkServicesTest {

	@Test
	public void peerNetworkServicesExposesAllConstructorParameters() {
		// Arrange:
		final PeerConnector peerConnector = new MockConnector();
		final SyncConnectorPool syncConnectorPool = Mockito.mock(SyncConnectorPool.class);
		final BlockSynchronizer blockSynchronizer = new MockBlockSynchronizer();

		// Act:
		final PeerNetworkServices services = new PeerNetworkServices(
				peerConnector,
				syncConnectorPool,
				blockSynchronizer);

		// Assert:
		Assert.assertThat(services.getPeerConnector(), IsSame.sameInstance(peerConnector));
		Assert.assertThat(services.getSyncConnectorPool(), IsSame.sameInstance(syncConnectorPool));
		Assert.assertThat(services.getBlockSynchronizer(), IsSame.sameInstance(blockSynchronizer));
	}
}
