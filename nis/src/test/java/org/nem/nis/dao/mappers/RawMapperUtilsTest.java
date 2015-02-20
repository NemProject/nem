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

	// TODO 20150219 J-B: consider a separate test for each overload
	// TODO 20150220 BR -> J: sure
	@Test
	public void mapAccountMapsNullLongToNullAccount() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, (Long)null);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
		Mockito.verify(mapper, Mockito.never()).map(Mockito.any(), Mockito.any());
	}

	@Test
	public void mapAccountMapsNullObjectToNullAccount() {
		// Arrange:
		final IMapper mapper = Mockito.mock(IMapper.class);

		// Act:
		final DbAccount account = RawMapperUtils.mapAccount(mapper, (Object)null);

		// Assert:
		Assert.assertThat(account, IsNull.nullValue());
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
		final DbAccount originalAccount = new DbAccount(8L);
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

	// TODO 20150219 J-B: consider two tests
	// TODO 20150220 BR -> J: sure
	@Test
	public void mapBlockMapsNullObjectToNonNullBlock() {
		// Act:
		final DbBlock block = RawMapperUtils.mapBlock(null);

		// Assert:
		Assert.assertThat(block, IsNull.notNullValue());
	}

	@Test
	public void mapBlockMapsNonNullObjectToNonNullBlock() {
		// Act:
		final DbBlock block = RawMapperUtils.mapBlock(BigInteger.valueOf(8L));

		// Assert:
		Assert.assertThat(block, IsNull.notNullValue());
	}

	//endregion

	//region castToLong

	@Test
	public void castToLongMapsNullObjectToNullLong() {
		// Act:
		final Long value = RawMapperUtils.castToLong(null);

		// Assert:
		Assert.assertThat(value, IsNull.nullValue());
	}

	@Test
	public void castToLongMapsNonNullObjectToNonNullLong() {
		// Act:
		final Long value = RawMapperUtils.castToLong(BigInteger.valueOf(5L));

		// Assert:
		Assert.assertThat(value, IsEqual.equalTo(5L));
	}

	//endregion
}