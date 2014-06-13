package org.nem.nis.controller;

import org.nem.core.crypto.Hash;
import org.nem.nis.BlockScorer;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.BlockIo;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Block objects.
 */
@RestController
public class BlockController {

	private final BlockIo blockIo;

	@Autowired(required = true)
	BlockController(final BlockIo blockIo, final BlockScorer scorer) {
		this.blockIo = blockIo;
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
	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public Block blockAt(@RequestBody final BlockHeight height) {
		return this.blockIo.getBlockAt(height);
	}
}
