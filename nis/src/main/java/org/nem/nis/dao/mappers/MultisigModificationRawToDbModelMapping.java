package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

/**
 * A mapping that is able to map raw multisig modification data to a db multisig modification.
 */
public class MultisigModificationRawToDbModelMapping implements IMapping<Object[], DbMultisigModification> {

	private final IMapper mapper;

	/**
	 * Creates a new mapping.
	 *
	 * @param mapper The mapper.
	 */
	public MultisigModificationRawToDbModelMapping(final IMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public DbMultisigModification map(final Object[] source) {
		final DbAccount cosignatory = this.mapAccount(castBigIntegerToLong((BigInteger)source[14]));

		final DbMultisigModification dbModification = new DbMultisigModification();
		dbModification.setId(castBigIntegerToLong((BigInteger)source[13]));
		dbModification.setCosignatory(cosignatory);
		dbModification.setModificationType((Integer)source[15]);

		return dbModification;
	}

	private DbAccount mapAccount(final Long id) {
		return this.mapper.map(id, DbAccount.class);
	}

	private Long castBigIntegerToLong(final BigInteger value) {
		return null == value ? null : value.longValue();
	}
}
