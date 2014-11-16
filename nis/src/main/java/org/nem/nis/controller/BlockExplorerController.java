package org.nem.nis.controller;

import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.mappers.BlockExplorerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
public class BlockExplorerController {
	private final ReadOnlyBlockDao blockDao;

	@Autowired(required = true)
	public BlockExplorerController(final ReadOnlyBlockDao blockDao) {
		this.blockDao = blockDao;
	}

	@RequestMapping(value = "/local/chain/blocks-after", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public SerializableList<ExplorerBlockViewModel> localBlocksAfter(@RequestBody final BlockHeight height) {
		final BlockExplorerMapper mapper = new BlockExplorerMapper();
		final SerializableList<ExplorerBlockViewModel> blockList = new SerializableList<>(BlockChainConstants.BLOCKS_LIMIT);
		final Collection<org.nem.nis.dbmodel.Block> dbBlockList = this.blockDao.getBlocksAfter(height, BlockChainConstants.BLOCKS_LIMIT);
		dbBlockList.stream()
				.map(dbBlock -> mapper.toExplorerViewModel(dbBlock))
				.forEach(viewModel -> blockList.add(viewModel));
		return blockList;
	}
}
