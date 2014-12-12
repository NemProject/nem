package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.cache.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.service.PushService;
import org.nem.nis.validators.*;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;

import java.util.*;
import java.util.function.Function;

public class TransactionControllerTest {

	//region transactionPrepare

	@Test
	@SuppressWarnings("deprecation")
	public void transactionPrepareFailsIfTransactionDataFailsValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.FAILURE_ENTITY_UNUSABLE);

		final Transaction transaction = createTransaction();
		final Deserializer deserializer = Utils.createDeserializer(JsonSerializer.serializeToJson(transaction.asNonVerifiable()));

		// Act:
		ExceptionAssert.assertThrows(
				v -> context.controller.transactionPrepare(deserializer),
				IllegalArgumentException.class);
		Mockito.verify(context.validator, Mockito.only()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void transactionPrepareSucceedsIfTransactionDataPassesValidation() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		final Transaction transaction = createTransaction();
		final Deserializer deserializer = Utils.createDeserializer(JsonSerializer.serializeToJson(transaction.asNonVerifiable()));

		// Act:
		final RequestPrepare requestPrepare = context.controller.transactionPrepare(deserializer);

		// Assert:
		Assert.assertThat(requestPrepare.getData(), IsEqual.equalTo(BinarySerializer.serializeToBytes(transaction.asNonVerifiable())));
		Mockito.verify(context.validator, Mockito.only()).validate(Mockito.any(), Mockito.any());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void transactionPreparePassesCorrectValidationContextToValidator() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.validator.validate(Mockito.any(), Mockito.any())).thenReturn(ValidationResult.SUCCESS);

		final Transaction transaction = createTransaction();
		final Deserializer deserializer = Utils.createDeserializer(JsonSerializer.serializeToJson(transaction.asNonVerifiable()));

		// Act:
		context.controller.transactionPrepare(deserializer);

		// Assert:
		final ArgumentCaptor<ValidationContext> validationContextCaptor = ArgumentCaptor.forClass(ValidationContext.class);
		Mockito.verify(context.validator, Mockito.only()).validate(Mockito.any(), validationContextCaptor.capture());

		final ValidationContext validationContext = validationContextCaptor.getValue();
		Assert.assertThat(validationContext.getBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(validationContext.getConfirmedBlockHeight(), IsEqual.equalTo(BlockHeight.MAX));
		Assert.assertThat(validationContext.getDebitPredicate(), IsEqual.equalTo(context.debitPredicate));
	}

	//endregion

	//region transactionPrepareAnnounce

	@Test
	public void transactionPrepareAnnounceSignsAndPushesTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionPrepareAnnounceSignsAndPushesTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionPrepareAnnounceSignsAndPushesTransaction(final ValidationResult validationResult) {
		final TestContext context = new TestContext();
		Mockito.when(context.pushService.pushTransaction(Mockito.any(), Mockito.any()))
				.thenReturn(validationResult);

		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final Transaction transaction = createTransactionWithSender(account);
		final RequestPrepareAnnounce request = new RequestPrepareAnnounce(transaction, keyPair.getPrivateKey());

		// Act:
		final NemRequestResult result = context.controller.transactionPrepareAnnounce(request);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(validationResult.getValue()));

		final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		Mockito.verify(context.pushService, Mockito.only()).pushTransaction(transactionCaptor.capture(), Mockito.eq(null));

		final Transaction pushedTransaction = transactionCaptor.getValue();
		Assert.assertThat(pushedTransaction.getSignature(), IsNull.notNullValue());
		Assert.assertThat(pushedTransaction.verify(), IsEqual.equalTo(true));
	}

	//endregion

	//region transactionAnnounce

	@Test
	public void transactionAnnounceSignsAndPushesTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionAnnounceSignsAndPushesTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionAnnounceSignsAndPushesTransaction(final ValidationResult validationResult) {
		final TestContext context = new TestContext();
		Mockito.when(context.pushService.pushTransaction(Mockito.any(), Mockito.any()))
				.thenReturn(validationResult);

		final Transaction transaction = createTransaction();
		final Signature signature = new Signature(Utils.generateRandomBytes(64));
		final RequestAnnounce requestAnnounce = new RequestAnnounce(
				BinarySerializer.serializeToBytes(transaction.asNonVerifiable()),
				signature.getBytes());

		// Act:
		final NemRequestResult result = context.controller.transactionAnnounce(requestAnnounce);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(validationResult.getValue()));

		final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		Mockito.verify(context.pushService, Mockito.only()).pushTransaction(transactionCaptor.capture(), Mockito.eq(null));
		Assert.assertThat(transactionCaptor.getValue().getSignature(), IsEqual.equalTo(signature));
	}

	//endregion

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
		Mockito.when(context.unconfirmedTransactions.getAll()).thenReturn(createTransactionList());

		// Act:
		final T result = action.apply(context);
		final SerializableList<Transaction> transactions = getUnconfirmedTransactions.apply(result);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(0).getTimeStamp(), IsEqual.equalTo(new TimeInstant(321)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).getAll();
		return result;
	}

	//endregion

	private static Transaction createTransaction() {
		return createTransactionWithSender(Utils.generateRandomAccount());
	}

	private static Transaction createTransactionWithSender(final Account sender) {
		final Account recipient = Utils.generateRandomAccount();
		return new TransferTransaction(new TimeInstant(321), sender, recipient, Amount.fromNem(100), null);
	}

	private static List<Transaction> createTransactionList() {
		final List<Transaction> transactions = new ArrayList<>();
		transactions.add(createTransaction());
		return transactions;
	}

	private static class TestContext {
		private final AccountLookup accountLookup = Mockito.mock(AccountLookup.class);
		private final PushService pushService = Mockito.mock(PushService.class);
		private final UnconfirmedTransactions unconfirmedTransactions = Mockito.mock(UnconfirmedTransactions.class);
		private final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final TransactionController controller;
		private final DebitPredicate debitPredicate;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.debitPredicate = Mockito.mock(DebitPredicate.class);
			final AccountStateRepository accountStateRepository = Mockito.mock(PoiFacade.class);
			Mockito.when(accountStateRepository.getDebitPredicate()).thenReturn(this.debitPredicate);

			this.controller = new TransactionController(
					this.accountLookup,
					this.pushService,
					this.unconfirmedTransactions,
					this.validator,
					this.host,
					accountStateRepository);
		}
	}
}
