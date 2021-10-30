package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.core.model.Transaction;
import org.nem.core.model.primitive.Amount;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.AbstractTransfer;

/**
 * Base class for mappings of transfer db model types to transfer model types.
 *
 * @param <TDbModel> The db model type.
 * @param <TModel> The model type.
 */
public abstract class AbstractTransferDbModelToModelMapping<TDbModel extends AbstractTransfer, TModel extends Transaction>
		implements
			IMapping<TDbModel, TModel> {

	@Override
	public final TModel map(final TDbModel source) {
		final TModel model = this.mapImpl(source);
		model.setFee(new Amount(source.getFee()));
		model.setDeadline(new TimeInstant(source.getDeadline()));
		model.setSignature(null == source.getSenderProof() ? null : new Signature(source.getSenderProof()));
		return model;
	}

	/**
	 * Function overridden by derived classes to preform custom derived-mapping logic.
	 *
	 * @param source The source object.
	 * @return The target object.
	 */
	protected abstract TModel mapImpl(final TDbModel source);
}
