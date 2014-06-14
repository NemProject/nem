package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.service.BlockIo;
import org.nem.peer.test.MockPeerNetwork;

import java.math.BigInteger;

public class DebugControllerTest {

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
		private final DebugController controller;

		private TestContext() {
			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new DebugController(this.host, this.blockScorer, this.blockIo);
		}
	}
}