package org.nem.nis.dao.mappers;

import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.math.BigInteger;

/**
 * Static class exposing utility functions used by raw mappers.
 */
public final class RawMapperUtils {

	/**
	 * Maps an account id to a db model account using the specified mapper.
	 *
	 * @param mapper The mapper.
	 * @param id The account id.
	 * @return The db model account.
	 */
	public static DbAccount mapAccount(final IMapper mapper, final Long id) {
		return null == id
				? null
				: mapper.map(id, DbAccount.class);
	}

	/**
	 * Maps an account id to a db model account using the specified mapper.
	 *
	 * @param mapper The mapper.
	 * @param id The account id.
	 * @return The db model account.
	 */
	public static DbAccount mapAccount(final IMapper mapper, final BigInteger id) {
		return mapAccount(mapper, castBigIntegerToLong(id));
	}

	/**
	 * Maps a block id to a db block.
	 *
	 * @param id The block id.
	 * @return The db block.
	 */
	protected static DbBlock mapBlock(final BigInteger id) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(castBigIntegerToLong(id));
		return dbBlock;
	}

	/**
	 * Casts a BigInteger value to a Long value.
	 *
	 * @param value The BigInteger value.
	 * @return The Long value.
	 */
	public static Long castBigIntegerToLong(final BigInteger value) {
		return null == value ? null : value.longValue();
	}
}
