package org.nem.nis.controller;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.service.PushService;
import org.nem.nis.test.NisUtils;
import org.nem.peer.SecureSerializableEntity;
import org.nem.peer.node.NodeIdentity;

public class PushControllerTest {

	@Test
	public void pushTransactionDelegatesToPushService() {
		// Arrange:
		final PushService pushService = Mockito.mock(PushService.class);
		final PushController controller = new PushController(pushService);

		final Transaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(11),
				null);
		transaction.sign();

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new SecureSerializableEntity<>(transaction, identity),
				new MockAccountLookup());

		// Act:
		controller.pushTransaction(deserializer);

		// Assert:
		Mockito.verify(pushService, Mockito.times(1)).pushTransaction(Mockito.any(), Mockito.eq(identity));
	}

	@Test
	public void pushBlockDelegatesToPushService() {
		// Arrange:
		final PushService pushService = Mockito.mock(PushService.class);
		final PushController controller = new PushController(pushService);

		final Block block = NisUtils.createRandomBlockWithHeight(123);
		block.sign();

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new SecureSerializableEntity<>(block, identity),
				new MockAccountLookup());

		// Act:
		controller.pushBlock(deserializer);

		// Assert:
		Mockito.verify(pushService, Mockito.times(1)).pushBlock(Mockito.any(), Mockito.eq(identity));
	}
}