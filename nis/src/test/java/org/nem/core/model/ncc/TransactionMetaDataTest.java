package org.nem.core.model.ncc;

import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.BlockHeight;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

public class TransactionMetaDataTest {
	@Test
	public void canCreateTransactionMetaData() {
		// Arrange:
		final TransactionMetaData transactionMetaData = createTransactionMetaData(1234);

		// Assert:
		Assert.assertThat(transactionMetaData.getHeight(), equalTo(new BlockHeight(1234)));
	}

	@Test
	public void canRoundTripTransactionMetaData() {
		// Arrange:
		final TransactionMetaData entity = createTransactionMetaData(1234);

		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(entity, mock(AccountLookup.class));
		final TransactionMetaData result = new TransactionMetaData(deserializer);

		// Assert:
		Assert.assertThat(result.getHeight(), equalTo(entity.getHeight()));
	}

	private TransactionMetaData createTransactionMetaData(long height) {
		return new TransactionMetaData(new BlockHeight(height));
	}
}
