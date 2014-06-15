package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.utils.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.PublicApi;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * Controller that exposes debug endpoints.
 */
@RestController
public class DebugController {
	private static final Logger LOGGER = Logger.getLogger(DebugController.class.getName());

	private final NisPeerNetworkHost host;
	private final BlockChain blockChain;
	private final BlockDao blockDao;

	/**
	 * Creates a new debug controller.
	 *
	 * @param host The host.
	 * @param blockChain The block chain.
	 * @param blockDao The block dao.
	 */
	@Autowired(required = true)
	public DebugController(
			final NisPeerNetworkHost host,
			final BlockChain blockChain,
			final BlockDao blockDao) {
		this.host = host;
		this.blockChain = blockChain;
		this.blockDao = blockDao;
	}

	/**
	 * Debug entry point that can force the node to shut down.
	 *
	 * @param signature The signature.
	 * @return The result of the operation.
	 */
	@RequestMapping(value = "/debug/fix-node", method = RequestMethod.GET)
	public String nodeFixer(@RequestParam(value = "data") final String signature) {
		final byte[] data = ArrayUtils.concat(
				StringEncoder.getBytes(host.getNetwork().getLocalNode().getEndpoint().getBaseUrl().toString()),
				ByteUtils.intToBytes(NisMain.TIME_PROVIDER.getCurrentTime().getRawTime() / 60));

		final Signer signer = new Signer(new KeyPair(NemesisBlock.ADDRESS.getPublicKey()));
		final byte[] signed = Base32Encoder.getBytes(signature);
		LOGGER.info(String.format("%d %s",
				NisMain.TIME_PROVIDER.getCurrentTime().getRawTime() / 60,
				host.getNetwork().getLocalNode().getEndpoint().getBaseUrl().toString()));

		if (signer.verify(data, new Signature(signed))) {
			LOGGER.info("forced shut down");
			System.exit(-1);
		}

		return "ok";
	}

	/**
	 * Gets debug information about the block with the specified height.
	 *
	 * @param height The height.
	 * @return The matching block debug information
	 */
	@RequestMapping(value = "/debug/block-info/get", method = RequestMethod.GET)
	@PublicApi
	public BlockDebugInfo blockDebugInfo(@RequestParam(value = "height") final String height) {
		final BlockHeight blockHeight = new BlockHeight(Long.parseLong(height));
		final AccountAnalyzer accountAnalyzer = this.blockChain.copyAccountAnalyzer();

		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		final Block block = BlockMapper.toModel(dbBlock, accountAnalyzer);

		final org.nem.nis.dbmodel.Block dbParent = 1 == blockHeight.getRaw() ? null : this.blockDao.findByHeight(blockHeight.prev());
		final Block parent = null == dbParent ? null : BlockMapper.toModel(dbParent, accountAnalyzer);

		final BlockScorer scorer = new BlockScorer(accountAnalyzer);
		scorer.forceImportanceCalculation(); // TODO: why do we need to force the calculation here?

		final BigInteger hit = scorer.calculateHit(block);
		final BigInteger target = null == parent ? BigInteger.ZERO : scorer.calculateTarget(parent, block);
		final int interBlockTime = null == parent ? 0 : block.getTimeStamp().subtract(parent.getTimeStamp());
		final BlockDebugInfo blockDebugInfo =  new BlockDebugInfo(
				block.getHeight(),
				block.getTimeStamp(),
				block.getSigner().getAddress(),
				block.getDifficulty(),
				hit,
				target,
				interBlockTime);

		for (final Transaction transaction : block.getTransactions())
			blockDebugInfo.addTransactionDebugInfo(mapToDebugInfo(transaction));

		return blockDebugInfo;
	}

	private static TransactionDebugInfo mapToDebugInfo(final Transaction transaction) {
		Address recipient = Address.fromEncoded("N/A");
		Amount amount = Amount.ZERO;
		String messageText = "";

		if (transaction instanceof TransferTransaction) {
			final TransferTransaction transfer = ((TransferTransaction)transaction);
			recipient = transfer.getRecipient().getAddress();
			amount = transfer.getAmount();

			final Message message = transfer.getMessage();
			if (null != message && message.canDecode())
				messageText = StringEncoder.getString(message.getDecodedPayload());
		}

		return new TransactionDebugInfo(
				transaction.getTimeStamp(),
				transaction.getDeadline(),
				transaction.getSigner().getAddress(),
				recipient,
				amount,
				transaction.getFee(),
				messageText);
	}
}
