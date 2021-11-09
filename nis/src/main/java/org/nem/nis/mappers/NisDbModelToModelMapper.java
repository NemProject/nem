package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.nis.dbmodel.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A NIS mapper facade for mapping db model types to model types.
 */
public class NisDbModelToModelMapper implements IMapper {
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
	@SuppressWarnings("rawtypes")
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

	@Override
	public <TSource, TTarget> TTarget map(final TSource source, final Class<TTarget> targetClass) {
		return this.mapper.map(source, targetClass);
	}

	private <TDbModel extends AbstractTransfer> Collection<Transaction> mapTransactions(final Collection<TDbModel> dbTransactions,
			final Predicate<AbstractTransfer> shouldInclude) {
		return dbTransactions.stream().filter(shouldInclude::test).map(t -> this.mapper.map(t, Transaction.class))
				.collect(Collectors.toList());
	}
}
