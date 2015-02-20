package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.DbAccount;
import org.nem.nis.mappers.IMapper;

import java.math.BigInteger;

public class RawMapperUtilsTest {

	//region RawMapperUtils

	@Test
	public void mapAccountMapsNullIdToNullAccount() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, null);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
		Mockito.verify(mapper, Mockito.never()).map(Mockito.any(), Mockito.any());
	}

	@Test
	public void mapAccountMapsNonNullIdToNonNullAccount() {
		// Arrange:
		final DbAccount originalAccount = new DbAccount(1);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(8L, DbAccount.class)).thenReturn(originalAccount);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, 8L);

		// Assert:
		Assert.assertThat(account, IsEqual.equalTo(originalAccount));
		Mockito.verify(mapper, Mockito.only()).map(Mockito.any(), Mockito.any());
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