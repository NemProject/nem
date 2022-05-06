package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class NamespaceRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbNamespace dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getFullName(), IsEqual.equalTo("foo.bar"));
		MatcherAssert.assertThat(dbModel.getOwner(), IsEqual.equalTo(context.dbOwner));
		MatcherAssert.assertThat(dbModel.getHeight(), IsEqual.equalTo(321L));
		MatcherAssert.assertThat(dbModel.getLevel(), IsEqual.equalTo(2));
	}

	private IMapping<Object[], DbNamespace> createMapping(final IMapper mapper) {
		return new NamespaceRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbOwner = Mockito.mock(DbAccount.class);
		private final Long ownerId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.ownerId, DbAccount.class)).thenReturn(this.dbOwner);
		}

		private Object[] createRaw() {
			final Object[] raw = new Object[5];
			raw[0] = BigInteger.valueOf(123L); // id
			raw[1] = "foo.bar"; // full name
			raw[2] = BigInteger.valueOf(this.ownerId); // owner id
			raw[3] = BigInteger.valueOf(321L); // expiry height
			raw[4] = 2; // level

			return raw;
		}
	}
}
