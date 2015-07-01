package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.NemProperty;
import org.nem.nis.dbmodel.*;

public class MosaicPropertyDbModelToModelMappingTest {

	@Test
	public void canMapDbNamespaceToNamespace() {
		// Arrange:
		final MosaicPropertyDbModelToModelMapping mapping = new MosaicPropertyDbModelToModelMapping();
		final DbMosaicProperty dbMosaicProperty = new DbMosaicProperty();
		dbMosaicProperty.setName("foo");
		dbMosaicProperty.setValue("bar");

		// Act:
		final NemProperty property = mapping.map(dbMosaicProperty);

		// Assert:
		Assert.assertThat(property.getName(), IsEqual.equalTo("foo"));
		Assert.assertThat(property.getValue(), IsEqual.equalTo("bar"));
	}
}
