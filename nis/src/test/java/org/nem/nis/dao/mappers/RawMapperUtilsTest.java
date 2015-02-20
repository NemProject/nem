package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.IMapper;
import org.nem.nis.test.NisUtils;

import java.math.BigInteger;

public class RawMapperUtilsTest {

	//region RawMapperUtils

	@Test
	public void mapAccountMapsNullIdToNullAccount() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, (Long)null);
		final DbAccount account2 = RawMapperUtils.mapAccount(mapper, (BigInteger)null);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
		Assert.assertThat(account2, IsNull.nullValue());
		Mockito.verify(mapper, Mockito.never()).map(Mockito.any(), Mockito.any());
	}

	@Test
	public void mapAccountMapsNonNullLongIdToNonNullAccount() {
		// Arrange:
		final DbAccount originalAccount = NisUtils.createDbAccount(1L);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(8L, DbAccount.class)).thenReturn(originalAccount);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, 8L);

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(originalAccount));
		Mockito.verify(mapper, Mockito.only()).map(Mockito.any(), Mockito.any());
	}

	@Test
	public void mapAccountMapsNonNullBigIntegerIdToNonNullAccount() {
		// Arrange:
		final DbAccount originalAccount = new DbAccount();
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(8L, DbAccount.class)).thenReturn(originalAccount);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, BigInteger.valueOf(8L));

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(originalAccount));
		Mockito.verify(mapper, Mockito.only()).map(Mockito.any(), Mockito.any());
	}

	//endregion

	//region mapBlock

	@Test
	public void mapBlockMapsAnyBigIntegerToNonNullBlock() {
		// Act:
		final DbBlock block = RawMapperUtils.mapBlock(BigInteger.valueOf(8L));
		final DbBlock block2 = RawMapperUtils.mapBlock(null);

		// Assert:
		Assert.assertThat(block, IsNull.notNullValue());
		Assert.assertThat(block2, IsNull.notNullValue());
	}

	//endregion

	//region castBigIntegerToLong

	@Test
	public void castBigIntegerToLongMapsNullLongToNullBigInteger() {
		// Act:
		final Long value = RawMapperUtils.castBigIntegerToLong(null);

		// Assert:
		Assert.assertThat(value, IsNull.nullValue());
	}

	@Test
	public void castBigIntegerToLongMapsNonNullLongToNonNullBigInteger() {
		// Act:
		final Long value = RawMapperUtils.castBigIntegerToLong(BigInteger.valueOf(5L));

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(5L));
	}

	//endregion
}