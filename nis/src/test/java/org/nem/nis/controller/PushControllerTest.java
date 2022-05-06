package org.nem.nis.controller;

import org.junit.Test;
import org.mockito.Mockito;
import org.nem.core.crypto.KeyPair;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.node.NodeIdentity;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.service.PushService;
import org.nem.nis.test.NisUtils;
import org.nem.peer.SecureSerializableEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.stream.*;

public class PushControllerTest {

	// region pushTransaction

	@Test
	public void pushTransactionDelegatesToPushService() {
		// Arrange:
		final Transaction transaction = new TransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
				Utils.generateRandomAccount(), Amount.fromNem(11), null);

		// Assert:
		assertPushTransactionDelegatesToPushService(transaction);
	}

	@Test
	public void pushTransactionDelegatesToPushServiceForImportanceTransferTransactions() {
		// Arrange:
		final Transaction transaction = new ImportanceTransferTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(),
				ImportanceTransferMode.Activate, Utils.generateRandomAccount());

		// Assert:
		assertPushTransactionDelegatesToPushService(transaction);
	}

	private static void assertPushTransactionDelegatesToPushService(final Transaction transaction) {
		// Arrange:
		final PushService pushService = Mockito.mock(PushService.class);
		final PushController controller = new PushController(pushService);
		transaction.sign();

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = Utils.roundtripSerializableEntity(new SecureSerializableEntity<>(transaction, identity),
				new MockAccountLookup());

		// Act:
		controller.pushTransaction(deserializer);

		// Assert:
		Mockito.verify(pushService, Mockito.times(1)).pushTransaction(Mockito.any(), Mockito.eq(identity));
	}

	// endregion

	// region pushTransactions

	@Test
	public void pushTransactionsDelegatesToPushService() {
		// Arrange:
		final Collection<Transaction> transactions = IntStream.range(0, 5).mapToObj(i -> RandomTransactionFactory.createTransfer())
				.collect(Collectors.toList());

		// Assert:
		assertPushTransactionsDelegatesToPushService(transactions);
	}

	private static void assertPushTransactionsDelegatesToPushService(final Collection<Transaction> transactions) {
		// Arrange:
		final PushService pushService = Mockito.mock(PushService.class);
		final PushController controller = new PushController(pushService);
		transactions.forEach(Transaction::sign);

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = Utils.roundtripSerializableEntity(
				new SerializableList<>(
						transactions.stream().map(t -> new SecureSerializableEntity<>(t, identity)).collect(Collectors.toList())),
				new MockAccountLookup());

		// Act:
		controller.pushTransactions(deserializer, Mockito.mock(HttpServletRequest.class));

		// Assert:
		Mockito.verify(pushService, Mockito.times(transactions.size())).pushTransaction(Mockito.any(), Mockito.eq(identity));
	}

	// endregion

	// region pushBlock

	@Test
	public void pushBlockDelegatesToPushService() {
		// Arrange:
		final PushService pushService = Mockito.mock(PushService.class);
		final PushController controller = new PushController(pushService);

		final Block block = NisUtils.createRandomBlockWithHeight(123);
		block.sign();

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = Utils.roundtripSerializableEntity(new SecureSerializableEntity<>(block, identity),
				new MockAccountLookup());

		// Act:
		controller.pushBlock(deserializer);

		// Assert:
		Mockito.verify(pushService, Mockito.times(1)).pushBlock(Mockito.any(), Mockito.eq(identity));
	}

	// endregion
}
