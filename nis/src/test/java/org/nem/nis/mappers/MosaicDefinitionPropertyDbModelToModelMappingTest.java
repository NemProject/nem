package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.DbMosaicProperty;

public class MosaicDefinitionPropertyDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicPropertyToNemProperty() {
		// Arrange:
		final MosaicPropertyDbModelToModelMapping mapping = new MosaicPropertyDbModelToModelMapping();
		final DbMosaicProperty dbMosaicProperty = new DbMosaicProperty();
		dbMosaicProperty.setName("foo");
		dbMosaicProperty.setValue("bar");

		// Act:
		final NemProperty property = mapping.map(dbMosaicProperty);

		// Assert:
		MatcherAssert.assertThat(property.getName(), IsEqual.equalTo("foo"));
		MatcherAssert.assertThat(property.getValue(), IsEqual.equalTo("bar"));
	}
}
