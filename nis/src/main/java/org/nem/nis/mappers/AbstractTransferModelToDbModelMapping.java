package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.nis.dbmodel.*;

/**
 * Base class for mappings of transfer model types to transfer db model types.
 */
public abstract class AbstractTransferModelToDbModelMapping<TModel extends Transaction, TDbModel extends AbstractTransfer>
		implements IMapping<TModel, TDbModel> {
	private final IMapper mapper;

	/**
	 * Creates a mapper.
	 *
	 * @param mapper The mapper.
	 */
	protected AbstractTransferModelToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public final TDbModel map(final TModel source) {
		final TDbModel dbModel = this.mapImpl(source);
		final org.nem.nis.dbmodel.Account sender = this.mapAccount(source.getSigner());
		AbstractTransferMapper.toDbModel(source, sender, -1, -1, dbModel);
		return dbModel;
	}

	/**
	 * Function overridden by derived classes to preform custom derived-mapping logic.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	protected abstract TDbModel mapImpl(final TModel source);

	/**
	 * Maps a model account to a db model account.
	 *
	 * @param account The model account.
	 * @return The db model account.
	 */
	protected org.nem.nis.dbmodel.Account mapAccount(final Account account) {
		return this.mapper.map(account, org.nem.nis.dbmodel.Account.class);
	}
}
