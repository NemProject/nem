package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
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
	public void canMapMosaicToDbMosaic() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic(context.feeMosaicId);

		// Act:
		final DbMosaicDefinition dbMosaicDefinition = context.mapping.map(mosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creator, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.feeRecipient, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.feeMosaicId, DbMosaicId.class);
		Mockito.verify(context.mapper, Mockito.times(5)).map(Mockito.any(), Mockito.eq(DbMosaicProperty.class));

		Assert.assertThat(dbMosaicDefinition.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbMosaicDefinition.getName(), IsEqual.equalTo("Alice's gift vouchers"));
		Assert.assertThat(dbMosaicDefinition.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbMosaicDefinition.getNamespaceId(), IsEqual.equalTo("alice.vouchers"));
		Assert.assertThat(dbMosaicDefinition.getProperties().size(), IsEqual.equalTo(5));
		Assert.assertThat(dbMosaicDefinition.getProperties(), IsEquivalent.equivalentTo(context.propertiesMap.keySet()));
		Assert.assertThat(dbMosaicDefinition.getFeeType(), IsEqual.equalTo(1));
		Assert.assertThat(dbMosaicDefinition.getFeeRecipient(), IsEqual.equalTo(context.dbFeeRecipient));
		Assert.assertThat(dbMosaicDefinition.getFeeDbMosaicId(), IsEqual.equalTo(12L));
		Assert.assertThat(dbMosaicDefinition.getFeeQuantity(), IsEqual.equalTo(123L));
	}

	@Test
	public void feeMosaicIdIsMappedToMinusOneIfMosaicIdEqualsFeeMosaicId() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinition mosaicDefinition = context.createMosaic(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"));

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
		private final DbAccount dbFeeRecipient = Mockito.mock(DbAccount.class);
		private final Account feeRecipient = Utils.generateRandomAccount();
		private final MosaicId feeMosaicId = Utils.createMosaicId(1);
		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("divisibility", "5"));
				this.put(new DbMosaicProperty(), new NemProperty("initialSupply", "123"));
				this.put(new DbMosaicProperty(), new NemProperty("supplyMutable", "true"));
				this.put(new DbMosaicProperty(), new NemProperty("transferable", "true"));
				this.put(new DbMosaicProperty(), new NemProperty("transferFeeEnabled", "false"));
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
					new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers"),
					new MosaicDescriptor("precious vouchers"),
					new DefaultMosaicProperties(this.propertiesMap.values()),
					createFeeInfo(feeMosaicId));
		}

		private MosaicTransferFeeInfo createFeeInfo(final MosaicId feeMosaicId) {
			return new MosaicTransferFeeInfo(
					MosaicTransferFeeType.Absolute,
					this.feeRecipient,
					feeMosaicId,
					Quantity.fromValue(123));
		}
	}
}
