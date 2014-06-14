package org.nem.nis.controller;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.utils.*;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.PublicApi;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.service.BlockIo;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

/**
 * Controller that exposes debug endpoints.
 */
public class DebugController {
	private static final Logger LOGGER = Logger.getLogger(DebugController.class.getName());

	private final NisPeerNetworkHost host;
	private final BlockScorer scorer;
	private final BlockIo blockIo;

	/**
	 * Creates a new debug controller.
	 *
	 * @param host The host.
	 * @param scorer The scorer.
	 * @param blockIo The block i/o.
	 */
	public DebugController(
			final NisPeerNetworkHost host,
			final BlockScorer scorer,
			final BlockIo blockIo) {
		this.host = host;
		this.scorer = scorer;
		this.blockIo = blockIo;
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

		final Signer signer = new Signer(new KeyPair(GenesisBlock.ADDRESS.getPublicKey()));
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
	 * @return The matching block debug information.
	 */
	@RequestMapping(value = "/debug/block-info/get", method = RequestMethod.GET)
	@PublicApi
	public BlockDebugInfo blockDebugInfo(@RequestParam(value = "height") final String height) {
		final BlockHeight blockHeight = new BlockHeight(Long.parseLong(height));
		final Block block = this.blockIo.getBlockAt(blockHeight);
		return new BlockDebugInfo(
				block.getHeight(),
				block.getSigner().getAddress(),
				block.getTimeStamp(),
				block.getDifficulty(),
				this.scorer.calculateHit(block));
	}
}
