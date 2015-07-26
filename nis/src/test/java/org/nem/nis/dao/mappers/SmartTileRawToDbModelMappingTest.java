package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbSmartTile;
import org.nem.nis.mappers.IMapping;

import java.math.BigInteger;

public class SmartTileRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbSmartTile dbModel = this.createMapping().map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getDbMosaicId(), IsEqual.equalTo(context.mosaicId));
		Assert.assertThat(dbModel.getQuantity(), IsEqual.equalTo(234L));
	}

	private IMapping<Object[], DbSmartTile> createMapping() {
		return new SmartTileRawToDbModelMapping();
	}

	private static class TestContext {
		private final Long mosaicId = 678L;

		private Object[] createRaw() {
			final Object[] raw = new Object[3];
			raw[0] = BigInteger.valueOf(123L);            // id
			raw[1] = BigInteger.valueOf(this.mosaicId);   // mosaic id
			raw[2] = BigInteger.valueOf(234L);            // quantity
			return raw;
		}
	}
}
