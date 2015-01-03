package org.nem.nis.mappers;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.AbstractTransfer;
import org.nem.nis.test.NisUtils;

public abstract class AbstractTransferModelToDbModelMappingTest<TModel extends Transaction, TDbModel extends AbstractTransfer> {

	/**
	 * Creates a model with the specified values.
	 *
	 * @param timeStamp The time stamp.
	 * @param sender The sender.
	 */
	protected abstract TModel createModel(final TimeInstant timeStamp, final Account sender);

	/**
	 * Maps a model to a db model.
	 *
	 * @param model The model.
	 * @param mapper The mapper.
	 * @return The db model.
	 */
	protected abstract TDbModel map(final TModel model, final IMapper mapper);

	@Test
	public void modelCanBeMappedToDbModelWithSignature() {
		this.assertModelCanBeMapped(true);
	}

	@Test
	public void modelCanBeMappedToDbModelWithoutSignature() {
		this.assertModelCanBeMapped(false);
	}

	private void assertModelCanBeMapped(final boolean signModel) {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final TModel model = this.createModel(new TimeInstant(721), sender);

		model.setFee(Amount.fromNem(11));
		model.setDeadline(new TimeInstant(800));

		if (signModel) {
			model.sign();
		}

		final Hash modelHash = HashUtils.calculateHash(model);

		final org.nem.nis.dbmodel.Account dbAccount = new org.nem.nis.dbmodel.Account();
		final IMapper mapper = Mockito.mock(IMapper.class);
		Mockito.when(mapper.map(sender, org.nem.nis.dbmodel.Account.class)).thenReturn(dbAccount);

		// Act:
		final AbstractTransfer dbModel = this.map(model, mapper);

		// Assert:
		Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(modelHash));
		Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(11000000L));
		Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));

		Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
		Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbAccount));

		if (signModel) {
			Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(model.getSignature().getBytes()));
		} else {
			Assert.assertThat(dbModel.getSenderProof(), IsNull.nullValue());
		}

		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
	}
}