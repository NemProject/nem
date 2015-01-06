package org.nem.nis.mappers;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.dbmodel.AbstractTransfer;

/**
 * Base class for mappings of transfer model types to transfer db model types.
 *
 * @param <TModel> The model type.
 * @param <TDbModel> The db model type.
 */
public abstract class AbstractTransferModelToDbModelMapping<TModel extends Transaction, TDbModel extends AbstractTransfer>
		implements IMapping<TModel, TDbModel> {
	protected final IMapper mapper;

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

		final Hash txHash = HashUtils.calculateHash(source);
		dbModel.setTransferHash(txHash);
		dbModel.setVersion(source.getVersion());
		dbModel.setFee(source.getFee().getNumMicroNem());
		dbModel.setTimeStamp(source.getTimeStamp().getRawTime());
		dbModel.setDeadline(source.getDeadline().getRawTime());
		dbModel.setSender(sender);
		dbModel.setSenderProof(null == source.getSignature() ? null : source.getSignature().getBytes());
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
