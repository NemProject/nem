package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.Predicate;
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
	 * Maps a db model transfer to a model transfer.
	 *
	 * @param transfer The db model transfer.
	 * @return The model transfer.
	 */
	public Transaction map(final Transfer transfer) {
		return this.mapper.map(transfer, TransferTransaction.class);
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
	 * Maps all the transactions in a block to model transactions.
	 *
	 * @param block The db model block.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransactions(final org.nem.nis.dbmodel.Block block) {
		return this.mapTransactionsIf(block, t -> true);
	}

	/**
	 * Maps all the transactions in a block for which a predicate evaluates to true to model transactions.
	 *
	 * @param block The db model block.
	 * @param shouldInclude The predicate used to determine whether or not a transfer should be included in the result.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransactionsIf(final org.nem.nis.dbmodel.Block block, final Predicate<AbstractTransfer> shouldInclude) {
		final Collection<Transaction> transactions = new ArrayList<>();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			transactions.addAll(this.mapTransactions(entry.getFromBlock.apply(block), shouldInclude));
		}

		return transactions;
	}

	private <TDbModel extends AbstractTransfer> Collection<Transaction> mapTransactions(
			final Collection<TDbModel> dbTransactions,
			final Predicate<AbstractTransfer> shouldInclude) {
		return dbTransactions.stream()
				.filter(shouldInclude::test)
				.map(t -> this.mapper.map(t, Transaction.class))
				.collect(Collectors.toList());
	}
}