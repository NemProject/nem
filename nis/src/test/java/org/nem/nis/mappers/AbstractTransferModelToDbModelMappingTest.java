package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

public abstract class AbstractTransferModelToDbModelMappingTest<TModel extends Transaction, TDbModel extends AbstractTransfer> {

	/**
	 * Creates a model with the specified values.
	 *
	 * @param timeStamp The time stamp.
	 * @param sender The sender.
	 * @return The model.
	 */
	protected abstract TModel createModel(final TimeInstant timeStamp, final Account sender);

	/**
	 * Creates a mapping that can map a model to a db model.
	 *
	 * @param mapper The mapper.
	 * @return The mapping.
	 */
	protected abstract IMapping<TModel, TDbModel> createMapping(final IMapper mapper);

	@Test
	public void abstractModelCanBeMappedToDbModelWithSignature() {
		this.assertModelCanBeMapped(true);
	}

	@Test
	public void abstractModelCanBeMappedToDbModelWithoutSignature() {
		this.assertModelCanBeMapped(false);
	}

	private void assertModelCanBeMapped(final boolean signModel) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final TModel model = this.createModel(new TimeInstant(721), sender);

		model.setFee(Amount.fromNem(2310));
		model.setDeadline(new TimeInstant(800));

		if (signModel) {
			model.sign();
		}

		final Hash modelHash = HashUtils.calculateHash(model);

		final DbAccount dbAccount = new DbAccount(1);
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(sender, DbAccount.class)).thenReturn(dbAccount);

		// Act:
		final AbstractTransfer dbModel = this.createMapping(mapper).map(model);

		// Assert:
		MatcherAssert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(modelHash));
		MatcherAssert.assertThat(dbModel.getVersion(), IsEqual.equalTo(this.getVersion()));
		MatcherAssert.assertThat(dbModel.getFee(), IsEqual.equalTo(2310000000L));
		MatcherAssert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));

		MatcherAssert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
		MatcherAssert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbAccount));

		if (signModel) {
			MatcherAssert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(model.getSignature().getBytes()));
		} else {
			MatcherAssert.assertThat(dbModel.getSenderProof(), IsNull.nullValue());
		}
	}

	protected int getVersion() {
		return VerifiableEntityUtils.VERSION_ONE;
	}
}
