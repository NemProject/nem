package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.DbMosaicProperty;

public class MosaicDefinitionPropertyModelToDbModelMappingTest {

	@Test
	public void canMapNemPropertyToDbMosaicProperty() {
		// Arrange:
		final MosaicPropertyModelToDbModelMapping mapping = new MosaicPropertyModelToDbModelMapping();
		final NemProperty property = new NemProperty("foo", "bar");

		// Act:
		final DbMosaicProperty dbMosaicProperty = mapping.map(property);

		// Assert:
		MatcherAssert.assertThat(dbMosaicProperty.getName(), IsEqual.equalTo("foo"));
		MatcherAssert.assertThat(dbMosaicProperty.getValue(), IsEqual.equalTo("bar"));
		MatcherAssert.assertThat(dbMosaicProperty.getMosaicDefinition(), IsNull.nullValue());
	}
}
