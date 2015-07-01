package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.primitive.GenericAmount;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;

import java.util.Collections;

public class MosaicModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final Mosaic mosaic = new Mosaic(
				context.creator,
				context.mosaicProperties,
				GenericAmount.fromValue(123));

		// Act:
		final DbMosaic dbMosaic = context.mapping.map(mosaic);

		// Assert:
		Assert.assertThat(dbMosaic.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaic.getProperties().size(), IsEqual.equalTo(1));
		Assert.assertThat(dbMosaic.getProperties().contains(context.dbProperty), IsEqual.equalTo(true));
		Assert.assertThat(dbMosaic.getAmount(), IsEqual.equalTo(123L));
		Assert.assertThat(dbMosaic.getPosition(), IsNull.nullValue());
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();
		private final DbMosaicProperty dbProperty = Mockito.mock(DbMosaicProperty.class);
		private final NemProperty property = new NemProperty("foo", "bar");
		private final MosaicProperties mosaicProperties = Mockito.mock(MosaicProperties.class);
		private final MosaicModelToDbModelMapping mapping = new MosaicModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.creator, DbAccount.class)).thenReturn(this.dbCreator);
			Mockito.when(this.mapper.map(this.property, DbMosaicProperty.class)).thenReturn(this.dbProperty);
			Mockito.when(mosaicProperties.asCollection()).thenReturn(Collections.singletonList(this.property));
		}
	}
}
