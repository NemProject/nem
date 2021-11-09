package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class MultisigCosignatoryModificationRawToDbModelMappingTest {

	@Test
	public void rawDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMultisigModification dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getCosignatory(), IsEqual.equalTo(context.dbCosignatory));
		MatcherAssert.assertThat(dbModel.getModificationType(), IsEqual.equalTo(234));
	}

	private IMapping<Object[], DbMultisigModification> createMapping(final IMapper mapper) {
		return new MultisigModificationRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbCosignatory = Mockito.mock(DbAccount.class);
		private final Long cosignatoryId = 678L;

		private TestContext() {
			Mockito.when(this.mapper.map(this.cosignatoryId, DbAccount.class)).thenReturn(this.dbCosignatory);
		}

		private Object[] createRaw() {
			final Object[] raw = new Object[16];
			raw[13] = BigInteger.valueOf(123L); // id
			raw[14] = BigInteger.valueOf(this.cosignatoryId); // cosignatory id
			raw[15] = 234; // modification type

			return raw;
		}
	}
}
