package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class MosaicCreationModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<MosaicCreationTransaction, DbMosaicCreationTransaction> {

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicCreationTransaction transfer = context.createModel();

		// Act:
		final DbMosaicCreationTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		Mockito.verify(context.mapper, Mockito.times(1)).map(context.mosaic, DbMosaic.class);

		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		Assert.assertThat(dbModel.getMosaics(), IsEquivalent.equivalentTo(context.dbMosaic));
	}

	@Test
	public void transactionMosaicCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicCreationTransaction transfer = context.createModel();

		// Act:
		final DbMosaicCreationTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		final DbMosaic dbMosaic = dbModel.getMosaics().get(0);
		Assert.assertThat(dbMosaic.getPosition(), IsEqual.equalTo(0));
		Assert.assertThat(dbMosaic.getMosaicCreationTransaction(), IsEqual.equalTo(dbModel));
	}

	@Override
	protected MosaicCreationTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		return RandomTransactionFactory.createMosaicCreationTransaction(timeStamp, sender);
	}

	@Override
	protected MosaicCreationModelToDbModelMapping createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(DbMosaic.class))).thenReturn(new DbMosaic());
		return new MosaicCreationModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final Mosaic mosaic = Mockito.mock(Mosaic.class);
		private final DbMosaic dbMosaic = new DbMosaic();
		private final MosaicCreationModelToDbModelMapping mapping = new MosaicCreationModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(this.mosaic, DbMosaic.class)).thenReturn(this.dbMosaic);
			Mockito.when(this.mosaic.getCreator()).thenReturn(this.signer);
		}

		public MosaicCreationTransaction createModel() {
			return new MosaicCreationTransaction(
					TimeInstant.ZERO,
					this.signer,
					this.mosaic);
		}
	}
}
