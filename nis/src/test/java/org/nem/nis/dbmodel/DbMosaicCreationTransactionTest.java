package org.nem.nis.dbmodel;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbMosaicCreationTransactionTest {

	//region setSender

	@Test
	public void setSenderSetsCreatorInMosaic() {
		// Arrange:
		final DbMosaicCreationTransaction transaction = new DbMosaicCreationTransaction();
		transaction.setMosaic(new DbMosaic());
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getMosaic().getCreator(), IsEqual.equalTo(sender));
	}

	@Test
	public void setSenderCannotBeCalledBeforeSetMosaic() {
		// Arrange:
		final DbMosaicCreationTransaction transaction = new DbMosaicCreationTransaction();
		final DbAccount sender = new DbAccount();

		// Act:
		ExceptionAssert.assertThrows(v -> transaction.setSender(sender), IllegalStateException.class);
	}

	//endregion
}