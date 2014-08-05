package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NisRequestResult;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.PushService;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.logging.Logger;

// TODO: add tests
@RestController
public class TransactionController {
	private static final Logger LOGGER = Logger.getLogger(TransactionController.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final PushService pushService;
	private final Foraging foraging;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	TransactionController(
			final AccountAnalyzer accountAnalyzer,
			final PushService pushService,
			final Foraging foraging,
			final NisPeerNetworkHost host) {
		this.accountAnalyzer = accountAnalyzer;
		this.pushService = pushService;
		this.foraging = foraging;
		this.host = host;
	}

	@RequestMapping(value = "/transaction/prepare", method = RequestMethod.POST)
	@ClientApi
	public RequestPrepare transactionPrepare(@RequestBody final Deserializer deserializer) {
		final TransferTransaction transfer = deserializeTransaction(deserializer);

		final ValidationResult validationResult = transfer.checkValidity();
		if (ValidationResult.SUCCESS != transfer.checkValidity())
			throw new IllegalArgumentException(validationResult.toString());

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
		return new RequestPrepare(transferData);
	}

	@RequestMapping(value = "/transaction/announce", method = RequestMethod.POST)
	@ClientApi
	public NisRequestResult transactionAnnounce(@RequestBody final RequestAnnounce requestAnnounce) throws Exception {
		final TransferTransaction transfer = deserializeTransaction(requestAnnounce.getData());
		transfer.setSignature(new Signature(requestAnnounce.getSignature()));
		final ValidationResult result = this.pushService.pushTransaction(transfer, null);
		return new NisRequestResult(result);
	}

	/**
	 * Gets unconfirmed transaction information.
	 *
	 * @param challenge The node challenge.
	 * @return List of unconfirmed transactions.
	 */
	@RequestMapping(value = "/transactions/unconfirmed", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Transaction>> transactionsUnconfirmed(
			@RequestBody final NodeChallenge challenge) {
		final SerializableList<Transaction> transactions = new SerializableList<>(this.getUnconfirmedTransactions());
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(transactions, localNode.getIdentity(), challenge);
	}

	private Collection<Transaction> getUnconfirmedTransactions() {
		return this.foraging.getUnconfirmedTransactionsForNewBlock(NisMain.TIME_PROVIDER.getCurrentTime());
	}

	private TransferTransaction deserializeTransaction(final byte[] bytes) throws Exception {
		try (final BinaryDeserializer dataDeserializer = getDeserializer(bytes, this.accountAnalyzer)) {
			return deserializeTransaction(dataDeserializer);
		}
	}

	private static TransferTransaction deserializeTransaction(final Deserializer deserializer) {
		return (TransferTransaction)TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);
	}

	private static BinaryDeserializer getDeserializer(final byte[] bytes, final AccountLookup accountLookup) {
		return new BinaryDeserializer(bytes, new DeserializationContext(accountLookup));
	}
}
