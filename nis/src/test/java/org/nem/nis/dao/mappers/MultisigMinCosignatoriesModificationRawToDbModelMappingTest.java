package org.nem.nis.dao.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.nis.dbmodel.DbMultisigMinCosignatoriesModification;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class MultisigMinCosignatoriesModificationRawToDbModelMappingTest {

	@Test
	public void rawNonNullDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMultisigMinCosignatoriesModification dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getRelativeChange(), IsEqual.equalTo(12));
	}

	@Test
	public void rawNullDataIsMappedToNullDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = new Object[18];

		// Act:
		final DbMultisigMinCosignatoriesModification dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		Assert.assertThat(dbModel, IsNull.nullValue());
	}

	protected IMapping<Object[], DbMultisigMinCosignatoriesModification> createMapping(final IMapper mapper) {
		return new MultisigMinCosignatoriesModificationRawToDbModelMapping();
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);

		private TestContext() {
		}

		private Object[] createRaw() {
			final Object[] raw = new Object[18];
			raw[16] = BigInteger.valueOf(123L);                             // id
			raw[17] = 12;               									// relative change

			return raw;
		}
	}
}
