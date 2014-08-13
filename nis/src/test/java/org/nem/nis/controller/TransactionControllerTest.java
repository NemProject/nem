package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.service.PushService;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.test.PeerUtils;

import java.util.*;
import java.util.function.Function;

public class TransactionControllerTest {
	//region unconfirmed

	@Test
	public void transactionsUnconfirmedReturnsListOfUnconfirmedTransactions() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runTransactionsUnconfirmedTest(
				context,
				c -> c.controller.transactionsUnconfirmed(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runTransactionsUnconfirmedTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, SerializableList<Transaction>> getUnconfirmedTransactions) {
		// Arrange:
		Mockito.when(context.foraging.getUnconfirmedTransactionsForNewBlock(Mockito.any())).thenReturn(createTransactionList());

		// Act:
		final T result = action.apply(context);
		final SerializableList<Transaction> transactions = getUnconfirmedTransactions.apply(result);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(0).getTimeStamp(), IsEqual.equalTo(new TimeInstant(321)));
		Mockito.verify(context.foraging, Mockito.times(1)).getUnconfirmedTransactionsForNewBlock(Mockito.any());
		return result;
	}

	//endregion

	private static List<Transaction> createTransactionList() {
		final Account sender = Utils.generateRandomAccount();
		final Account recipient = Utils.generateRandomAccount();
		final TransferTransaction transaction = new TransferTransaction(new TimeInstant(321), sender, recipient, Amount.fromNem(100), null);
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(transaction);
		return transactions;
	}

	private static class TestContext {
		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final PushService pushService = Mockito.mock(PushService.class);
		private final Foraging foraging = Mockito.mock(Foraging.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final TransactionController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(PeerUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new TransactionController(
					this.accountLookup,
					this.pushService,
					this.foraging,
					this.host);
		}
	}
}
