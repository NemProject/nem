package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbMosaicProperty;

import java.math.BigInteger;

public class MosaicDefinitionPropertyRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final Object[] raw = new Object[4];
		raw[1] = BigInteger.valueOf(123L); // id
		raw[2] = "foo"; // name
		raw[3] = "bar"; // value

		// Act:
		final DbMosaicProperty dbModel = new MosaicPropertyRawToDbModelMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getName(), IsEqual.equalTo("foo"));
		MatcherAssert.assertThat(dbModel.getValue(), IsEqual.equalTo("bar"));
	}
}
