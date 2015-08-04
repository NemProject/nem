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

public class MosaicDefinitionModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic();

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = context.mapping.map(mosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creator, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(4)).map(Mockito.any(), Mockito.eq(DbMosaicProperty.class));

		Assert.assertThat(dbMosaicDefinition.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaicDefinition.getName(), IsEqual.equalTo("Alice's gift vouchers"));
		Assert.assertThat(dbMosaicDefinition.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbMosaicDefinition.getNamespaceId(), IsEqual.equalTo("alice.vouchers"));
		Assert.assertThat(dbMosaicDefinition.getProperties().size(), IsEqual.equalTo(4));
		Assert.assertThat(dbMosaicDefinition.getProperties(), IsEquivalent.equivalentTo(context.propertiesMap.keySet()));
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
			}
		};

		private final MosaicDefinitionModelToDbModelMapping mapping = new MosaicDefinitionModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.creator, DbAccount.class)).thenReturn(this.dbCreator);

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getValue(), DbMosaicProperty.class)).thenReturn(entry.getKey());
			}
		}

		private MosaicDefinition createMosaic() {
			return new MosaicDefinition(
					this.creator,
					new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"),
					new MosaicDescriptor("precious vouchers"),
					new DefaultMosaicProperties(this.propertiesMap.values()));
		}
	}
}
