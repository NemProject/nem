package org.nem.nis.controller;

import org.nem.core.model.Block;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.SerializableList;
import org.nem.nis.BlockChainConstants;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.BlockExplorerMapper;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
public class BlockExplorerController {
	private final ReadOnlyBlockDao blockDao;
	private final NisDbModelToModelMapper mapper;

	@Autowired(required = true)
	public BlockExplorerController(
			final ReadOnlyBlockDao blockDao,
	        final NisDbModelToModelMapper mapper) {
		this.blockDao = blockDao;
		this.mapper = mapper;
	}

	@RequestMapping(value = "/local/chain/blocks-after", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public SerializableList<ExplorerBlockViewModel> localBlocksAfter(@RequestBody final BlockHeight height) {
		final BlockExplorerMapper mapper = new BlockExplorerMapper(this.mapper);
		final SerializableList<ExplorerBlockViewModel> blockList = new SerializableList<>(10);
		final Collection<DbBlock> dbBlockList = this.blockDao.getBlocksAfter(height, 10);
		dbBlockList.stream()
				.map(dbBlock -> mapper.toExplorerViewModel(dbBlock))
				.forEach(viewModel -> blockList.add(viewModel));
		return blockList;
	}
}
