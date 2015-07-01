package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public class MosaicCreationModelToDbModelMappingTest extends AbstractTransferModelToDbModelMappingTest<MosaicCreationTransaction, DbMosaicCreationTransaction> {
	private static final Mosaic MOSAIC = Mockito.mock(Mosaic.class);
	private static final DbMosaic DB_MOSAIC = Mockito.mock(DbMosaic.class);

	@Test
	public void transactionCanBeMappedToDbModel() {
		// Arrange:
		final TestContext context = new TestContext();
		final MosaicCreationTransaction transfer = context.createModel();

		// Act:
		final DbMosaicCreationTransaction dbModel = context.mapping.map(transfer);

		// Assert:
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
		Mockito.verify(context.mapper, Mockito.times(1)).map(MOSAIC, DbMosaic.class);
		Mockito.verify(DB_MOSAIC, Mockito.times(1)).setPosition(0);
	}

	@Override
	protected MosaicCreationTransaction createModel(final TimeInstant timeStamp, final Account sender) {
		Mockito.when(MOSAIC.getCreator()).thenReturn(sender);
		return new MosaicCreationTransaction(timeStamp, sender, MOSAIC);
	}

	@Override
	protected MosaicCreationModelToDbModelMapping createMapping(final IMapper mapper) {
		Mockito.when(mapper.map(MOSAIC, DbMosaic.class)).thenReturn(DB_MOSAIC);
		return new MosaicCreationModelToDbModelMapping(mapper);
	}

	private static class TestContext {
		private final Account signer = Utils.generateRandomAccount();
		private final IMapper mapper = Mockito.mock(IMapper.class);
		private final MosaicCreationModelToDbModelMapping mapping = new MosaicCreationModelToDbModelMapping(this.mapper);

		public TestContext() {
			Mockito.when(this.mapper.map(MOSAIC, DbMosaic.class)).thenReturn(DB_MOSAIC);
			Mockito.when(MOSAIC.getCreator()).thenReturn(this.signer);
		}

		public MosaicCreationTransaction createModel() {
			return new MosaicCreationTransaction(
					TimeInstant.ZERO,
					signer,
					MOSAIC);
		}
	}
}
