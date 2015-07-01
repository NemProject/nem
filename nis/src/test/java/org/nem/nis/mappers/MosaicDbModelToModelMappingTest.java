package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicToMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaic dbMosaic = new DbMosaic();
		dbMosaic.setCreator(context.dbCreator);
		dbMosaic.setProperties(context.dbProperties);
		dbMosaic.setAmount(123L);

		// Act:
		final Mosaic mosaic = context.mapping.map(dbMosaic);

		// Assert:
		Assert.assertThat(mosaic.getCreator(), IsEqual.equalTo(context.creator));
		Assert.assertThat(mosaic.getProperties(), IsEquivalent.equivalentTo(Arrays.asList(context.propertyName, context.propertyNamespace)));
		Assert.assertThat(mosaic.getAmount(), IsEqual.equalTo(GenericAmount.fromValue(123)));
		Assert.assertThat(mosaic.getChildren(), IsEqual.equalTo(Collections.emptyList()));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();
		private final DbMosaicProperty dbPropertyName = Mockito.mock(DbMosaicProperty.class);
		private final DbMosaicProperty dbPropertyNamespace = Mockito.mock(DbMosaicProperty.class);
		private final NemProperty propertyName = new NemProperty("name", "foo");
		private final NemProperty propertyNamespace = new NemProperty("namespace", "bar");
		private final HashSet<DbMosaicProperty> dbProperties = new HashSet<>();
		private final MosaicDbModelToModelMapping mapping = new MosaicDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbCreator, Account.class)).thenReturn(this.creator);
			this.dbProperties.add(this.dbPropertyName);
			this.dbProperties.add(this.dbPropertyNamespace);
			Mockito.when(this.mapper.map(this.dbPropertyName, NemProperty.class)).thenReturn(this.propertyName);
			Mockito.when(this.mapper.map(this.dbPropertyNamespace, NemProperty.class)).thenReturn(this.propertyNamespace);
		}
	}
}
