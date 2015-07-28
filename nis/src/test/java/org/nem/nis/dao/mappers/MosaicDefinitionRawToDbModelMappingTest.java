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
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getCreator(), IsEqual.equalTo(context.dbCreator));
		Assert.assertThat(dbModel.getName(), IsEqual.equalTo("Alice's vouchers"));
		Assert.assertThat(dbModel.getDescription(), IsEqual.equalTo("precious vouchers"));
		Assert.assertThat(dbModel.getNamespaceId(), IsEqual.equalTo("alice.voucher"));
		Assert.assertThat(dbModel.getProperties(), IsEqual.equalTo(new HashSet<>()));
	}

	protected IMapping<Object[], DbMosaicDefinition> createMapping(final IMapper mapper) {
		return new MosaicDefinitionRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCreator = Mockito.mock(DbAccount.class);
		private final Long creatorId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.creatorId, DbAccount.class)).thenReturn(this.dbCreator);
		}

		private Object[] createRaw() {
			final Object[] raw = new Object[5];
			raw[0] = BigInteger.valueOf(123L);            // id
			raw[1] = BigInteger.valueOf(this.creatorId);  // creator id
			raw[2] = "Alice's vouchers";                  // name
			raw[3] = "precious vouchers";                 // description
			raw[4] = "alice.voucher";                     // namespace id
			return raw;
		}
	}
}
