package org.nem.nis.mappers;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;
import org.nem.nis.test.NisUtils;

public class AbstractTransferMapperTest {

	@Test
	public void canMapModelToDbModel() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Transaction model = new TransferTransaction(
				new TimeInstant(721),
				sender,
				Utils.generateRandomAccount(),
				new Amount(144),
				null);

		model.setFee(Amount.fromNem(11));
		model.setDeadline(new TimeInstant(800));
		model.sign();

		final Hash modelHash = HashUtils.calculateHash(model);

		// Act:
		final org.nem.nis.dbmodel.Account dbAccount = new org.nem.nis.dbmodel.Account();
		final AbstractTransfer dbModel = new ImportanceTransfer();
		dbModel.setBlock(NisUtils.createDbBlockWithTimeStamp(100));
		AbstractTransferMapper.toDbModel(
				model,
				dbAccount,
				11,
				22,
				dbModel);

		// Assert:
		Assert.assertThat(dbModel.getTransferHash(), IsEqual.equalTo(modelHash));
		Assert.assertThat(dbModel.getVersion(), IsEqual.equalTo(1));
		Assert.assertThat(dbModel.getFee(), IsEqual.equalTo(11000000L));
		Assert.assertThat(dbModel.getTimeStamp(), IsEqual.equalTo(721));

		Assert.assertThat(dbModel.getDeadline(), IsEqual.equalTo(800));
		Assert.assertThat(dbModel.getSender(), IsEqual.equalTo(dbAccount));
		Assert.assertThat(dbModel.getSenderProof(), IsEqual.equalTo(model.getSignature().getBytes()));
		Assert.assertThat(dbModel.getOrderId(), IsEqual.equalTo(-1));
		Assert.assertThat(dbModel.getBlkIndex(), IsEqual.equalTo(11));
		Assert.assertThat(dbModel.getReferencedTransaction(), IsEqual.equalTo(0L));
	}
}