package org.nem.nis.mappers;

import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.DbMosaicCreationTransaction;

/**
 * A mapping that is able to map a db provision namespace transaction to a model provision namespace transaction.
 */
public class MosaicCreationDbModelToModelMapping extends AbstractTransferDbModelToModelMapping<DbMosaicCreationTransaction, MosaicCreationTransaction> {
	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MosaicCreationDbModelToModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	protected MosaicCreationTransaction mapImpl(final DbMosaicCreationTransaction source) {
		final Account sender = this.mapper.map(source.getSender(), Account.class);

		// stage 1 specific
		assert 1 == source.getMosaics().size();
		final Mosaic mosaic = this.mapper.map(source.getMosaics().get(0), Mosaic.class);
		return new MosaicCreationTransaction(
				new TimeInstant(source.getTimeStamp()),
				sender,
				mosaic);
	}
}
