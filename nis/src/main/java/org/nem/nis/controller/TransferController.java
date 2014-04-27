package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.model.TransactionFactory;
import org.nem.core.model.TransferTransaction;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.peer.node.NodeApiId;
import org.nem.peer.PeerNetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

// TODO: add tests
@RestController
public class TransferController {
	private static final Logger LOGGER = Logger.getLogger(TransferController.class.getName());

	private final AccountAnalyzer accountAnalyzer;
	private final Foraging foraging;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	TransferController(
			final AccountAnalyzer accountAnalyzer,
			final Foraging foraging,
			final NisPeerNetworkHost host) {
		this.accountAnalyzer = accountAnalyzer;
		this.foraging = foraging;
		this.host = host;
	}

	@RequestMapping(value = "/transfer/prepare", method = RequestMethod.POST)
	@ClientApi
	public RequestPrepare transferPrepare(@RequestBody final Deserializer deserializer) {
		final TransferTransaction transfer = deserializeTransaction(deserializer);

		if (!transfer.isValid())
			throw new IllegalArgumentException("transfer must be valid");

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
		return new RequestPrepare(transferData);
	}

	@RequestMapping(value = "/transfer/announce", method = RequestMethod.POST)
	@ClientApi
	public void transferAnnounce(@RequestBody final RequestAnnounce requestAnnounce) throws Exception {
		final TransferTransaction transfer = deserializeTransaction(requestAnnounce.getData());
		transfer.setSignature(new Signature(requestAnnounce.getSignature()));

		LOGGER.info("   signer: " + transfer.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("recipient: " + transfer.getRecipient().getAddress().getEncoded());
		LOGGER.info("   verify: " + Boolean.toString(transfer.verify()));

		if (!transfer.isValid() || !transfer.verify())
			throw new IllegalArgumentException("transfer must be valid and verifiable");

        final PeerNetwork network = this.host.getNetwork();

        // add to unconfirmed transactions
        if (foraging.processTransaction(transfer)) {

            // propagate transactions
            // TODO: this should queue request and return immediately, so that client who
            // actually has sent /transfer/announce won't wait for this...
            network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, transfer);
        }
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
