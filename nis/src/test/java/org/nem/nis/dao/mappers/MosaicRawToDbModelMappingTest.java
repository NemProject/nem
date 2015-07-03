package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.HashSet;

public class MosaicRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMosaic dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbModel.getAmount(), IsEqual.equalTo(321L));
		Assert.assertThat(dbModel.getPosition(), IsEqual.equalTo(234));
		Assert.assertThat(dbModel.getProperties(), IsEqual.equalTo(new HashSet<>()));
	}

	protected IMapping<Object[], DbMosaic> createMapping(final IMapper mapper) {
		return new MosaicRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Long creatorId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.creatorId, DbAccount.class)).thenReturn(this.dbCreator);
		}

		private Object[] createRaw() {
			final Object[] raw = new Object[8];
			raw[0] = BigInteger.valueOf(12L);             // mosaic creation transaction id
			raw[1] = BigInteger.valueOf(123L);            // id
			raw[2] = BigInteger.valueOf(this.creatorId);  // creator id
			raw[3] = "Alice's vouchers";                  // mosaic id
			raw[4] = "precious vouchers";                 // description
			raw[5] = "alice.voucher";                     // namespace id
			raw[6] = BigInteger.valueOf(321L);            // amount
			raw[7] = 234;                                 // position

			return raw;
		}
	}
}
