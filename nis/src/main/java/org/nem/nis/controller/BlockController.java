package org.nem.nis.controller;

import org.nem.nis.NisPeerNetworkHost;
import org.nem.nis.controller.viewmodels.AuthenticatedBlockHeightRequest;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.core.model.BlockHeight;
import org.nem.core.crypto.Hash;
import org.nem.nis.BlockScorer;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.BlockIo;
import org.nem.core.model.Block;
import org.nem.peer.node.AuthenticatedResponse;
import org.nem.peer.node.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Block objects.
 */
@RestController
public class BlockController {

	private final BlockIo blockIo;
	private final BlockScorer scorer;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	BlockController(
			final BlockIo blockIo,
			final BlockScorer scorer,
			final NisPeerNetworkHost host) {
		this.blockIo = blockIo;
		this.scorer = scorer;
		this.host = host;
	}

	/**
	 * Gets a block with the specified hash.
	 *
	 * @param blockHashString The hash as a string.
	 * @return The matching block.
	 */
	@RequestMapping(value = "/block/get", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockGet(@RequestParam(value = "blockHash") final String blockHashString) {
		final Hash blockHash = Hash.fromHexString(blockHashString);
		return this.blockIo.getBlock(blockHash);
	}

	/**
	 * Gets a block with the specified height.
	 *
	 * @param height The height.
	 * @return The matching block.
	 */
	@RequestMapping(value = "/block/at/public", method = RequestMethod.POST)
	@PublicApi
	public Block blockAt(@RequestBody final BlockHeight height) {
		return this.blockIo.getBlockAt(height);
	}

	/**
	 * Gets a block with the specified height.
	 *
	 * @param request The request containing the block height.
	 * @return The matching block.
	 */
	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public AuthenticatedResponse<Block> blockAt(@RequestBody final AuthenticatedBlockHeightRequest request) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(
				this.blockAt(request.getEntity()),
				localNode.getIdentity(),
				request.getChallenge());
	}

	/**
	 * Gets debug information about the block with the specified height.
	 *
	 * @param height The height.
	 * @return The matching block debug information
	 */
	@RequestMapping(value = "/block/debug-info/get", method = RequestMethod.GET)
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
