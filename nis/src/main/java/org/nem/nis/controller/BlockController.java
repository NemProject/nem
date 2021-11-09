package org.nem.nis.controller;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.Node;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.requests.AuthenticatedBlockHeightRequest;
import org.nem.nis.service.BlockIo;
import org.nem.peer.node.AuthenticatedResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Block objects.
 */
@RestController
public class BlockController {
	private final BlockIo blockIo;
	private final NisPeerNetworkHost host;

	@Autowired(required = true)
	BlockController(final BlockIo blockIo, final NisPeerNetworkHost host) {
		this.blockIo = blockIo;
		this.host = host;
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
		return new AuthenticatedResponse<>(this.blockAt(request.getEntity()), localNode.getIdentity(), request.getChallenge());
	}
}
