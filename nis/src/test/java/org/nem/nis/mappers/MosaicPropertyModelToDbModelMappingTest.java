package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.DbMosaicProperty;

public class MosaicPropertyModelToDbModelMappingTest {

	@Test
	public void canMapNemPropertyToDbMosaicProperty() {
		// Arrange:
		final MosaicPropertyModelToDbModelMapping mapping = new MosaicPropertyModelToDbModelMapping();
		final NemProperty property = new NemProperty("foo", "bar");

		// Act:
		final DbMosaicProperty dbMosaicProperty = mapping.map(property);

		// Assert:
		Assert.assertThat(dbMosaicProperty.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(dbMosaicProperty.getValue(), IsEqual.equalTo("bar"));
		Assert.assertThat(dbMosaicProperty.getMosaic(), IsNull.nullValue());
	}
}
