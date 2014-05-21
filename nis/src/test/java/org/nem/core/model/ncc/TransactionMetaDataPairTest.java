package org.nem.core.model.ncc;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.test.MockTransaction;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionMetaDataPairTest {
	@Test
	public void canCreateTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.sign();
		final TransactionMetaData transactionMetaData = new TransactionMetaData(new BlockHeight(1234));
		final TransactionMetaDataPair entity = new TransactionMetaDataPair(transaction, transactionMetaData);

		// Assert:
		Assert.assertThat(entity.getTransaction().getType(), IsEqual.equalTo(transaction.getType()));
		final MockTransaction result = (MockTransaction)entity.getTransaction();
		Assert.assertThat(result.getCustomField(), equalTo(transaction.getCustomField()));
	}

	@Test
	public void canRoundTripTransactionMetaDataPair() {
		// Arrange:
		final AccountLookup accountLookup = mock(AccountLookup.class);
		final Account signer = Utils.generateRandomAccount();
		when(accountLookup.findByAddress(signer.getAddress())).thenReturn(signer);

		final TransferTransaction transaction = new TransferTransaction(TimeInstant.ZERO, signer, signer, Amount.fromNem(123), null);
		transaction.sign();
		final TransactionMetaData transactionMetaData = new TransactionMetaData(new BlockHeight(1234));
		final TransactionMetaDataPair transactionMetaDataPair = new TransactionMetaDataPair(transaction, transactionMetaData);

		// Act:
		final TransactionMetaDataPair roundTripped = new TransactionMetaDataPair(Utils.roundtripSerializableEntity(transactionMetaDataPair, accountLookup));

		// Assert:
		Assert.assertThat(roundTripped.getTransaction().getType(), IsEqual.equalTo(transaction.getType()));
		final TransferTransaction result = (TransferTransaction)roundTripped.getTransaction();
		Assert.assertThat(result.getAmount(), equalTo(transaction.getAmount()));
		Assert.assertThat(result.getSigner(), equalTo(transaction.getSigner()));
	}
}
