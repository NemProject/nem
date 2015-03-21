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
import org.nem.nis.controller.requests.AuthenticatedUnconfirmedTransactionsRequest;
import org.nem.nis.harvesting.UnconfirmedTransactionsFilter;
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
	public void transactionPrepareAnnounceSignsAndPushesNonMultisigTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesNonMultisigTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionPrepareAnnounceSignsAndPushesNonMultisigTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesNonMultisigTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionPrepareAnnounceSignsAndPushesNonMultisigTransaction(final ValidationResult validationResult) {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesTransaction(
				validationResult,
				TransactionControllerTest::createTransactionWithSender,
				null);
	}

	@Test
	public void transactionPrepareAnnounceSignsAndPushesMultisigTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesMultisigTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionPrepareAnnounceSignsAndPushesMultisigTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesMultisigTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionPrepareAnnounceSignsAndPushesMultisigTransaction(final ValidationResult validationResult) {
		// Arrange:
		final Transaction innerTransaction = createTransaction();

		// Assert:
		assertTransactionPrepareAnnounceSignsAndPushesTransaction(
				validationResult,
				signer -> new MultisigTransaction(TimeInstant.ZERO, signer, innerTransaction),
				HashUtils.calculateHash(innerTransaction));
	}

	private static void assertTransactionPrepareAnnounceSignsAndPushesTransaction(
			final ValidationResult validationResult,
			final Function<Account, Transaction> createTransaction,
			final Hash expectedInnerTransactionHash) {
		final TestContext context = new TestContext();
		Mockito.when(context.pushService.pushTransaction(Mockito.any(), Mockito.any()))
				.thenReturn(validationResult);

		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final Transaction transaction = createTransaction.apply(account);
		final Hash expectedTransactionHash = HashUtils.calculateHash(transaction);
		final RequestPrepareAnnounce request = new RequestPrepareAnnounce(transaction, keyPair.getPrivateKey());

		// Act:
		final NemAnnounceResult result = context.controller.transactionPrepareAnnounce(request);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(validationResult.getValue()));
		Assert.assertThat(result.getTransactionHash(), IsEqual.equalTo(expectedTransactionHash));
		Assert.assertThat(result.getInnerTransactionHash(), IsEqual.equalTo(expectedInnerTransactionHash));

		final ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		Mockito.verify(context.pushService, Mockito.only()).pushTransaction(transactionCaptor.capture(), Mockito.eq(null));

		final Transaction pushedTransaction = transactionCaptor.getValue();
		Assert.assertThat(pushedTransaction.getSignature(), IsNull.notNullValue());
		Assert.assertThat(pushedTransaction.verify(), IsEqual.equalTo(true));
	}

	//endregion

	//region transactionAnnounce

	@Test
	public void transactionAnnounceSignsAndPushesNonMultisigTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesNonMultisigTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionAnnounceSignsAndPushesNonMultisigTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesNonMultisigTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionAnnounceSignsAndPushesNonMultisigTransaction(final ValidationResult validationResult) {
		// Assert:
		assertTransactionAnnounceSignsAndPushesTransaction(
				validationResult,
				createTransaction(),
				null);
	}

	@Test
	public void transactionAnnounceSignsAndPushesMultisigTransactionIfTransactionPassesValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesMultisigTransaction(ValidationResult.SUCCESS);
	}

	@Test
	public void transactionAnnounceSignsAndPushesMultisigTransactionIfTransactionFailsValidation() {
		// Assert:
		assertTransactionAnnounceSignsAndPushesMultisigTransaction(ValidationResult.FAILURE_FUTURE_DEADLINE);
	}

	private static void assertTransactionAnnounceSignsAndPushesMultisigTransaction(final ValidationResult validationResult) {
		// Arrange:
		final Transaction innerTransaction = createTransaction();

		// Assert:
		assertTransactionAnnounceSignsAndPushesTransaction(
				validationResult,
				new MultisigTransaction(TimeInstant.ZERO, Utils.generateRandomAccount(), innerTransaction),
				HashUtils.calculateHash(innerTransaction));
	}

	private static void assertTransactionAnnounceSignsAndPushesTransaction(
			final ValidationResult validationResult,
			final Transaction transaction,
			final Hash expectedInnerTransactionHash) {
		final TestContext context = new TestContext();
		Mockito.when(context.pushService.pushTransaction(Mockito.any(), Mockito.any()))
				.thenReturn(validationResult);

		final Signature signature = new Signature(Utils.generateRandomBytes(64));
		final RequestAnnounce requestAnnounce = new RequestAnnounce(
				BinarySerializer.serializeToBytes(transaction.asNonVerifiable()),
				signature.getBytes());

		// Act:
		final NemAnnounceResult result = context.controller.transactionAnnounce(requestAnnounce);

		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_VALIDATION_RESULT));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(validationResult.getValue()));
		Assert.assertThat(result.getInnerTransactionHash(), IsEqual.equalTo(expectedInnerTransactionHash));

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
				c -> c.controller.transactionsUnconfirmed(new AuthenticatedUnconfirmedTransactionsRequest(challenge)),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runTransactionsUnconfirmedTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, SerializableList<Transaction>> getUnconfirmedTransactions) {
		// Arrange:
		Mockito.when(context.unconfirmedTransactions.getUnknownTransactions(Mockito.any())).thenReturn(createTransactionList());

		// Act:
		final T result = action.apply(context);
		final SerializableList<Transaction> transactions = getUnconfirmedTransactions.apply(result);

		// Assert:
		Assert.assertThat(transactions.size(), IsEqual.equalTo(1));
		Assert.assertThat(transactions.get(0).getTimeStamp(), IsEqual.equalTo(new TimeInstant(321)));
		Mockito.verify(context.unconfirmedTransactions, Mockito.times(1)).getUnknownTransactions(Mockito.any());
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
		private final UnconfirmedTransactionsFilter unconfirmedTransactions = Mockito.mock(UnconfirmedTransactionsFilter.class);
		private final SingleTransactionValidator validator = Mockito.mock(SingleTransactionValidator.class);
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final TransactionController controller;
		private final DebitPredicate debitPredicate = Mockito.mock(DebitPredicate.class);

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			Mockito.when(this.accountLookup.findByAddress(Mockito.any()))
					.thenAnswer(invocationOnMock -> new Account((Address)invocationOnMock.getArguments()[0]));

			this.controller = new TransactionController(
					this.accountLookup,
					this.pushService,
					this.unconfirmedTransactions,
					this.validator,
					this.host,
					this.debitPredicate);
		}
	}
}
