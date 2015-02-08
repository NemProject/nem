package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dbmodel.*;

/**
 * Class that contains functions for converting db-models to block explorer view models.
 *
 * TODO 20150206 J-G: why not make this a mapping that we can register with all other mappings?
 */
public class BlockExplorerMapper {
	private final NisDbModelToModelMapper mapper;

	/**
	 * Creates a new mapper.
	 *
	 * @param mapper The nis db model to model mapper.
	 */
	public BlockExplorerMapper(final NisDbModelToModelMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Maps a database block to an explorer block view model.
	 *
	 * @param dbBlock The database block.
	 * @return The explorer block view model.
	 */
	public ExplorerBlockViewModel toExplorerViewModel(final DbBlock dbBlock) {
		final Block block = this.mapper.map(dbBlock);
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(
				block,
				dbBlock.getBlockHash());

		block.getTransactions().stream()
				.map(transfer -> this.toExplorerViewModel(transfer))
				.forEach(transfer -> viewModel.addTransaction(transfer));

		// TODO 20150206 J-G: why are you clearing transactions?
		// CLEAR
		block.getTransactions().clear();
		return viewModel;
	}

	/**
	 * Maps a database transfer to an explorer transfer view model.
	 *
	 * @param transfer The database transfer.
	 * @return The explorer transfer view model.
	 */
	public ExplorerTransferViewModel toExplorerViewModel(final Transaction transfer) {
		return new ExplorerTransferViewModel(transfer, HashUtils.calculateHash(transfer));
	}
}
