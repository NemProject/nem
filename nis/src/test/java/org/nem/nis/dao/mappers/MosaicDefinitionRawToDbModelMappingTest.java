package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;
import java.util.HashSet;

public class MosaicDefinitionRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMosaicDefinition dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creatorId, DbAccount.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.recipientId, DbAccount.class);
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbModel.getName(), IsEqual.equalTo("Alice's vouchers"));
		Assert.assertThat(dbModel.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.voucher"));
		Assert.assertThat(dbModel.getProperties(), IsEqual.equalTo(new HashSet<>()));
		Assert.assertThat(dbModel.getFeeType(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getFeeRecipient(), IsEqual.equalTo(context.dbRecipient));
		Assert.assertThat(dbModel.getFeeDbMosaicId(), IsEqual.equalTo(234L));
		Assert.assertThat(dbModel.getFeeQuantity(), IsEqual.equalTo(345L));
	}

	protected IMapping<Object[], DbMosaicDefinition> createMapping(final IMapper mapper) {
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

		private Object[] createRaw() {
			final Object[] raw = new Object[9];
			raw[0] = BigInteger.valueOf(123L);                // id
			raw[1] = BigInteger.valueOf(this.creatorId);      // creator id
			raw[2] = "Alice's vouchers";                      // name
			raw[3] = "precious vouchers";                     // description
			raw[4] = "alice.voucher";                         // namespace id
			raw[5] = 1;                                       // fee type
			raw[6] = BigInteger.valueOf(this.recipientId);    // fee recipient id
			raw[7] = BigInteger.valueOf(234L);                // fee db mosaic id
			raw[8] = BigInteger.valueOf(345L);                // fee quantity
			return raw;
		}
	}
}
