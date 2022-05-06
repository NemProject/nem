package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.HashSet;

public class MosaicDefinitionRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModelWithLevy() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(true);

		// Act:
		final DbMosaicDefinition dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creatorId, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.recipientId, DbAccount.class);
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getCreator(), IsEqual.equalTo(context.dbCreator));
		MatcherAssert.assertThat(dbModel.getName(), IsEqual.equalTo("Alice's vouchers"));
		MatcherAssert.assertThat(dbModel.getDescription(), IsEqual.equalTo("precious vouchers"));
		MatcherAssert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.voucher"));
		MatcherAssert.assertThat(dbModel.getProperties(), IsEqual.equalTo(new HashSet<>()));
		MatcherAssert.assertThat(dbModel.getFeeType(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getFeeRecipient(), IsEqual.equalTo(context.dbRecipient));
		MatcherAssert.assertThat(dbModel.getFeeDbMosaicId(), IsEqual.equalTo(234L));
		MatcherAssert.assertThat(dbModel.getFeeQuantity(), IsEqual.equalTo(345L));
	}

	@Test
	public void rawDataCanBeMappedToDbModelWithoutLevy() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(false);

		// Act:
		final DbMosaicDefinition dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creatorId, DbAccount.class);
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getCreator(), IsEqual.equalTo(context.dbCreator));
		MatcherAssert.assertThat(dbModel.getName(), IsEqual.equalTo("Alice's vouchers"));
		MatcherAssert.assertThat(dbModel.getDescription(), IsEqual.equalTo("precious vouchers"));
		MatcherAssert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.voucher"));
		MatcherAssert.assertThat(dbModel.getProperties(), IsEqual.equalTo(new HashSet<>()));
		MatcherAssert.assertThat(dbModel.getFeeType(), IsNull.nullValue());
		MatcherAssert.assertThat(dbModel.getFeeRecipient(), IsNull.nullValue());
		MatcherAssert.assertThat(dbModel.getFeeDbMosaicId(), IsNull.nullValue());
		MatcherAssert.assertThat(dbModel.getFeeQuantity(), IsNull.nullValue());
	}

	private IMapping<Object[], DbMosaicDefinition> createMapping(final IMapper mapper) {
		return new MosaicDefinitionRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Long creatorId = 678L;
		private final DbAccount dbRecipient = Mockito.mock(DbAccount.class);
		private final Long recipientId = 789L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.creatorId, DbAccount.class)).thenReturn(this.dbCreator);
			Mockito.when(this.mapper.map(this.recipientId, DbAccount.class)).thenReturn(this.dbRecipient);
		}

		private Object[] createRaw(final boolean hasFee) {
			final Object[] raw = new Object[9];
			raw[0] = BigInteger.valueOf(123L); // id
			raw[1] = BigInteger.valueOf(this.creatorId); // creator id
			raw[2] = "Alice's vouchers"; // name
			raw[3] = "precious vouchers"; // description
			raw[4] = "alice.voucher"; // namespace id
			if (hasFee) {
				raw[5] = 1; // fee type
				raw[6] = BigInteger.valueOf(this.recipientId); // fee recipient id
				raw[7] = BigInteger.valueOf(234L); // fee db mosaic id
				raw[8] = BigInteger.valueOf(345L); // fee quantity
			}
			return raw;
		}
	}
}
