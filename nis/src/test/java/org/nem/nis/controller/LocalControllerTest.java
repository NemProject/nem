package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.peer.PeerNetwork;

public class LocalControllerTest {

	@Test
	public void statusReturnsStatusRunningWhenNetworkIsNotBootedAndNotBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.RUNNING);
	}

	@Test
	public void statusReturnsStatusBootingWhenNetworkIsNotBootedButBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.BOOTING);
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
		assertStatus(result, NemStatus.BOOTED);
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
		assertStatus(result, NemStatus.SYNCHRONIZED);
	}

	private static void assertStatus(final NemRequestResult result, final NemStatus expectedStatus) {
		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(expectedStatus.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	private class TestContext {
		private final ReadOnlyBlockDao blockDao = Mockito.mock(ReadOnlyBlockDao.class);
		private final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final LocalController controller;

		private TestContext() {
			this.controller = new LocalController(this.blockDao, this.host);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);
		}
	}
}
