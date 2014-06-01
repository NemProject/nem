package org.nem.nis.controller;

import org.nem.core.model.BlockHeight;
import org.nem.core.crypto.Hash;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.service.BlockIo;

import org.nem.core.model.Block;
import org.nem.core.utils.HexEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for interacting with Block objects.
 */
@RestController
public class BlockController {

	private final BlockIo blockIo;

	@Autowired(required = true)
	BlockController(final BlockIo blockIo) {
		this.blockIo = blockIo;
	}

	/**
	 * Obtain block from the block chain.
	 *
	 * @param blockHashString hash of a block
	 *
	 * @return block along with associated elements.
	 */
	@RequestMapping(value = "/block/get", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public Block blockGet(@RequestParam(value = "blockHash") final String blockHashString) {
		final Hash blockHash = new Hash(HexEncoder.getBytes(blockHashString));
		return blockIo.getBlock(blockHash);
	}

	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public Block blockAt(@RequestBody final BlockHeight height) {
		return blockIo.getBlockAt(height);
	}
}
