package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockScorer;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.service.BlockIo;
import org.nem.nis.test.*;
import org.nem.peer.node.*;
import org.nem.peer.test.MockPeerNetwork;

import java.math.BigInteger;
import java.util.function.*;

public class BlockControllerTest {

	@Test
	public void blockGetDelegatesToBlockIo() {
		// Arrange:
		final TestContext context = new TestContext();
		final Hash hash = Utils.generateRandomHash();
		final Block blockIoBlock = NisUtils.createRandomBlockWithTimeStamp(27);
		Mockito.when(context.blockIo.getBlock(hash)).thenReturn(blockIoBlock);

		// Act:
		final Block block = context.controller.blockGet(hash.toString());

		// Assert:
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlock(hash);
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlock(Mockito.any());
	}

	//region blockAt

	@Test
	public void blockAtGetDelegatesToBlockIo() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runBlockAtTests(
				context,
				h -> h,
				(c, h) -> c.controller.blockAt(h),
				b -> b);
	}

	@Test
	public void blockAtAuthenticatedGetDelegatesToBlockIo() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runBlockAtTests(
				context,
				h -> new AuthenticatedBlockHeightRequest(h, challenge),
				(c, r) -> c.controller.blockAt(r),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <TRequest, TResponse> TResponse runBlockAtTests(
			final TestContext context,
			final Function<BlockHeight, TRequest> getRequest,
			final BiFunction<TestContext, TRequest, TResponse> action,
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
		Assert.assertThat(block.getTimeStamp(), IsEqual.equalTo(new TimeInstant(27)));
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(height);
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(Mockito.any());
		return response;
	}

	//endregion

	@Test
	public void blockDebugInfoDelegatesToBlockIoAndBlockScorer() {
		// Arrange:
		final TestContext context = new TestContext();
		final BlockHeight height = new BlockHeight(10);
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final TimeInstant timestamp = new TimeInstant(1000);
		final BlockDifficulty difficulty = new BlockDifficulty(123_000_000_000_000L);
		final BigInteger hit = BigInteger.valueOf(1234);
		final Block blockIoBlock = new Block(
				new Account(address),
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				timestamp,
				height);
		blockIoBlock.setDifficulty(difficulty);

		Mockito.when(context.blockIo.getBlockAt(new BlockHeight(43))).thenReturn(blockIoBlock);
		Mockito.when(context.blockScorer.calculateHit(blockIoBlock)).thenReturn(hit);

		// Act:
		final BlockDebugInfo blockDebugInfo = context.controller.blockDebugInfo("43");

		// Assert:
		Assert.assertThat(blockDebugInfo.getHeight(), IsEqual.equalTo(height));
		Assert.assertThat(blockDebugInfo.getForagerAddress(), IsEqual.equalTo(address));
		Assert.assertThat(blockDebugInfo.getTimeInstant(), IsEqual.equalTo(timestamp));
		Assert.assertThat(blockDebugInfo.getDifficulty(), IsEqual.equalTo(difficulty));
		Assert.assertThat(blockDebugInfo.getHit(), IsEqual.equalTo(hit));

		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(new BlockHeight(43));
		Mockito.verify(context.blockIo, Mockito.times(1)).getBlockAt(Mockito.any());

		Mockito.verify(context.blockScorer, Mockito.times(1)).calculateHit(blockIoBlock);
		Mockito.verify(context.blockScorer, Mockito.times(1)).calculateHit(Mockito.any());
	}

	private static class TestContext {
		private final BlockIo blockIo = Mockito.mock(BlockIo.class);
		private final BlockScorer blockScorer = Mockito.mock(BlockScorer.class);
		private final MockPeerNetwork network = new MockPeerNetwork();
		private final NisPeerNetworkHost host;
		private final BlockController controller;

		private TestContext() {
			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new BlockController(this.blockIo, this.blockScorer, this.host);
		}
	}
}
