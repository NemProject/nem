package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbMosaicProperty;

import java.math.BigInteger;

public class MosaicPropertyRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final Object[] raw = new Object[8];
		raw[5] = BigInteger.valueOf(123L);           // id
		raw[6] = "foo";                              // name
		raw[7] = "bar";                              // value

		// Act:
		final DbMosaicProperty dbModel = new MosaicPropertyRawToDbModelMapping().map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(dbModel.getValue(), IsEqual.equalTo("bar"));
	}
}
