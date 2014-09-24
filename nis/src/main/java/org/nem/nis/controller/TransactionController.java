package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.node.Node;
import org.nem.core.serialization.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.harvesting.Foraging;
import org.nem.nis.service.PushService;
import org.nem.nis.validators.TransactionValidator;
import org.nem.peer.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

// TODO: add tests
@RestController
public class TransactionController {
	private final AccountLookup accountLookup;
	private final PushService pushService;
	private final Foraging foraging;
	private final TransactionValidator validator;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	TransactionController(
			final AccountLookup accountLookup,
			final PushService pushService,
			final Foraging foraging,
			final TransactionValidator validator,
			final NisPeerNetworkHost host) {
		this.accountLookup = accountLookup;
		this.pushService = pushService;
		this.foraging = foraging;
		this.validator = validator;
		this.host = host;
	}

	@RequestMapping(value = "/transaction/prepare", method = RequestMethod.POST)
	@ClientApi
	@Deprecated
	public RequestPrepare transactionPrepare(@RequestBody final Deserializer deserializer) {
		final Transaction transfer = deserializeTransaction(deserializer);

		final ValidationResult validationResult = this.validator.validate(transfer);
		if (ValidationResult.SUCCESS != validationResult) {
			throw new IllegalArgumentException(validationResult.toString());
		}

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
		return new RequestPrepare(transferData);
	}

	@RequestMapping(value = "/transaction/announce", method = RequestMethod.POST)
	@ClientApi
	public NemRequestResult transactionAnnounce(@RequestBody final RequestAnnounce requestAnnounce) throws Exception {
		final Transaction transfer = this.deserializeTransaction(requestAnnounce.getData());
		transfer.setSignature(new Signature(requestAnnounce.getSignature()));
		final ValidationResult result = this.pushService.pushTransaction(transfer, null);
		return new NemRequestResult(result);
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

	private Transaction deserializeTransaction(final byte[] bytes) throws Exception {
		try (final BinaryDeserializer dataDeserializer = getDeserializer(bytes, this.accountLookup)) {
			return deserializeTransaction(dataDeserializer);
		}
	}

	private static Transaction deserializeTransaction(final Deserializer deserializer) {
		return TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);
	}

	private static BinaryDeserializer getDeserializer(final byte[] bytes, final AccountLookup accountLookup) {
		return new BinaryDeserializer(bytes, new DeserializationContext(accountLookup));
	}
}
