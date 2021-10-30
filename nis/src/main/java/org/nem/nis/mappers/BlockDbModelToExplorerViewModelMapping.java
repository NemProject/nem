package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dbmodel.DbBlock;

/**
 * A mapping that is able to map a db block to an explorer block view model.
 */
public class BlockDbModelToExplorerViewModelMapping implements IMapping<DbBlock, ExplorerBlockViewModel> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public BlockDbModelToExplorerViewModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public ExplorerBlockViewModel map(final DbBlock dbBlock) {
		final Block block = this.mapper.map(dbBlock, Block.class);
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(block, dbBlock.getBlockHash());

		block.getTransactions().stream().map(this::toExplorerViewModel).forEach(viewModel::addTransaction);
		return viewModel;
	}

	private ExplorerTransferViewModel toExplorerViewModel(final Transaction transfer) {
		return new ExplorerTransferViewModel(transfer, HashUtils.calculateHash(transfer));
	}
}
