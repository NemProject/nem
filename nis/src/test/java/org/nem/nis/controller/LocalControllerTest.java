package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.service.RequiredBlockDao;
import org.nem.peer.PeerNetwork;

public class LocalControllerTest {

	@Test
	public void statusReturnsStatusRunningWhenNetworkIsNotBooted() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NemStatus.RUNNING.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	@Test
	public void statusReturnsStatusBootedWhenNetworkIsBootedButNotSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NemStatus.BOOTED.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	@Test
	public void statusReturnsStatusSynchronizedWhenNetworkIsBootedAndSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NemStatus.SYNCHRONIZED.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	private class TestContext {
		private final RequiredBlockDao blockDao = Mockito.mock(RequiredBlockDao.class);
		private final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final LocalController controller;

		private TestContext() {
			this.controller = new LocalController(blockDao, host);
			Mockito.when(this.host.getNetwork()).thenReturn(network);
		}
	}
}
