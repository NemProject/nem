package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.time.*;
import org.nem.nis.controller.viewmodels.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.dbmodel.*;

/**
 * Class that contains functions for converting db-models to block explorer view models.
 */
public class BlockExplorerMapper {

	/**
	 * Maps a database block to an explorer block view model.
	 *
	 * @param block The database block.
	 * @return The explorer block view model.
	 */
	public ExplorerBlockViewModel toExplorerViewModel(final DbBlock block) {
		final ExplorerBlockViewModel viewModel = new ExplorerBlockViewModel(
				new BlockHeight(block.getHeight()),
				Address.fromPublicKey(block.getHarvester().getPublicKey()),
				UnixTime.fromTimeInstant(new TimeInstant(block.getTimeStamp())),
				block.getBlockHash());

		block.getBlockTransferTransactions().stream()
				.map(transfer -> this.toExplorerViewModel(transfer))
				.forEach(transfer -> viewModel.addTransaction(transfer));
		return viewModel;
	}

	/**
	 * Maps a database transfer to an explorer transfer view model.
	 *
	 * @param transfer The database transfer.
	 * @return The explorer transfer view model.
	 */
	public ExplorerTransferViewModel toExplorerViewModel(final DbTransferTransaction transfer) {
		return new ExplorerTransferViewModel(
				TransactionTypes.TRANSFER,
				Amount.fromMicroNem(transfer.getFee()),
				UnixTime.fromTimeInstant(new TimeInstant(transfer.getTimeStamp())),
				Address.fromPublicKey(transfer.getSender().getPublicKey()),
				new Signature(transfer.getSenderProof()),
				transfer.getTransferHash(),
				Address.fromEncoded(transfer.getRecipient().getPrintableKey()),
				Amount.fromMicroNem(transfer.getAmount()),
				transfer.getMessageType(),
				transfer.getMessagePayload());
	}
}
