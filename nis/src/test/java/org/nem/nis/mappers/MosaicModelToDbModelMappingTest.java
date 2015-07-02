package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final Mosaic mosaic = new Mosaic(
				context.creator,
				new MosaicPropertiesImpl(context.propertiesMap.values()),
				GenericAmount.fromValue(123));

		// Act:
		final DbMosaic dbMosaic = context.mapping.map(mosaic);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creator, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(2)).map(Mockito.any(), Mockito.eq(DbMosaicProperty.class));

		Assert.assertThat(dbMosaic.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaic.getProperties().size(), IsEqual.equalTo(2));
		Assert.assertThat(dbMosaic.getProperties(), IsEquivalent.equivalentTo(context.propertiesMap.keySet()));
		Assert.assertThat(dbMosaic.getAmount(), IsEqual.equalTo(123L));
		Assert.assertThat(dbMosaic.getPosition(), IsNull.nullValue());
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();

		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("name", "foo"));
				this.put(new DbMosaicProperty(), new NemProperty("namespace", "bar"));
			}
		};

		private final MosaicModelToDbModelMapping mapping = new MosaicModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.creator, DbAccount.class)).thenReturn(this.dbCreator);

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getValue(), DbMosaicProperty.class)).thenReturn(entry.getKey());
			}
		}
	}
}
