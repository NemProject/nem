package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockChain;
import org.nem.nis.test.NisUtils;
import org.nem.peer.*;

public class HarvestingTaskTest {

	@Test
	public void harvestDropsExpiredTransactions() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.harvest();

		// Assert:
		Mockito.verify(context.unconfirmedTransactions, Mockito.only()).dropExpiredTransactions(context.currentTime);
	}

	@Test
	public void harvestedBlockIsNotPushedIfNull() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.harvester.harvestBlock()).thenReturn(null);
		Mockito.when(context.blockChain.processBlock(Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.harvest();

		// Assert:
		Mockito.verify(context.harvester, Mockito.only()).harvestBlock();
		Mockito.verify(context.blockChain, Mockito.never()).processBlock(Mockito.any());
		Mockito.verify(context.network, Mockito.never()).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void harvestedBlockIsNotPushedIfProcessBlockReturnsFailure() {
		// Assert:
		assertHarvestedBlockNotPushed(ValidationResult.FAILURE_UNKNOWN);
	}

	@Test
	public void harvestedBlockIsNotPushedIfProcessBlockReturnsNeutral() {
		// Assert:
		assertHarvestedBlockNotPushed(ValidationResult.NEUTRAL);
	}

	private static void assertHarvestedBlockNotPushed(final ValidationResult processBlockResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = createSignedBlock();
		Mockito.when(context.harvester.harvestBlock()).thenReturn(block);
		Mockito.when(context.blockChain.processBlock(Mockito.any())).thenReturn(processBlockResult);

		// Act:
		context.harvest();

		// Assert:
		Mockito.verify(context.harvester, Mockito.only()).harvestBlock();
		Mockito.verify(context.blockChain, Mockito.only()).processBlock(block);
		Mockito.verify(context.network, Mockito.never()).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void harvestedBlockIsPushedIfItPassesBlockChainProcessing() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = createSignedBlock();
		final Hash blockHash = HashUtils.calculateHash((SerializableEntity) block);
		Mockito.when(context.harvester.harvestBlock()).thenReturn(block);
		Mockito.when(context.blockChain.processBlock(Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.harvest();

		// Assert:
		Mockito.verify(context.harvester, Mockito.only()).harvestBlock();
		Mockito.verify(context.blockChain, Mockito.only()).processBlock(block);

		final ArgumentCaptor<SecureSerializableEntity> captor = ArgumentCaptor.forClass(SecureSerializableEntity.class);
		Mockito.verify(context.network, Mockito.times(1)).broadcast(Mockito.eq(NisPeerId.REST_PUSH_BLOCK), captor.capture());

		final Hash entityHash = HashUtils.calculateHash(captor.getValue().getEntity());
		MatcherAssert.assertThat(entityHash, IsEqual.equalTo(blockHash));
		MatcherAssert.assertThat(captor.getValue().getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
	}

	private static Block createSignedBlock() {
		final Block block = NisUtils.createRandomBlock();
		block.sign();
		return block;
	}

	private static class TestContext {
		private final BlockChain blockChain = Mockito.mock(BlockChain.class);
		private final Harvester harvester = Mockito.mock(Harvester.class);
		private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);

		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final NodeIdentity localNodeIdentity = new NodeIdentity(new KeyPair());
		private final TimeInstant currentTime = new TimeInstant(44544);

		private final HarvestingTask task = new HarvestingTask(this.blockChain, this.harvester, this.unconfirmedTransactions);

		public TestContext() {
			Mockito.when(this.network.getLocalNode()).thenReturn(new Node(this.localNodeIdentity, NodeEndpoint.fromHost("10.0.0.1")));
		}

		public void harvest() {
			this.task.harvest(this.network, this.currentTime);
		}
	}
}
