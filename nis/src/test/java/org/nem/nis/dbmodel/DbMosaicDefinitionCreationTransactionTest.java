package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbMosaicDefinitionCreationTransactionTest {

	//region setSender

	@Test
	public void setSenderSetsCreatorInMosaic() {
		// Arrange:
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(null);
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getMosaicDefinition().getCreator(), IsSame.sameInstance(sender));
	}

	@Test
	public void setSenderSetsFeeRecipientInMosaicIfFeeRecipientEqualsSender() {
		// Arrange:
		final DbAccount sender = new DbAccount(123L);
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(new DbAccount(123L));

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getMosaicDefinition().getFeeRecipient(), IsSame.sameInstance(sender));
	}

	@Test
	public void setSenderDoesNotSetFeeRecipientInMosaicIfFeeRecipientIsNotEqualToSender() {
		// Arrange:
		final DbAccount sender = new DbAccount(123L);
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(new DbAccount(234L));

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getMosaicDefinition().getFeeRecipient(), IsEqual.equalTo(new DbAccount(234L)));
	}

	@Test
	public void setSenderCannotBeCalledBeforeSetMosaic() {
		// Arrange:
		final DbMosaicDefinitionCreationTransaction transaction = new DbMosaicDefinitionCreationTransaction();
		final DbAccount sender = new DbAccount();

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setSender(sender), IllegalStateException.class);
	}

	//endregion

	private static DbMosaicDefinitionCreationTransaction createTransaction(final DbAccount feeRecipient) {
		final DbMosaicDefinitionCreationTransaction transaction = new DbMosaicDefinitionCreationTransaction();
		final DbMosaicDefinition mosaicDefinition = new DbMosaicDefinition();
		mosaicDefinition.setFeeRecipient(feeRecipient);
		transaction.setMosaicDefinition(mosaicDefinition);
		return transaction;
	}
}