package org.nem.nis.dao.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.test.Utils;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.*;

import java.math.BigInteger;

public class BlockRawToDbModelMappingTest {

	@Test
	public void rawDataWithoutLessorCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(false);

		// Act:
		final DbBlock dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(context, dbModel, null);
	}

	@Test
	public void rawDataWithLessorCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final Object[] raw = context.createRaw(true);

		// Act:
		final DbBlock dbModel = this.createMapping(context.mapper).map(raw);

		// Assert:
		this.assertDbModelFields(context, dbModel, context.dbLessor);
	}

	private void assertDbModelFields(final TestContext context, final DbBlock dbModel, final DbAccount lessor) {
		// Assert:
		MatcherAssert.assertThat(dbModel, IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlockTransferTransactions(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlockImportanceTransferTransactions(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getBlockMultisigTransactions(), IsNull.notNullValue());
		MatcherAssert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		MatcherAssert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		MatcherAssert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(context.previousBlockHash));
		MatcherAssert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(context.blockHash));
		MatcherAssert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(context.generationHash));
		MatcherAssert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(345));
		MatcherAssert.assertThat(dbModel.getHarvester(), IsEqual.equalTo(context.dbHarvester));
		MatcherAssert.assertThat(dbModel.getHarvesterProof(), IsEqual.equalTo(context.harvesterProof));
		MatcherAssert.assertThat(dbModel.getLessor(), IsEqual.equalTo(lessor));
		MatcherAssert.assertThat(dbModel.getHeight(), IsEqual.equalTo(456L));
		MatcherAssert.assertThat(dbModel.getTotalFee(), IsEqual.equalTo(567L));
		MatcherAssert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(678L));
	}

	private IMapping<Object[], DbBlock> createMapping(final IMapper mapper) {
		return new BlockRawToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final DbAccount dbHarvester = Mockito.mock(DbAccount.class);
		private final DbAccount dbLessor = Mockito.mock(DbAccount.class);
		private final Long harvesterId = 678L;
		private final Long lessorId = 789L;
		private final Hash previousBlockHash = Utils.generateRandomHash();
		private final Hash blockHash = Utils.generateRandomHash();
		private final Hash generationHash = Utils.generateRandomHash();
		private final byte[] harvesterProof = Utils.generateRandomBytes(64);

		private TestContext() {
			Mockito.when(this.mapper.map(this.harvesterId, DbAccount.class)).thenReturn(this.dbHarvester);
			Mockito.when(this.mapper.map(this.lessorId, DbAccount.class)).thenReturn(this.dbLessor);
		}

		private Object[] createRaw(final boolean useLessor) {
			final Object[] raw = new Object[12];
			raw[0] = BigInteger.valueOf(123L); // id
			raw[1] = 1; // version
			raw[2] = this.previousBlockHash.getRaw(); // raw previous block hash
			raw[3] = this.blockHash.getRaw(); // raw block hash
			raw[4] = this.generationHash.getRaw(); // raw generation hash
			raw[5] = 345; // timestamp
			raw[6] = BigInteger.valueOf(this.harvesterId); // harvester id
			raw[7] = this.harvesterProof; // harvester proof
			raw[8] = useLessor ? BigInteger.valueOf(this.lessorId) : null; // lessor id
			raw[9] = BigInteger.valueOf(456L); // height
			raw[10] = BigInteger.valueOf(567L); // total fee
			raw[11] = BigInteger.valueOf(678L); // difficulty

			return raw;
		}
	}
}
