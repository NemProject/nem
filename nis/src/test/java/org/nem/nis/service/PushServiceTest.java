package org.nem.nis.service;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.SerializableEntity;
import org.nem.core.test.*;
import org.nem.core.time.*;
import org.nem.nis.BlockChain;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.test.NisUtils;
import org.nem.peer.*;

public class PushServiceTest {
	private static final int BASE_TIME = 1122448;

	@After
	public void resetNetwork() {
		NetworkInfos.setDefault(null);
	}

	// region pushTransaction

	@Test
	public void pushTransactionValidSuccessAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidSuccessAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushTransactionValidSuccessAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		final TestContext context = assertPushTransactionExperienceChange(ValidationResult.SUCCESS);

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.networkBroadcastBuffer, Mockito.times(1)).queue(Mockito.eq(NisPeerId.REST_PUSH_TRANSACTIONS),
				broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>) (broadcastEntityArgument.getValue());
		MatcherAssert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		MatcherAssert.assertThat(((MockTransaction) secureEntity.getEntity()).getCustomField(), IsEqual.equalTo(12));
	}

	private static TestContext assertPushTransactionExperienceChange(final ValidationResult result) {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.sign();

		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(result);

		// Act:
		context.service.pushTransaction(transaction, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(result);
		return context;
	}

	// endregion

	// region pushBlock

	@Test
	public void pushBlockValidFailureIncrementsFailedExperience() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(12);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.FAILURE_UNKNOWN);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidFailureEntityUnusableDoesNotChangeExperienceAndDoesNotGetProcessedOrBroadcast() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(12);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.FAILURE_ENTITY_UNUSABLE_OUT_OF_SYNC);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertNoUpdateExperience();
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
		Mockito.verify(context.blockChain, Mockito.times(0)).processBlock(Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.NEUTRAL, ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidSuccessAcceptedFailureIncrementsFailedExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.SUCCESS, ValidationResult.FAILURE_UNKNOWN);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.NEUTRAL, ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidSuccessAcceptedNeutralDoesNotChangeExperience() {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(ValidationResult.SUCCESS, ValidationResult.NEUTRAL);
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushBlockValidNeutralAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(ValidationResult.NEUTRAL);
	}

	@Test
	public void pushBlockValidSuccessAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts() {
		// Assert:
		assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(ValidationResult.SUCCESS);
	}

	private static void assertPushBlockAcceptedSuccessIncrementsSuccessExperienceAndBroadcasts(final ValidationResult validResult) {
		// Assert:
		final TestContext context = assertPushBlockExperienceChange(validResult, ValidationResult.SUCCESS);

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.network, Mockito.times(1)).broadcast(Mockito.eq(NisPeerId.REST_PUSH_BLOCK),
				broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>) (broadcastEntityArgument.getValue());
		MatcherAssert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		MatcherAssert.assertThat(((Block) secureEntity.getEntity()).getHeight(), IsEqual.equalTo(new BlockHeight(14)));
	}

	private static TestContext assertPushBlockExperienceChange(final ValidationResult validResult, final ValidationResult acceptedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(14);
		block.sign();

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(validResult);
		Mockito.when(context.blockChain.processBlock(block)).thenReturn(acceptedResult);

		// Act:
		context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		context.assertSingleUpdateExperience(acceptedResult);
		return context;
	}

	// endregion

	// region other

	@Test
	public void pushCanFailForUnidentifiedNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.sign();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.FAILURE_FUTURE_DEADLINE);

		// Act:
		context.service.pushTransaction(transaction, null);

		// Assert:
		context.assertNoUpdateExperience();
		Mockito.verify(context.network, Mockito.times(0)).broadcast(Mockito.any(), Mockito.any());
	}

	@Test
	public void pushCanSucceedForUnidentifiedNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final MockTransaction transaction = new MockTransaction(Utils.generateRandomAccount(), 12);
		transaction.sign();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		context.service.pushTransaction(transaction, null);

		// Assert:
		context.assertNoUpdateExperience();

		final ArgumentCaptor<SerializableEntity> broadcastEntityArgument = ArgumentCaptor.forClass(SerializableEntity.class);
		Mockito.verify(context.networkBroadcastBuffer, Mockito.times(1)).queue(Mockito.eq(NisPeerId.REST_PUSH_TRANSACTIONS),
				broadcastEntityArgument.capture());

		final SecureSerializableEntity<?> secureEntity = (SecureSerializableEntity<?>) (broadcastEntityArgument.getValue());
		MatcherAssert.assertThat(secureEntity.getIdentity(), IsEqual.equalTo(context.localNodeIdentity));
		MatcherAssert.assertThat(((MockTransaction) secureEntity.getEntity()).getCustomField(), IsEqual.equalTo(12));
	}

	@Test
	public void pushTransactionCachesTransactionIfValidationSucceeds() {
		assertPushServiceTransactionCaching(ValidationResult.SUCCESS, ValidationResult.NEUTRAL, 1);
	}

	@Test
	public void pushTransactionCachesTransactionIfValidationFails() {
		// Assert:
		assertPushServiceTransactionCaching(ValidationResult.FAILURE_CHAIN_INVALID, ValidationResult.FAILURE_CHAIN_INVALID, 1);
	}

	private static void assertPushServiceTransactionCaching(final ValidationResult transactionValidationResult,
			final ValidationResult expectedValidationResult, final int expectedNumberOfInvocations) {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = createMockTransaction();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(transactionValidationResult);
		context.setAddTimeStamps(500, 2700);

		// Act:
		// initial push (cached validation result should NOT be used)
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp 500 seconds later than first one --> first transaction not pruned
		// (cached validation result should be used)
		final ValidationResult result = context.service.pushTransaction(transaction, null);

		// Assert:
		// transaction validation should have only occurred the expected number of times
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(expectedNumberOfInvocations)).addNew(Mockito.any());
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedValidationResult));
	}

	@Test
	public void pushTransactionPrunesTransactionHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Transaction transaction = createMockTransaction();
		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);
		context.setAddTimeStamps(500, 2700);

		// Act:
		// initial push (cached validation result should NOT be used)
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp 500 seconds later than first one --> first transaction not pruned.
		// (cached validation result should be used)
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp 2200 seconds later than second one --> first and second transaction pruned.
		// (cached validation result should NOT be used)
		context.service.pushTransaction(transaction, null);

		// time provider supplies time stamp same as third one --> third transaction not pruned.
		// (cached validation result should be used)
		context.service.pushTransaction(transaction, null);

		// Assert:
		// transaction validation should have only occurred twice
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(2)).addNew(Mockito.any());
	}

	private static Transaction createMockTransaction() {
		final MockTransaction mockTransaction = new MockTransaction(12, new TimeInstant(BASE_TIME));
		mockTransaction.sign();
		return mockTransaction;
	}

	@Test
	public void pushBlockCachesBlockIfValidationSucceeds() {
		assertPushServiceBlockCaching(ValidationResult.SUCCESS, 1);
	}

	@Test
	public void pushBlockCachesBlockIfValidationFails() {
		assertPushServiceBlockCaching(ValidationResult.FAILURE_CHAIN_INVALID, 1);
	}

	private static void assertPushServiceBlockCaching(final ValidationResult blockValidationResult, final int expectedNumberOfInvocations) {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithHeight(12);
		block.sign();
		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(blockValidationResult);
		Mockito.when(context.blockChain.processBlock(block)).thenReturn(ValidationResult.SUCCESS);
		context.setAddTimeStamps(5000, 16000);

		// Act:
		// initial push (cached validation result should NOT be used)
		context.service.pushBlock(block, null);

		// time provider supplies time stamp 5000 seconds later than first one --> first block not pruned
		// (cached validation result should be used)
		context.service.pushBlock(block, null);

		// Assert:
		Mockito.verify(context.blockChain, Mockito.times(expectedNumberOfInvocations)).checkPushedBlock(block);
	}

	@Test
	public void pushBlockPrunesBlockHashCache() {
		// Arrange:
		final TestContext context = new TestContext();
		final Block block = NisUtils.createRandomBlockWithTimeStamp(BASE_TIME);
		block.sign();
		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.SUCCESS);
		Mockito.when(context.blockChain.processBlock(block)).thenReturn(ValidationResult.SUCCESS);
		context.setAddTimeStamps(5000, 14000);

		// Act:
		// initial push (cached validation result should NOT be used)
		context.service.pushBlock(block, null);

		// time provider supplies time stamp 5000 seconds later than first one --> first block not pruned.
		context.service.pushBlock(block, null);

		// time provider supplies time stamp 9000 seconds later than second one --> first and second block pruned.
		context.service.pushBlock(block, null);

		// time provider supplies time stamp same as third one --> third block not pruned.
		context.service.pushBlock(block, null);

		// Assert:
		// transaction validation should have only occurred twice
		Mockito.verify(context.blockChain, Mockito.times(2)).checkPushedBlock(block);
	}

	// endregion

	// region reject entities from different network

	@Test
	public void pushTransactionRejectsTransactionsFromDifferentNetwork() {
		// Assert:
		assertTransactionNetworkCheck(NetworkInfos.getTestNetworkInfo(), NetworkInfos.getMainNetworkInfo(),
				ValidationResult.FAILURE_WRONG_NETWORK);
	}

	@Test
	public void pushTransactionAcceptsTransactionsFromSameNetwork() {
		// Assert:
		assertTransactionNetworkCheck(NetworkInfos.getTestNetworkInfo(), NetworkInfos.getTestNetworkInfo(), ValidationResult.SUCCESS);
	}

	private static void assertTransactionNetworkCheck(final NetworkInfo localNetwork, final NetworkInfo remoteNetwork,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		setNetworkInfo(remoteNetwork);
		final Transaction transaction = new MockTransaction();
		transaction.sign();
		setNetworkInfo(localNetwork);

		Mockito.when(context.unconfirmedTransactions.addNew(transaction)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.service.pushTransaction(transaction, context.remoteNodeIdentity);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	@Test
	public void pushBlockRejectsBlocksFromDifferentNetwork() {
		// Assert:
		assertBlockNetworkCheck(NetworkInfos.getTestNetworkInfo(), NetworkInfos.getMainNetworkInfo(),
				ValidationResult.FAILURE_WRONG_NETWORK);
	}

	@Test
	public void pushBlockAcceptsBlocksFromSameNetwork() {
		// Assert:
		assertBlockNetworkCheck(NetworkInfos.getTestNetworkInfo(), NetworkInfos.getTestNetworkInfo(), ValidationResult.SUCCESS);
	}

	private static void assertBlockNetworkCheck(final NetworkInfo localNetwork, final NetworkInfo remoteNetwork,
			final ValidationResult expectedResult) {
		// Arrange:
		final TestContext context = new TestContext();
		setNetworkInfo(remoteNetwork);
		final Block block = NisUtils.createRandomBlockWithTimeStamp(BASE_TIME);
		block.sign();
		setNetworkInfo(localNetwork);

		Mockito.when(context.blockChain.checkPushedBlock(block)).thenReturn(ValidationResult.SUCCESS);
		Mockito.when(context.blockChain.processBlock(block)).thenReturn(ValidationResult.SUCCESS);

		// Act:
		final ValidationResult result = context.service.pushBlock(block, context.remoteNodeIdentity);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(expectedResult));
	}

	// endregion

	private static void setNetworkInfo(final NetworkInfo info) {
		NetworkInfos.setDefault(null);
		NetworkInfos.setDefault(info);
	}

	private static class TestContext {
		private final NodeIdentity remoteNodeIdentity;
		private final Node remoteNode;
		private final NodeIdentity localNodeIdentity;
		private final PeerNetwork network;
		private final PeerNetworkBroadcastBuffer networkBroadcastBuffer;
		private final UnconfirmedTransactions unconfirmedTransactions;
		private final BlockChain blockChain;
		private final TimeProvider timeProvider;
		private final PushService service;

		public TestContext() {
			this.remoteNodeIdentity = new NodeIdentity(new KeyPair());
			this.remoteNode = new Node(this.remoteNodeIdentity, NodeEndpoint.fromHost("10.0.0.1"));

			this.localNodeIdentity = new NodeIdentity(new KeyPair());
			final Node localNode = new Node(this.localNodeIdentity, NodeEndpoint.fromHost("10.0.0.2"));

			final NodeCollection collection = new NodeCollection();
			collection.update(this.remoteNode, NodeStatus.ACTIVE);

			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getNodes()).thenReturn(collection);
			Mockito.when(this.network.getLocalNode()).thenReturn(localNode);

			this.networkBroadcastBuffer = Mockito.mock(PeerNetworkBroadcastBuffer.class);

			this.unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
			this.blockChain = Mockito.mock(BlockChain.class);
			this.timeProvider = Mockito.mock(TimeProvider.class);
			this.setAddTimeStamps(0, 0);

			final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(host.getNetwork()).thenReturn(this.network);
			Mockito.when(host.getNetworkBroadcastBuffer()).thenReturn(this.networkBroadcastBuffer);

			this.service = new PushService(this.unconfirmedTransactions, this.blockChain, host, this.timeProvider);
		}

		public void setAddTimeStamps(final int delta1, final int delta2) {
			Mockito.when(this.timeProvider.getCurrentTime()).thenReturn(new TimeInstant(BASE_TIME), new TimeInstant(BASE_TIME),
					new TimeInstant(BASE_TIME + delta1), new TimeInstant(BASE_TIME + delta1), new TimeInstant(BASE_TIME + delta2));
		}

		public void assertNoUpdateExperience() {
			Mockito.verify(this.network, Mockito.times(0)).updateExperience(Mockito.any(), Mockito.any());
		}

		public void assertSingleUpdateExperience(final ValidationResult expectedResult) {
			Mockito.verify(this.network, Mockito.times(1)).updateExperience(this.remoteNode,
					NodeInteractionResult.fromValidationResult(expectedResult));
			Mockito.verify(this.network, Mockito.times(1)).updateExperience(Mockito.eq(this.remoteNode), Mockito.any());
		}
	}
}
