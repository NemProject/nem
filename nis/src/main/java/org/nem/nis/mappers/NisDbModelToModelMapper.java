package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.namespace.Namespace;
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
	 * Maps a TDbModel to a model transaction.
	 *
	 * @param <TDbModel> The DbModel type.
	 * @param transfer The TDbModel derived from abstract block transfer.
	 * @return The model transfer transaction.
	 */
	public <TDbModel extends AbstractBlockTransfer> Transaction map(final TDbModel transfer) {
		return this.mapper.map(transfer, Transaction.class);
	}

	/**
	 * Maps a db model block to a model block.
	 *
	 * @param block The db model block.
	 * @return The model block.
	 */
	public Block map(final DbBlock block) {
		return this.mapper.map(block, Block.class);
	}

	/**
	 * Maps all the transactions in a block to model transactions.
	 *
	 * @param block The db model block.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransactions(final DbBlock block) {
		return this.mapTransactionsIf(block, t -> true);
	}

	/**
	 * Maps all the transactions in a block for which a predicate evaluates to true to model transactions.
	 *
	 * @param block The db model block.
	 * @param shouldInclude The predicate used to determine whether or not a transfer should be included in the result.
	 * @return The model transactions.
	 */
	public Collection<Transaction> mapTransactionsIf(final DbBlock block, final Predicate<AbstractTransfer> shouldInclude) {
		final Collection<Transaction> transactions = new ArrayList<>();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			transactions.addAll(this.mapTransactions(entry.getFromBlock.apply(block), shouldInclude));
		}

		return transactions;
	}

	/**
	 * Maps a db model namespace to a model namespace.
	 *
	 * @param dbNamespace The db model namespace.
	 * @return The model namespace.
	 */
	public Namespace map(final DbNamespace dbNamespace) {
		return this.mapper.map(dbNamespace, Namespace.class);
	}

	/**
	 * Maps a db model mosaic to a model mosaic.
	 *
	 * @param dbMosaic The db model mosaic.
	 * @return The model mosaic.
	 */
	public Mosaic map(final DbMosaic dbMosaic) {
		return this.mapper.map(dbMosaic, Mosaic.class);
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