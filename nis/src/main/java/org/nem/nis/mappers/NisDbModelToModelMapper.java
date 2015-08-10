package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
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

	// TODO 20150709 J-B: we don't really need to keep modifying this class each time we add a new entity type
	// > if we expose the mapper; not sure if that's a good idea or not
	// TODO 20150810 BR -> J: instead of exposing the mapper we could have a general map method which takes a TSource and s TDestinationClass parameter.
	// > or is that approach worse than the other?
	// TODO 20150810 J-B: i just don't want this class to be a giant mapping class that is coupled with everything ...
	// > for common things it is fine (e.g. transactions and block), but for one off things i don't think it helps (or saves) much

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
	 * Maps a db model mosaic definition to a model mosaic definition.
	 *
	 * @param dbMosaicDefinition The db model mosaic definition.
	 * @return The model mosaic.
	 */
	public MosaicDefinition map(final DbMosaicDefinition dbMosaicDefinition) {
		return this.mapper.map(dbMosaicDefinition, MosaicDefinition.class);
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