package org.nem.nis.controller;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.serialization.*;
import org.nem.core.transactions.TransactionFactory;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.nem.peer.NodeApiId;
import org.nem.peer.PeerNetwork;
import org.nem.peer.PeerNetworkHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
public class TransferController {
	private static final Logger LOGGER = Logger.getLogger(TransferController.class.getName());

	@Autowired
	AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value="/transfer/prepare", method = RequestMethod.POST)
	public String transferPrepare(@RequestBody final String body) {
        final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
        final TransferTransaction transfer = deserializeTransaction(deserializer);

        // TODO: exception
		if (!transfer.isValid())
			return Utils.jsonError(1, "incorrect data");

		final byte[] transferData = BinarySerializer.serializeToBytes(transfer.asNonVerifiable());
        return ControllerUtils.serialize(new RequestPrepare(transferData));
	}

	@RequestMapping(value="/transfer/announce", method = RequestMethod.POST)
	public String transferAnnounce(@RequestBody final String body) throws Exception {
        final Deserializer deserializer = ControllerUtils.getDeserializer(body, this.accountAnalyzer);
		final RequestAnnounce requestAnnounce = new RequestAnnounce(deserializer);

		final TransferTransaction transfer = deserializeTransaction(requestAnnounce.getData());
        transfer.setSignature(new Signature(requestAnnounce.getSignature()));

        // TODO: move logger to controller
		LOGGER.info("   signer: " + HexEncoder.getString(transfer.getSigner().getKeyPair().getPublicKey()));
		LOGGER.info("recipient: " + transfer.getRecipient().getAddress().getEncoded());
		LOGGER.info("   verify: " + Boolean.toString(transfer.verify()));

		if (transfer.isValid() && transfer.verify()) {
            final PeerNetwork network = PeerNetworkHost.getDefaultHost().getNetwork();

			// add to unconfirmed transactions
			if (blockChain.processTransaction(transfer)) {

				// propagate transactions
				network.broadcast(NodeApiId.REST_PUSH_TRANSACTION, transfer);
			}

			return Utils.jsonOk();
		}

        // TODO: exception
		return Utils.jsonError(2, "transaction couldn't be verified " + Boolean.toString(transfer.verify()));
	}

    private TransferTransaction deserializeTransaction(final byte[] bytes) throws Exception {
        try (final BinaryDeserializer dataDeserializer = ControllerUtils.getDeserializer(bytes, this.accountAnalyzer)) {
            return deserializeTransaction(dataDeserializer);
        }
    }

    private static TransferTransaction deserializeTransaction(final Deserializer deserializer) {
        return (TransferTransaction)TransactionFactory.NON_VERIFIABLE.deserialize(deserializer);
    }
}
