package org.nem.nis.controller;

import org.nem.core.model.Hash;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.utils.RequiredBlockDaoAdapter;

import org.nem.nis.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.serialization.Deserializer;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.MissingResourceException;

/**
 * REST API for interacting with Block objects.
 */
@RestController
public class BlockController {

	private final RequiredBlockDaoAdapter blockDao;
	private final AccountAnalyzer accountAnalyzer;

	@Autowired(required = true)
	BlockController(final RequiredBlockDaoAdapter blockDao, final AccountAnalyzer accountAnalyzer) {
		this.blockDao = blockDao;
		this.accountAnalyzer = accountAnalyzer;
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
		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHash(blockHash);
		return BlockMapper.toModel(dbBlock, this.accountAnalyzer);
	}

	@RequestMapping(value = "/block/at", method = RequestMethod.POST)
	@P2PApi
	public Block blockAt(@RequestBody final Deserializer deserializer) {
		final Long blockHeight = deserializer.readLong("height");
		final org.nem.nis.dbmodel.Block dbBlock = blockDao.findByHeight(blockHeight);
		return BlockMapper.toModel(dbBlock, this.accountAnalyzer);
	}
}
