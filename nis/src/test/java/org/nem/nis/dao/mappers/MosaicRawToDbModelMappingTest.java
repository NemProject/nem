package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbMosaic;
import org.nem.nis.mappers.IMapping;

import java.math.BigInteger;

public class MosaicRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMosaic dbModel = this.createMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getDbMosaicId(), IsEqual.equalTo(context.mosaicId));
		MatcherAssert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(234L));
	}

	private IMapping<Object[], DbMosaic> createMapping() {
		return new MosaicRawToDbModelMapping();
	}

	private static class TestContext {
		private final Long mosaicId = 678L;

		private Object[] createRaw() {
			final Object[] raw = new Object[3];
			raw[0] = BigInteger.valueOf(123L); // id
			raw[1] = BigInteger.valueOf(this.mosaicId); // mosaic id
			raw[2] = BigInteger.valueOf(234L); // quantity
			return raw;
		}
	}
}
