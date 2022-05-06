package org.nem.nis.controller;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.serialization.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.ExplorerBlockViewModel;
import org.nem.nis.dao.ReadOnlyBlockDao;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Controller used by NEM ecosystem services, including the block explorer.
 */
@RestController
public class BlockExplorerController {
	private static final int BLOCKS_LIMIT = 10;

	private final ReadOnlyBlockDao blockDao;
	private final IMapper mapper;

	@Autowired(required = true)
	public BlockExplorerController(final ReadOnlyBlockDao blockDao, final MapperFactory mapperFactory, final AccountLookup accountLookup) {
		this.blockDao = blockDao;
		this.mapper = mapperFactory.createDbModelToModelMapper(accountLookup);
	}

	@RequestMapping(value = "/local/chain/blocks-after", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public SerializableList<ExplorerBlockViewModel> localBlocksAfter(@RequestBody final BlockHeight height) {
		final SerializableList<ExplorerBlockViewModel> blockList = new SerializableList<>(BLOCKS_LIMIT);
		final Collection<DbBlock> dbBlockList = this.blockDao.getBlocksAfter(height, BLOCKS_LIMIT);
		dbBlockList.stream().map(dbBlock -> this.mapper.map(dbBlock, ExplorerBlockViewModel.class)).forEach(blockList::add);
		return blockList;
	}

	@RequestMapping(value = "/local/block/at", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public ExplorerBlockViewModel localBlockAt(@RequestBody final BlockHeight height) {
		final DbBlock dbBlock = this.blockDao.findByHeight(height);
		return this.mapper.map(dbBlock, ExplorerBlockViewModel.class);
	}
}
