package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class MosaicDefinitionCreationModelToDbModelMappingTest
		extends
			AbstractTransferModelToDbModelMappingTest<MosaicDefinitionCreationTransaction, DbMosaicDefinitionCreationTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicDefinitionCreationTransaction transfer = context.createModel();

		// Act:
		final DbMosaicDefinitionCreationTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		MatcherAssert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		MatcherAssert.assertThat(dbModel.getMosaicDefinition(), IsEqual.equalTo(context.dbMosaicDefinition));
		MatcherAssert.assertThat(dbModel.getCreationFeeSink(), IsEqual.equalTo(context.dbCreationFeeSink));
		MatcherAssert.assertThat(dbModel.getCreationFee(), IsEqual.equalTo(25_000_000L));
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaicDefinition, DbMosaicDefinition.class);
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.creationFeeSink, DbAccount.class);
	}

	@Override
	protected MosaicDefinitionCreationTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return RandomTransactionFactory.createMosaicDefinitionCreationTransaction(timeStamp, sender);
	}

	@Override
	protected MosaicDefinitionCreationModelToDbModelMapping createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaicDefinition.class))).thenReturn(new DbMosaicDefinition());
		return new MosaicDefinitionCreationModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicDefinition mosaicDefinition = Mockito.mock(MosaicDefinition.class);
		private final DbMosaicDefinition dbMosaicDefinition = new DbMosaicDefinition();
		private final Account creationFeeSink = Utils.generateRandomAccount();
		private final DbAccount dbCreationFeeSink = Mockito.mock(DbAccount.class);
		private final MosaicDefinitionCreationModelToDbModelMapping mapping = new MosaicDefinitionCreationModelToDbModelMapping(
				this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.mosaicDefinition, DbMosaicDefinition.class)).thenReturn(this.dbMosaicDefinition);
			Mockito.when(this.mosaicDefinition.getCreator()).thenReturn(this.signer);
			Mockito.when(this.mapper.map(this.creationFeeSink, DbAccount.class)).thenReturn(this.dbCreationFeeSink);
		}

		public MosaicDefinitionCreationTransaction createModel() {
			return new MosaicDefinitionCreationTransaction(TimeInstant.ZERO, this.signer, this.mosaicDefinition, this.creationFeeSink,
					Amount.fromNem(25));
		}
	}
}
