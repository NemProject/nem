package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.ncc.*;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.*;
import org.nem.nis.harvesting.UnconfirmedTransactions;
import org.nem.nis.service.PushService;
import org.nem.nis.validators.*;
import org.nem.peer.node.AuthenticatedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
public class TransactionController {
	private final AccountLookup accountLookup;
	private final PushService pushService;
	private final UnconfirmedTransactions unconfirmedTransactions;
	private final SingleTransactionValidator validator;
	private final NisPeerNetworkHost host;
	private final DebitPredicate debitPredicate;

	@Autowired(required = true)
	TransactionController(
			final AccountLookup accountLookup,
			final PushService pushService,
			final UnconfirmedTransactions unconfirmedTransactions,
			final SingleTransactionValidator validator,
			final NisPeerNetworkHost host,
			final DebitPredicate debitPredicate) {
		this.accountLookup = accountLookup;
		this.pushService = pushService;
		this.unconfirmedTransactions = unconfirmedTransactions;
		this.validator = validator;
		this.host = host;
		this.debitPredicate = debitPredicate;
	}

	/**
	 * A request for NIS to serialize unsigned transaction data into binary for signing by the client.
	 * <br/>
	 * This is insecure if an attacker modifies the binary payload in-between NIS and the client.
	 *
	 * @param deserializer The deserializer that is expected to contain transaction data.
	 * @return The binary form of the transaction data.
	 */
	@RequestMapping(value = "/transaction/prepare", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	@Deprecated
	public RequestPrepare transactionPrepare(@RequestBody final Deserializer deserializer) {
		final Transaction transfer = deserializeTransaction(deserializer);

		final ValidationContext context = new ValidationContext(this.debitPredicate);
		final ValidationResult validationResult = this.validator.validate(transfer, context);
		if (!validationResult.isSuccess()) {
			throw new IllegalArgumentException(validationResult.toString());
		}

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
		return new RequestPrepare(transferData);
	}

	/**
	 * A request for NIS to sign unsigned transaction data and announce it given a private key.
	 * <br/>
	 * This is insecure if an attacker modifies the binary payload in-between NIS and the client
	 * (not to mention it exposes the private key).
	 *
	 * @param request The request, which contains the transaction data and a private key.
	 * @return The result of the operation.
	 */
	@RequestMapping(value = "/transaction/prepare-announce", method = RequestMethod.POST)
	@TrustedApi
	@ClientApi
	public NemRequestResult transactionPrepareAnnounce(@RequestBody final RequestPrepareAnnounce request) {
		final Account account = new Account(new KeyPair(request.getPrivateKey()));
		final Transaction transfer = request.getTransaction();
		transfer.signBy(account);
		return this.push(transfer);
	}

	/**
	 * A request for NIS to announce signed transaction data.
	 *
	 * @param request The request, which contains the transaction data and a signature.
	 * @return The result of the operation.
	 */
	@RequestMapping(value = "/transaction/announce", method = RequestMethod.POST)
	@ClientApi
	public NemRequestResult transactionAnnounce(@RequestBody final RequestAnnounce request) {
		final Transaction transfer = this.deserializeTransaction(request.getData());
		transfer.setSignature(new Signature(request.getSignature()));
		return this.push(transfer);
	}

	private NemRequestResult push(final Transaction transaction) {
		final ValidationResult result = this.pushService.pushTransaction(transaction, null);

		// TODO 20150108 J-G - i guess this is just for logging?
		if ((transaction instanceof MultisigTransaction) && (result == ValidationResult.SUCCESS)) {
			return new NemRequestResult(
					NemRequestResult.TYPE_VALIDATION_RESULT,
					result.getValue(),
					result.toString() + ":" + HashUtils.calculateHash(((MultisigTransaction)transaction).getOtherTransaction()).toString());
		}
		return new NemRequestResult(result);
	}

	/**
	 * Gets unconfirmed transaction information.
	 *
	 * @param request The authenticated unconfirmed transactions request.
	 * @return List of unconfirmed transactions.
	 */
	@RequestMapping(value = "/transactions/unconfirmed", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Transaction>> transactionsUnconfirmed(@RequestBody final AuthenticatedUnconfirmedTransactionsRequest request) {
		final SerializableList<Transaction> transactions = new SerializableList<>(this.getUnconfirmedTransactions(request.getEntity()));
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(transactions, localNode.getIdentity(), request.getChallenge());
	}

	private Collection<Transaction> getUnconfirmedTransactions(final UnconfirmedTransactionsRequest request) {
		return this.unconfirmedTransactions.getUnknownTransactions(request.getHashShortIds());
	}

	private Transaction deserializeTransaction(final byte[] bytes) {
		return ExceptionUtils.propagate(() -> {
			try (final BinaryDeserializer dataDeserializer = getDeserializer(bytes, this.accountLookup)) {
				return deserializeTransaction(dataDeserializer);
			}
		});
	}

	private static Transaction deserializeTransaction(final Deserializer deserializer) {
		return TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);
	}

	private static BinaryDeserializer getDeserializer(final byte[] bytes, final AccountLookup accountLookup) {
		return new BinaryDeserializer(bytes, new DeserializationContext(accountLookup));
	}
}
