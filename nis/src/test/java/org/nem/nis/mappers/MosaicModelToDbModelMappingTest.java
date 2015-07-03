package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final Mosaic mosaic = createMosaic(context.creator, context.getProperties());

		// Act:
		final DbMosaic dbMosaic = context.mapping.map(mosaic);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creator, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbMosaicProperty.class));

		Assert.assertThat(dbMosaic.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaic.getMosaicId(), IsEqual.equalTo("Alice's gift vouchers"));
		Assert.assertThat(dbMosaic.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbMosaic.getNamespaceId(), IsEqual.equalTo("alice.vouchers"));
		Assert.assertThat(dbMosaic.getAmount(), IsEqual.equalTo(123L));
		Assert.assertThat(dbMosaic.getProperties().size(), IsEqual.equalTo(2));
		Assert.assertThat(dbMosaic.getProperties(), IsEquivalent.equivalentTo(context.propertiesMap.keySet()));
		Assert.assertThat(dbMosaic.getPosition(), IsNull.nullValue());
	}

	private static Mosaic createMosaic(final Account creator, final MosaicProperties properties) {
		return new Mosaic(
				creator,
				new MosaicId("Alice's gift vouchers"),
				new MosaicDescriptor("precious vouchers"),
				new NamespaceId("alice.vouchers"),
				GenericAmount.fromValue(123),
				properties);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();
		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("divisibility", "3"));
				this.put(new DbMosaicProperty(), new NemProperty("foo", "bar"));
			}
		};

		private final MosaicModelToDbModelMapping mapping = new MosaicModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.creator, DbAccount.class)).thenReturn(this.dbCreator);

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getValue(), DbMosaicProperty.class)).thenReturn(entry.getKey());
			}
		}

		private MosaicProperties getProperties() {
			return new MosaicPropertiesImpl(this.propertiesMap.values());
		}
	}
}
