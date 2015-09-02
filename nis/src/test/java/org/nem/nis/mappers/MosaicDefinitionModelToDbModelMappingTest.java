package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.*;

public class MosaicDefinitionModelToDbModelMappingTest {

	@Test
	public void canMapMosaicToDbMosaicWhenMosaicLevyIsPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic(context.feeMosaicId);

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = context.mapping.map(mosaicDefinition);

		// Assert:
		assertMapping(context, dbMosaicDefinition, 12L);
	}

	@Test
	public void canMapMosaicToDbMosaicWhenMosaicLevyIsNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic(null);

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = context.mapping.map(mosaicDefinition);

		// Assert:
		assertMapping(context, dbMosaicDefinition, null);
	}

	private static void assertMapping(final TestContext context, final DbMosaicDefinition dbMosaicDefinition, final Long feeMosaicId) {
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creator, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(4)).map(Mockito.any(), Mockito.eq(DbMosaicProperty.class));

		Assert.assertThat(dbMosaicDefinition.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaicDefinition.getName(), IsEqual.equalTo("alice's gift vouchers"));
		Assert.assertThat(dbMosaicDefinition.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbMosaicDefinition.getNamespaceId(), IsEqual.equalTo("alice.vouchers"));
		Assert.assertThat(dbMosaicDefinition.getProperties().size(), IsEqual.equalTo(4));
		Assert.assertThat(dbMosaicDefinition.getProperties(), IsEquivalent.equivalentTo(context.propertiesMap.keySet()));

		if (null != feeMosaicId) {
			Mockito.verify(context.mapper, Mockito.times(1)).map(context.feeRecipient, DbAccount.class);
			Mockito.verify(context.mapper, Mockito.times(1)).map(context.feeMosaicId, DbMosaicId.class);

			Assert.assertThat(dbMosaicDefinition.getFeeType(), IsEqual.equalTo(1));
			Assert.assertThat(dbMosaicDefinition.getFeeRecipient(), IsEqual.equalTo(context.dbFeeRecipient));
			Assert.assertThat(dbMosaicDefinition.getFeeDbMosaicId(), IsEqual.equalTo(feeMosaicId));
			Assert.assertThat(dbMosaicDefinition.getFeeQuantity(), IsEqual.equalTo(123L));
		} else {
			Assert.assertThat(dbMosaicDefinition.getFeeType(), IsNull.nullValue());
			Assert.assertThat(dbMosaicDefinition.getFeeRecipient(), IsNull.nullValue());
			Assert.assertThat(dbMosaicDefinition.getFeeDbMosaicId(), IsNull.nullValue());
			Assert.assertThat(dbMosaicDefinition.getFeeQuantity(), IsNull.nullValue());
		}
	}

	@Test
	public void feeMosaicIdIsMappedToMinusOneIfMosaicIdEqualsFeeMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic(context.mosaicId);

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = context.mapping.map(mosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(), Mockito.eq(DbMosaicId.class));
		Assert.assertThat(dbMosaicDefinition.getFeeDbMosaicId(), IsEqual.equalTo(-1L));
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();
		private final MosaicId mosaicId = new MosaicId(new NamespaceId("alice.vouchers"), "alice's gift vouchers");
		private final DbAccount dbFeeRecipient = Mockito.mock(DbAccount.class);
		private final Account feeRecipient = Utils.generateRandomAccount();
		private final MosaicId feeMosaicId = Utils.createMosaicId(1);
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
			Mockito.when(this.mapper.map(this.feeRecipient, DbAccount.class)).thenReturn(this.dbFeeRecipient);
			Mockito.when(this.mapper.map(this.feeMosaicId, DbMosaicId.class)).thenReturn(new DbMosaicId(12L));

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getValue(), DbMosaicProperty.class)).thenReturn(entry.getKey());
			}
		}

		private MosaicDefinition createMosaic(final MosaicId feeMosaicId) {
			return new MosaicDefinition(
					this.creator,
					this.mosaicId,
					new MosaicDescriptor("precious vouchers"),
					new DefaultMosaicProperties(this.propertiesMap.values()),
					null == feeMosaicId ? null : this.createMosaicLevy(feeMosaicId));
		}

		private MosaicLevy createMosaicLevy(final MosaicId feeMosaicId) {
			return new MosaicLevy(
					MosaicTransferFeeType.Absolute,
					this.feeRecipient,
					feeMosaicId,
					Quantity.fromValue(123));
		}
	}
}
