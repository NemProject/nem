package org.nem.nis.dao.mappers;

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

	private void assertDbModelFields(
			final TestContext context,
			final DbBlock dbModel,
			final DbAccount lessor) {
		// Assert:
		Assert.assertThat(dbModel, IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlockTransferTransactions(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlockImportanceTransferTransactions(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlockMultisigAggregateModificationTransactions(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getBlockMultisigTransactions(), IsNull.notNullValue());
		Assert.assertThat(dbModel.getId(), IsEqual.equalTo(123L));
		Assert.assertThat(dbModel.getShortId(), IsEqual.equalTo(context.blockHash.getShortId()));
		Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getPrevBlockHash(), IsEqual.equalTo(context.previousBlockHash));
		Assert.assertThat(dbModel.getBlockHash(), IsEqual.equalTo(context.blockHash));
		Assert.assertThat(dbModel.getGenerationHash(), IsEqual.equalTo(context.generationHash));
		Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(345));
		Assert.assertThat(dbModel.getHarvester(), IsEqual.equalTo(context.dbHarvester));
		Assert.assertThat(dbModel.getHarvesterProof(), IsEqual.equalTo(context.harvesterProof));
		Assert.assertThat(dbModel.getLessor(), IsEqual.equalTo(lessor));
		Assert.assertThat(dbModel.getHeight(), IsEqual.equalTo(456L));
		Assert.assertThat(dbModel.getTotalFee(), IsEqual.equalTo(567L));
		Assert.assertThat(dbModel.getDifficulty(), IsEqual.equalTo(678L));
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
			final Object[] raw = new Object[13];
			raw[0] = BigInteger.valueOf(123L);                              // id
			raw[1] = BigInteger.valueOf(this.blockHash.getShortId());       // short id
			raw[2] = 1;                                                     // version
			raw[3] = this.previousBlockHash.getRaw();                       // raw previous block hash
			raw[4] = this.blockHash.getRaw();                               // raw block hash
			raw[5] = this.generationHash.getRaw();                          // raw generation hash
			raw[6] = 345;                                                   // timestamp
			raw[7] = BigInteger.valueOf(this.harvesterId);                  // harvester id
			raw[8] = this.harvesterProof;                                   // harvester proof
			raw[9] = useLessor ? BigInteger.valueOf(this.lessorId) : null;  // lessor id
			raw[10] = BigInteger.valueOf(456L);                             // height
			raw[11] = BigInteger.valueOf(567L);                             // total fee
			raw[12] = BigInteger.valueOf(678L);                             // difficulty

			return raw;
		}
	}
}
