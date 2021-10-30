package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;

import java.math.BigInteger;
import java.util.function.*;

public class RawMapperUtilsTest {

	// region RawMapperUtils

	@Test
	public void mapAccountMapsNullLongToNullAccount() {
		// Assert:
		assertNullAccountIdIsMappedToNullAccount(mapper -> RawMapperUtils.mapAccount(mapper, (Long) null));
	}

	@Test
	public void mapAccountMapsNullObjectToNullAccount() {
		// Assert:
		assertNullAccountIdIsMappedToNullAccount(mapper -> RawMapperUtils.mapAccount(mapper, (Object) null));
	}

	private static void assertNullAccountIdIsMappedToNullAccount(final Function<IMapper, DbAccount> mapAccount) {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);

		// Act:
		final DbAccount account = mapAccount.apply(mapper);

		// Assert:
		MatcherAssert.assertThat(account, IsNull.nullValue());
		Mockito.verify(mapper, Mockito.never()).map(Mockito.any(), Mockito.any());
	}

	@Test
	public void mapAccountMapsNonNullLongIdToNonNullAccount() {
		// Assert:
		assertNonNullAccountIdIsMappedToNonNullAccount((mapper, id) -> RawMapperUtils.mapAccount(mapper, 8L));
	}

	@Test
	public void mapAccountMapsNonNullBigIntegerIdToNonNullAccount() {
		// Assert:
		assertNonNullAccountIdIsMappedToNonNullAccount((mapper, id) -> RawMapperUtils.mapAccount(mapper, BigInteger.valueOf(8L)));
	}

	private static void assertNonNullAccountIdIsMappedToNonNullAccount(final BiFunction<IMapper, Long, DbAccount> mapAccount) {
		// Arrange:
		final DbAccount originalAccount = new DbAccount(8L);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(8L, DbAccount.class)).thenReturn(originalAccount);

		// Act:
		final DbAccount account = mapAccount.apply(mapper, 8L);

		// Assert:
		MatcherAssert.assertThat(account, IsEqual.equalTo(originalAccount));
		Mockito.verify(mapper, Mockito.only()).map(Mockito.any(), Mockito.any());
	}

	// endregion

	// region mapBlock

	@Test
	public void mapBlockMapsNullObjectToBlockWithNullId() {
		// Act:
		final DbBlock block = RawMapperUtils.mapBlock(null);

		// Assert:
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		MatcherAssert.assertThat(block.getId(), IsNull.nullValue());
	}

	@Test
	public void mapBlockMapsNonNullObjectToBlockWithNonNullId() {
		// Act:
		final DbBlock block = RawMapperUtils.mapBlock(BigInteger.valueOf(8L));

		// Assert:
		MatcherAssert.assertThat(block, IsNull.notNullValue());
		MatcherAssert.assertThat(block.getId(), IsEqual.equalTo(8L));
	}

	// endregion

	// region castToLong

	@Test
	public void castToLongMapsNullObjectToNullLong() {
		// Act:
		final Long value = RawMapperUtils.castToLong(null);

		// Assert:
		MatcherAssert.assertThat(value, IsNull.nullValue());
	}

	@Test
	public void castToLongMapsNonNullObjectToNonNullLong() {
		// Act:
		final Long value = RawMapperUtils.castToLong(BigInteger.valueOf(5L));

		// Assert:
		MatcherAssert.assertThat(value, IsEqual.equalTo(5L));
	}

	// endregion
}
