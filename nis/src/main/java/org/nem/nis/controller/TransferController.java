package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.Foraging;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.ClientApi;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.Node;
import org.nem.peer.node.NodeApiId;
import org.nem.peer.trust.score.NodeExperience;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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

	// TODO make it common with push/transfer?
	@RequestMapping(value = "/transfer/announce", method = RequestMethod.POST)
	@ClientApi
	public void transferAnnounce(@RequestBody final RequestAnnounce requestAnnounce, final HttpServletRequest request) throws Exception {
		final TransferTransaction transfer = deserializeTransaction(requestAnnounce.getData());
		transfer.setSignature(new Signature(requestAnnounce.getSignature()));

		LOGGER.info("   signer: " + transfer.getSigner().getKeyPair().getPublicKey());
		LOGGER.info("recipient: " + transfer.getRecipient().getAddress().getEncoded());
		LOGGER.info("   verify: " + Boolean.toString(transfer.verify()));
		final Node remoteNode = host.getNetwork().getNodes().getNode(request.getRemoteAddr());

		if (!transfer.isValid() || !transfer.verify()) {
			// Bad experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.FAILURE);
			}

			throw new IllegalArgumentException("transfer must be valid and verifiable");
		}

		// add to unconfirmed transactions

		final PeerNetwork network = this.host.getNetwork();
		// add to unconfirmed transactions
		if (this.foraging.processTransaction(transfer)) {
			// Good experience with the remote node.
			if (remoteNode != null) {
				host.getNetwork().updateExperience(remoteNode, NodeExperience.Code.SUCCESS);
			}

			// propagate transactions
			// this returns immediately, so that client who
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
