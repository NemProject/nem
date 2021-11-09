package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.node.Node;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.core.time.synchronization.CommunicationTimeStamps;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;

import java.util.function.Function;

public class TimeSynchronizationControllerTest {

	// region delegation

	@Test
	public void nonAuthenticatedGetNetworkTimeDelegatesToTimeProvider() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.controller.getNetworkTime();

		// Assert:
		Mockito.verify(context.timeProvider, Mockito.times(2)).getNetworkTime();
	}

	@Test
	public void authenticatedGetNetworkTimeDelegatesToNisPeerNetworkHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		runCommunicationTimeStampsTest(context, c -> c.controller.getNetworkTime(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));

		// Assert:
		Mockito.verify(context.host, Mockito.times(1)).getNetwork();
	}

	@Test
	public void authenticatedGetNetworkTimeDelegatesToTimeProvider() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		runCommunicationTimeStampsTest(context, c -> c.controller.getNetworkTime(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));

		// Assert:
		Mockito.verify(context.timeProvider, Mockito.times(2)).getNetworkTime();
	}

	// endregion

	// region getNetworkTime

	@Test
	public void nonAuthenticatedGetNetworkTimeReturnsCommunicationTimeStamps() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final CommunicationTimeStamps timeStamps = context.controller.getNetworkTime();

		// Assert:
		MatcherAssert.assertThat(timeStamps, IsEqual.equalTo(context.timeStamps));
	}

	@Test
	public void authenticatedGetNetworkTimeReturnsCommunicationTimeStamps() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runCommunicationTimeStampsTest(context, c -> c.controller.getNetworkTime(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	// endregion

	private static <T> T runCommunicationTimeStampsTest(final TestContext context, final Function<TestContext, T> action,
			final Function<T, CommunicationTimeStamps> getCommunicationTimeStamps) {
		// Act:
		final T response = action.apply(context);
		final CommunicationTimeStamps timeStamps = getCommunicationTimeStamps.apply(response);

		// Assert:
		MatcherAssert.assertThat(timeStamps, IsEqual.equalTo(context.timeStamps));
		return response;
	}

	private static class TestContext {
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final TimeSynchronizationController controller;
		private final TimeProvider timeProvider;
		private final CommunicationTimeStamps timeStamps;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));
			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);
			this.timeProvider = Mockito.mock(SystemTimeProvider.class);
			Mockito.when(this.timeProvider.getNetworkTime()).thenReturn(new NetworkTimeStamp(10), new NetworkTimeStamp(20));
			this.timeStamps = new CommunicationTimeStamps(new NetworkTimeStamp(10), new NetworkTimeStamp(20));

			this.controller = new TimeSynchronizationController(this.timeProvider, this.host);
		}
	}
}
