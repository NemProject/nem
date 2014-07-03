package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NisRequestResult;
import org.nem.core.serialization.*;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.nis.service.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

// TODO: add tests
@RestController
public class TransactionController {
	private static final Logger LOGGER = Logger.getLogger(TransactionController.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final PushService pushService;

	@Autowired(required = true)
	TransactionController(
			final AccountAnalyzer accountAnalyzer,
			final PushService pushService) {
		this.accountAnalyzer = accountAnalyzer;
		this.pushService = pushService;
	}

	@RequestMapping(value = "/transfer/prepare", method = RequestMethod.POST)
	@ClientApi
	public RequestPrepare transferPrepare(@RequestBody final Deserializer deserializer) {
		final TransferTransaction transfer = deserializeTransaction(deserializer);

		final ValidationResult validationResult = transfer.checkValidity();
		if (ValidationResult.SUCCESS != transfer.checkValidity())
			throw new IllegalArgumentException(validationResult.toString());

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
		return new RequestPrepare(transferData);
	}

	@RequestMapping(value = "/transfer/announce", method = RequestMethod.POST)
	@ClientApi
	public NisRequestResult transferAnnounce(@RequestBody final RequestAnnounce requestAnnounce) throws Exception {
		final TransferTransaction transfer = deserializeTransaction(requestAnnounce.getData());
		transfer.setSignature(new Signature(requestAnnounce.getSignature()));
		ValidationResult result = this.pushService.pushTransaction(transfer, null);
		return new NisRequestResult(NisRequestResult.TYPE_VALIDATION_RESULT, result.getValue(), result.toString());
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
