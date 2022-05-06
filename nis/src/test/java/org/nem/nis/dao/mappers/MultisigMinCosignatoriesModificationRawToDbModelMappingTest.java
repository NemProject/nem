package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.dbmodel.DbMultisigMinCosignatoriesModification;
import org.nem.nis.mappers.IMapping;

import java.math.BigInteger;

public class MultisigMinCosignatoriesModificationRawToDbModelMappingTest {

	@Test
	public void rawNonNullDataCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw();

		// Act:
		final DbMultisigMinCosignatoriesModification dbModel = this.createMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getRelativeChange(), IsEqual.equalTo(12));
	}

	@Test
	public void rawNullDataIsMappedToNullDbModel() {
		// Arrange:
		final Object[] raw = new Object[18];

		// Act:
		final DbMultisigMinCosignatoriesModification dbModel = this.createMapping().map(raw);

		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.nullValue());
	}

	private IMapping<Object[], DbMultisigMinCosignatoriesModification> createMapping() {
		return new MultisigMinCosignatoriesModificationRawToDbModelMapping();
	}

	private static class TestContext {

		private Object[] createRaw() {
			final Object[] raw = new Object[18];
			raw[16] = BigInteger.valueOf(123L); // id
			raw[17] = 12; // relative change
			return raw;
		}
	}
}
