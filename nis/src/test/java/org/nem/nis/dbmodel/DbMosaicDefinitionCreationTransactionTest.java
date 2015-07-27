package org.nem.nis.dbmodel;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.ExceptionAssert;

public class DbMosaicDefinitionCreationTransactionTest {

	//region setSender

	@Test
	public void setSenderSetsCreatorInMosaic() {
		// Arrange:
		final DbMosaicDefinitionCreationTransaction transaction = new DbMosaicDefinitionCreationTransaction();
		transaction.setMosaicDefinition(new DbMosaicDefinition());
		final DbAccount sender = new DbAccount();

		// Act:
		transaction.setSender(sender);

		// Assert:
		Assert.assertThat(transaction.getSender(), IsEqual.equalTo(sender));
		Assert.assertThat(transaction.getMosaicDefinition().getCreator(), IsEqual.equalTo(sender));
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
}