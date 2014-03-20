package org.nem.nis.controller;

import org.apache.commons.codec.DecoderException;
import org.nem.core.dao.BlockDao;

import org.nem.core.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class BlockController {

	@Autowired
	private BlockDao blockDao;

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value="/block/last", method = RequestMethod.GET)
	public String blockLast() {
		final Block lastBlock = BlockMapper.toModel(this.blockChain.getLastDbBlock(), this.accountAnalyzer);
        return ControllerUtils.serialize(lastBlock);
	}

	/**
	 * Obtain block from the block chain.
	 *
	 * @param blockHashString hash of a block
	 * @return block along with associated elements.
	 */
	@RequestMapping(value="/block/get", method = RequestMethod.GET)
	public String blockGet(@RequestParam(value = "blockHash") final String blockHashString) throws DecoderException {
		final byte[] blockHash = HexEncoder.getBytes(blockHashString);
        final org.nem.core.dbmodel.Block dbBlock = blockDao.findByHash(blockHash);
		if (null == dbBlock)
			return Utils.jsonError(2, "hash not found in the db");

		final Block block = BlockMapper.toModel(dbBlock, this.accountAnalyzer);
        return ControllerUtils.serialize(block);
	}

}
