package org.nem.nis.controller;

import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.utils.*;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.controller.annotations.PublicApi;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.mappers.BlockMapper;
import org.nem.nis.poi.*;
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
	private final BlockDao blockDao;
	private final BlockAnalyzer blockAnalyzer;
	private final ImportanceCalculator importanceCalculator;

	/**
	 * Creates a new debug controller.
	 *
	 * @param host The host.
	 * @param blockDao The block dao.
	 * @param blockAnalyzer The block analyzer.
	 * @param importanceCalculator The importance calculator.
	 */
	@Autowired(required = true)
	public DebugController(
			final NisPeerNetworkHost host,
			final BlockDao blockDao,
			final BlockAnalyzer blockAnalyzer,
			final ImportanceCalculator importanceCalculator) {
		this.host = host;
		this.blockDao = blockDao;
		this.blockAnalyzer = blockAnalyzer;
		this.importanceCalculator = importanceCalculator;
	}

	/**
	 * Debug entry point that can force the node to shut down.
	 *
	 * @param signature The signature.
	 * @return The result of the operation.
	 */
	@RequestMapping(value = "/debug/fix-node", method = RequestMethod.GET)
	@PublicApi
	public String nodeFixer(@RequestParam(value = "data") final String signature) {
		final byte[] data = ArrayUtils.concat(
				StringEncoder.getBytes(this.host.getNetwork().getLocalNode().getEndpoint().getBaseUrl().toString()),
				ByteUtils.intToBytes(NisMain.TIME_PROVIDER.getCurrentTime().getRawTime() / 60));

		final Signer signer = new Signer(new KeyPair(NemesisBlock.ADDRESS.getPublicKey()));
		final byte[] signed = Base32Encoder.getBytes(signature);
		LOGGER.info(String.format("%d %s",
				NisMain.TIME_PROVIDER.getCurrentTime().getRawTime() / 60,
				this.host.getNetwork().getLocalNode().getEndpoint().getBaseUrl().toString()));

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
		final AccountAnalyzer accountAnalyzer = new AccountAnalyzer(
				new AccountCache(),
				new PoiFacade(this.importanceCalculator));
		this.blockAnalyzer.analyze(accountAnalyzer, blockHeight.getRaw());
		final AccountLookup accountLookup = accountAnalyzer.getAccountCache();

		// this API can be called for any block in the chain, so we need to force an importance recalculation
		// because we want the returned importances to be relative to the requested height
		final BlockScorer scorer = new BlockScorer(accountAnalyzer.getPoiFacade());

		final org.nem.nis.dbmodel.Block dbBlock = this.blockDao.findByHeight(blockHeight);
		final Block block = BlockMapper.toModel(dbBlock, accountLookup);

		final org.nem.nis.dbmodel.Block dbParent = 1 == blockHeight.getRaw() ? null : this.blockDao.findByHeight(blockHeight.prev());
		final Block parent = null == dbParent ? null : BlockMapper.toModel(dbParent, accountLookup);

		final BigInteger hit = scorer.calculateHit(block);
		final BigInteger target = null == parent ? BigInteger.ZERO : scorer.calculateTarget(parent, block);
		final int interBlockTime = null == parent ? 0 : block.getTimeStamp().subtract(parent.getTimeStamp());
		final BlockDebugInfo blockDebugInfo = new BlockDebugInfo(
				block.getHeight(),
				block.getTimeStamp(),
				block.getSigner().getAddress(),
				block.getDifficulty(),
				hit,
				target,
				interBlockTime);

		for (final Transaction transaction : block.getTransactions()) {
			blockDebugInfo.addTransactionDebugInfo(mapToDebugInfo(transaction));
		}

		return blockDebugInfo;
	}

	/**
	 * Gets debug information about all running timers.
	 *
	 * @return Debug information about all running timers.
	 */
	@RequestMapping(value = "/debug/timers", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<NemAsyncTimerVisitor> timersInfo() {
		return new SerializableList<>(this.host.getVisitors());
	}

	/**
	 * Gets debug information about incoming connections.
	 *
	 * @return Debug information about incoming connections.
	 */
	@RequestMapping(value = "/debug/connections/incoming", method = RequestMethod.GET)
	@PublicApi
	public AuditCollection incomingConnectionsInfo() {
		return this.host.getIncomingAudits();
	}

	/**
	 * Gets debug information about outgoing connections.
	 *
	 * @return Debug information about outgoing connections.
	 */
	@RequestMapping(value = "/debug/connections/outgoing", method = RequestMethod.GET)
	@PublicApi
	public AuditCollection outgoingConnectionsInfo() {
		return this.host.getOutgoingAudits();
	}

	/**
	 * Gets debug information about the time synchronization.
	 *
	 * @return Debug information about the time synchronization.
	 */
	@RequestMapping(value = "/debug/time-synchronization", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<TimeSynchronizationResult> timeSynchronizationIno() {
		return new SerializableList<>(this.host.getNetwork().getTimeSynchronizationResults());
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
			if (null != message && message.canDecode()) {
				messageText = StringEncoder.getString(message.getDecodedPayload());
			}
		} else if (transaction instanceof ImportanceTransferTransaction) {
			final ImportanceTransferTransaction transfer = ((ImportanceTransferTransaction)transaction);
			recipient = transfer.getRemote().getAddress();
			amount = Amount.fromMicroNem(transfer.getType());
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
