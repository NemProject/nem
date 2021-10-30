package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.DbMultisigMinCosignatoriesModification;
import org.nem.nis.mappers.IMapping;

/**
 * A mapping that is able to map raw multisig min cosignatories modification data to a db multisig min cosignatories modification.
 */
public class MultisigMinCosignatoriesModificationRawToDbModelMapping implements IMapping<Object[], DbMultisigMinCosignatoriesModification> {

	@Override
	public DbMultisigMinCosignatoriesModification map(final Object[] source) {
		if (null == source[16]) {
			return null;
		}

		final DbMultisigMinCosignatoriesModification dbMinCosignatoriesModification = new DbMultisigMinCosignatoriesModification();
		dbMinCosignatoriesModification.setId(RawMapperUtils.castToLong(source[16]));
		dbMinCosignatoriesModification.setRelativeChange((Integer) source[17]);
		return dbMinCosignatoriesModification;
	}
}
