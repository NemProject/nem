package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Signature;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.AbstractTransfer;

public abstract class AbstractTransferDbModelToModelMappingTest<TDbModel extends AbstractTransfer, TModel extends Transaction> {

	/**
	 * Creates a db model.
	 *
	 * @return The db model.
	 */
	protected abstract TDbModel createDbModel();

	/**
	 * Creates a mapping that can map a db model to a model.
	 *
	 * @param mapper The mapper.
	 * @return The mapping.
	 */
	protected abstract IMapping<TDbModel, TModel> createMapping(final IMapper mapper);

	@Test
	public void abstractDbModelWithSignatureCanBeMappedToModel() {
		// Arrange:
		final Signature signature = Utils.generateRandomSignature();
		final TDbModel dbModel = this.createDbModel();
		dbModel.setSenderProof(signature.getBytes());
		dbModel.setFee(2310000000L);
		dbModel.setDeadline(800);
		dbModel.setTimeStamp(0);
		dbModel.setVersion(0);

		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Account.class))).thenReturn(Utils.generateRandomAccount());

		// Act:
		final Transaction model = this.createMapping(mapper).map(dbModel);

		// Assert:
		Assert.assertThat(model.getSignature(), IsEqual.equalTo(signature));
		Assert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(2310000000L)));
		Assert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(800)));
	}

	@Test
	public void abstractDbModelWithoutSignatureCanBeMappedToModel() {
		// Arrange:
		final TDbModel dbModel = this.createDbModel();
		dbModel.setFee(2310000000L);
		dbModel.setDeadline(800);
		dbModel.setTimeStamp(0);
		dbModel.setVersion(0);

		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(Mockito.any(), Mockito.eq(Account.class))).thenReturn(Utils.generateRandomAccount());

		// Act:
		final Transaction model = this.createMapping(mapper).map(dbModel);

		// Assert:
		Assert.assertThat(model.getSignature(), IsNull.nullValue());
		Assert.assertThat(model.getFee(), IsEqual.equalTo(Amount.fromMicroNem(2310000000L)));
		Assert.assertThat(model.getDeadline(), IsEqual.equalTo(new TimeInstant(800)));
	}
}