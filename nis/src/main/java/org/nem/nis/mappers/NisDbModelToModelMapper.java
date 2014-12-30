package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.AbstractTransfer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A NIS mapper facade for mapping db model types to model types.
 */
public class NisDbModelToModelMapper {
	private final IMapper mapper;

	/**
	 * Creates a mapper facade.
	 *
	 * @param mapper The mapper.
	 */
	public NisDbModelToModelMapper(final IMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Maps a db model block to a model block.
	 *
	 * @param block The db model block.
	 * @return The model block.
	 */
	public Block map(final org.nem.nis.dbmodel.Block block) {
		return this.mapper.map(block, Block.class);
	}

	/**
	 * Maps all the transfer transactions in a block to model transactions.
	 * TODO 20141230 J-G: can we remove this (look at where it is being called)?
	 *
	 * @param block The db model block.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransferTransactions(final org.nem.nis.dbmodel.Block block) {
		final Collection<Transaction> transactions = new ArrayList<>();
		transactions.addAll(this.mapTransactions(block.getBlockTransfers(), TransferTransaction.class));
		return transactions;
	}

	/**
	 * Maps all the transactions in a block to model transactions.
	 *
	 * @param block The db model block.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransactions(final org.nem.nis.dbmodel.Block block) {
		final Collection<Transaction> transactions = new ArrayList<>();
		transactions.addAll(this.mapTransactions(block.getBlockTransfers(), TransferTransaction.class));
		transactions.addAll(this.mapTransactions(block.getBlockImportanceTransfers(), ImportanceTransferTransaction.class));
		return transactions;
	}

	// TODO 20141230 J-J: should remove need for transaction type
	private <TDbModel extends AbstractTransfer, TModel extends Transaction> Collection<Transaction> mapTransactions(
			final Collection<TDbModel> dbTransactions,
			final Class<TModel> transactionType) {
		return dbTransactions.stream()
				.map(t -> this.mapper.map(t, transactionType))
				.collect(Collectors.toList());
	}
}