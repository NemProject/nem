package org.nem.nis.controller;

import net.minidev.json.JSONObject;
import org.nem.core.mappers.BlockMapper;
import org.nem.core.model.Block;
import org.nem.core.utils.HexEncoder;
import org.nem.nis.AccountAnalyzer;
import org.nem.nis.BlockChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChainController {

	@Autowired
	private AccountAnalyzer accountAnalyzer;

	@Autowired
	private BlockChain blockChain;

	@RequestMapping(value="/chain/last-block", method = RequestMethod.GET)
	public String blockLast() {
		final Block lastBlock = BlockMapper.toModel(this.blockChain.getLastDbBlock(), this.accountAnalyzer);
		return ControllerUtils.serialize(lastBlock);
	}


}
