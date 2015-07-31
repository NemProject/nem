package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicDefinitionDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicToMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		dbMosaicDefinition.setCreator(context.dbCreator);
		dbMosaicDefinition.setName("Alice's gift vouchers");
		dbMosaicDefinition.setDescription("precious vouchers");
		dbMosaicDefinition.setNamespaceId("alice.vouchers");
		dbMosaicDefinition.setProperties(context.propertiesMap.keySet());

		// Act:
		final MosaicDefinition mosaicDefinition = context.mapping.map(dbMosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbCreator, Account.class);
		Mockito.verify(context.mapper, Mockito.times(5)).map(Mockito.any(), Mockito.eq(NemProperty.class));

		Assert.assertThat(mosaicDefinition.getCreator(), IsEqual.equalTo(context.creator));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers")));
		Assert.assertThat(mosaicDefinition.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection().size(), IsEqual.equalTo(5));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection(), IsEquivalent.equivalentTo(context.propertiesMap.values()));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();

		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("divisibility", "5"));
				this.put(new DbMosaicProperty(), new NemProperty("initialSupply", "123"));
				this.put(new DbMosaicProperty(), new NemProperty("supplyMutable", "true"));
				this.put(new DbMosaicProperty(), new NemProperty("transferable", "true"));
				this.put(new DbMosaicProperty(), new NemProperty("hasTransferFee", "false"));
			}
		};

		private final MosaicDefinitionDbModelToModelMapping mapping = new MosaicDefinitionDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbCreator, Account.class)).thenReturn(this.creator);

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getKey(), NemProperty.class)).thenReturn(entry.getValue());
			}
		}
	}
}
