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

public class MosaicDefinitionDbModelToModelMappingTest {

	@Test
	public void canMapDbMosaicToMosaicWhenMosaicLevyIsPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(context, 12L);

		// Act:
		final MosaicDefinition mosaicDefinition = context.mapping.map(dbMosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbCreator, Account.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbFeeRecipient, Account.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicId, MosaicId.class);
		Mockito.verify(context.mapper, Mockito.times(5)).map(Mockito.any(), Mockito.eq(NemProperty.class));

		Assert.assertThat(mosaicDefinition.getCreator(), IsEqual.equalTo(context.creator));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers")));
		Assert.assertThat(mosaicDefinition.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection().size(), IsEqual.equalTo(5));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection(), IsEquivalent.equivalentTo(context.propertiesMap.values()));
		Assert.assertThat(mosaicDefinition.getMosaicLevy().getType(), IsEqual.equalTo(MosaicTransferFeeType.Absolute));
		Assert.assertThat(mosaicDefinition.getMosaicLevy().getRecipient(), IsEqual.equalTo(context.feeRecipient));
		Assert.assertThat(mosaicDefinition.getMosaicLevy().getMosaicId(), IsEqual.equalTo(Utils.createMosaicId(12)));
		Assert.assertThat(mosaicDefinition.getMosaicLevy().getFee(), IsEqual.equalTo(Quantity.fromValue(123)));
	}

	@Test
	public void canMapDbMosaicToMosaicWhenMosaicLevyIsNotPresent() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(context, null);

		// Act:
		final MosaicDefinition mosaicDefinition = context.mapping.map(dbMosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbCreator, Account.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbFeeRecipient, Account.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.dbMosaicId, MosaicId.class);
		Mockito.verify(context.mapper, Mockito.times(5)).map(Mockito.any(), Mockito.eq(NemProperty.class));

		Assert.assertThat(mosaicDefinition.getCreator(), IsEqual.equalTo(context.creator));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(new MosaicId(new NamespaceId("alice.vouchers"), "Alice's gift vouchers")));
		Assert.assertThat(mosaicDefinition.getDescriptor(), IsEqual.equalTo(new MosaicDescriptor("precious vouchers")));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection().size(), IsEqual.equalTo(5));
		Assert.assertThat(mosaicDefinition.getProperties().asCollection(), IsEquivalent.equivalentTo(context.propertiesMap.values()));
		Assert.assertThat(mosaicDefinition.getMosaicLevy(), IsNull.nullValue());
	}

	@Test
	public void feeDbMosaicIdIsMappedToSameMosaicIdAsMosaicIdInMosaicDefinitionIfFeeDbMosaicIdIsMinusOne() {
		// Arrange:
		final TestContext context = new TestContext();
		final DbMosaicDefinition dbMosaicDefinition = createDbMosaicDefinition(context, -1L);

		// Act:
		final MosaicDefinition mosaicDefinition = context.mapping.map(dbMosaicDefinition);

		// Assert:
		Mockito.verify(context.mapper, Mockito.never()).map(Mockito.any(), Mockito.eq(MosaicId.class));
		Assert.assertThat(mosaicDefinition.getId(), IsEqual.equalTo(mosaicDefinition.getMosaicLevy().getMosaicId()));
	}

	private static DbMosaicDefinition createDbMosaicDefinition(final TestContext context, Long feeDbMosaicId) {
		final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		dbMosaicDefinition.setCreator(context.dbCreator);
		dbMosaicDefinition.setName("Alice's gift vouchers");
		dbMosaicDefinition.setDescription("precious vouchers");
		dbMosaicDefinition.setNamespaceId("alice.vouchers");
		dbMosaicDefinition.setProperties(context.propertiesMap.keySet());

		if (null != feeDbMosaicId) {
			dbMosaicDefinition.setFeeType(1);
			dbMosaicDefinition.setFeeRecipient(context.dbFeeRecipient);
			dbMosaicDefinition.setFeeDbMosaicId(feeDbMosaicId);
			dbMosaicDefinition.setFeeQuantity(123L);
		}

		return dbMosaicDefinition;
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Account creator = Utils.generateRandomAccount();
		private final DbAccount dbFeeRecipient = Mockito.mock(DbAccount.class);
		private final Account feeRecipient = Utils.generateRandomAccount();
		private final DbMosaicId dbMosaicId = new DbMosaicId(12L);

		private final Map<DbMosaicProperty, NemProperty> propertiesMap = new HashMap<DbMosaicProperty, NemProperty>() {
			{
				this.put(new DbMosaicProperty(), new NemProperty("divisibility", "5"));
				this.put(new DbMosaicProperty(), new NemProperty("initialSupply", "123"));
				this.put(new DbMosaicProperty(), new NemProperty("supplyMutable", "true"));
				this.put(new DbMosaicProperty(), new NemProperty("transferable", "true"));
			}
		};

		private final MosaicDefinitionDbModelToModelMapping mapping = new MosaicDefinitionDbModelToModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.dbCreator, Account.class)).thenReturn(this.creator);
			Mockito.when(this.mapper.map(this.dbFeeRecipient, Account.class)).thenReturn(this.feeRecipient);
			Mockito.when(this.mapper.map(this.dbMosaicId, MosaicId.class)).thenReturn(Utils.createMosaicId(12));

			for (final Map.Entry<DbMosaicProperty, NemProperty> entry : this.propertiesMap.entrySet()) {
				Mockito.when(this.mapper.map(entry.getKey(), NemProperty.class)).thenReturn(entry.getValue());
			}
		}
	}
}
