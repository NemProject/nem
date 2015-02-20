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

	// TODO 20150219 J-B: also can use these in the blockloader
	// > any reason not to use Object id instead of BigIntegerId (so the casts are inside the helpers instead of outside in every call)
	// TODO 20150220 BR _> J: no good reason i guess.

	/**
	 * Maps an account id to a db model account using the specified mapper.
	 *
	 * @param mapper The mapper.
	 * @param id The account id.
	 * @return The db model account.
	 */
	public static DbAccount mapAccount(final IMapper mapper, final Object id) {
		return mapAccount(mapper, castToLong(id));
	}

	/**
	 * Maps a block id to a db block.
	 *
	 * @param id The block id.
	 * @return The db block.
	 */
	protected static DbBlock mapBlock(final Object id) {
		final DbBlock dbBlock = new DbBlock();
		dbBlock.setId(castToLong(id));
		return dbBlock;
	}

	/**
	 * Casts an object value to a Long value.
	 *
	 * @param value The object value.
	 * @return The Long value.
	 */
	public static Long castToLong(final Object value) {
		return castBigIntegerToLong((BigInteger)value);
	}

	private static Long castBigIntegerToLong(final BigInteger value) {
		return null == value ? null : value.longValue();
	}
}
