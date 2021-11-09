package org.nem.nis.dbmodel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbMosaicDefinitionCreationTransactionTest {

	// region setSender

	@Test
	public void setSenderSetsCreatorInMosaic() {
		// Arrange:
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(null);
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		MatcherAssert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(transaction.getMosaicDefinition().getCreator(), IsSame.sameInstance(sender));
	}

	@Test
	public void setSenderSetsFeeRecipientInMosaicIfFeeRecipientEqualsSender() {
		// Arrange:
		final DbAccount sender = new DbAccount(123L);
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(new DbAccount(123L));

		// Act:
		transaction.setSender(sender);

		// Assert:
		MatcherAssert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(transaction.getMosaicDefinition().getFeeRecipient(), IsSame.sameInstance(sender));
	}

	@Test
	public void setSenderDoesNotSetFeeRecipientInMosaicIfFeeRecipientIsNotEqualToSender() {
		// Arrange:
		final DbAccount sender = new DbAccount(123L);
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(new DbAccount(234L));

		// Act:
		transaction.setSender(sender);

		// Assert:
		MatcherAssert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		MatcherAssert.assertThat(transaction.getMosaicDefinition().getFeeRecipient(), IsEqual.equalTo(new DbAccount(234L)));
	}

	@Test
	public void setSenderCannotBeCalledBeforeSetMosaic() {
		// Arrange:
		final DbMosaicDefinitionCreationTransaction transaction = new DbMosaicDefinitionCreationTransaction();
		final DbAccount sender = new DbAccount();

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setSender(sender), IllegalStateException.class);
	}

	@Test
	public void setSenderDoesNotSetFeeRecipientInMosaicIfFeeRecipientDoesNotHaveValidId() {
		// Arrange:
		final DbAccount feeRecipient = new DbAccount();
		final DbMosaicDefinitionCreationTransaction transaction = createTransaction(feeRecipient);

		// Act:
		transaction.setSender(new DbAccount(123L));

		// Assert:
		MatcherAssert.assertThat(transaction.getMosaicDefinition().getFeeRecipient(), IsSame.sameInstance(feeRecipient));
	}

	// endregion

	private static DbMosaicDefinitionCreationTransaction createTransaction(final DbAccount feeRecipient) {
		final DbMosaicDefinitionCreationTransaction transaction = new DbMosaicDefinitionCreationTransaction();
		final DbMosaicDefinition mosaicDefinition = new DbMosaicDefinition();
		mosaicDefinition.setFeeRecipient(feeRecipient);
		transaction.setMosaicDefinition(mosaicDefinition);
		return transaction;
	}
}
