package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.MockAccountLookup;
import org.nem.core.test.MockTransaction;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class TransactionMetaDataPairTest {

	@Test
	public void canCreateTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.sign();
		final TransactionMetaData metaData = new TransactionMetaData(new BlockHeight(1234));
		final TransactionMetaDataPair entity = new TransactionMetaDataPair(transaction, metaData);

		// Assert:
		Assert.assertThat(entity.getTransaction(), IsSame.sameInstance(transaction));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	// TODO: review next
	@Test
	public void canRoundTripTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final TransactionMetaDataPair metaDataPair = createRoundTrippedPair(signer, 123, 8756);

		// Assert:
		Assert.assertThat(metaDataPair.getTransaction().getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
		final TransferTransaction transaction = (TransferTransaction)metaDataPair.getTransaction();
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));

		Assert.assertThat(metaDataPair.getMetaData().getHeight(), IsEqual.equalTo(new BlockHeight(8756)));
	}

	private static TransactionMetaDataPair createRoundTrippedPair(
			final Account signer,
			final long amount,
			final long blockHeight) {
		// Arrange:
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(signer);

		final Transaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				signer,
			    signer,
				Amount.fromNem(amount),
				null);
		transaction.sign();
		final TransactionMetaData metaData = new TransactionMetaData(new BlockHeight(blockHeight));
		final TransactionMetaDataPair metaDataPair = new TransactionMetaDataPair(transaction, metaData);

		// Act:
		return new TransactionMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, accountLookup));
	}
}
