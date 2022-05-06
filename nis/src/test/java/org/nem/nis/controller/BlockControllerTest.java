package org.nem.nis.controller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.requests.AuthenticatedBlockHeightRequest;
import org.nem.nis.service.BlockIo;
import org.nem.nis.test.NisUtils;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;

import java.util.function.*;

public class BlockControllerTest {

	// region blockAt

	@Test
	public void blockAtGetDelegatesToBlockIo() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runBlockAtTests(context, h -> h, (c, h) -> c.controller.blockAt(h), b -> b);
	}

	@Test
	public void blockAtAuthenticatedGetDelegatesToBlockIo() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runBlockAtTests(context, h -> new AuthenticatedBlockHeightRequest(h, challenge),
				(c, r) -> c.controller.blockAt(r), r -> r.getEntity(localNode.getIdentity(), challenge));
		MatcherAssert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <TRequest, TResponse> TResponse runBlockAtTests(final TestContext context,
			final Function<BlockHeight, TRequest> getRequest, final BiFunction<TestContext, TRequest, TResponse> action,
			final Function<TResponse, Block> getBlock) {
		// Arrange
		final BlockHeight height = new BlockHeight(17);
		final Block blockIoBlock = NisUtils.createRandomBlockWithTimeStamp(27);
		Mockito.when(context.blockIo.getBlockAt(height)).thenReturn(blockIoBlock);

		final TRequest request = getRequest.apply(height);

		// Act:
		final TResponse response = action.apply(context, request);
		final Block block = getBlock.apply(response);

		// Assert:
		MatcherAssert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(height);
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(Mockito.any());
		return response;
	}

	// endregion

	private static class TestContext {
		private final BlockIo blockIo = Mockito.mock(BlockIo.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final BlockController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new BlockController(this.blockIo, this.host);
		}
	}
}
