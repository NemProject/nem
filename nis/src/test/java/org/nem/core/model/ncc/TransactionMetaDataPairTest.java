package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class TransactionMetaDataPairTest {

	@Test
	public void canCreateTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.sign();
		final TransactionMetaData metaData = new TransactionMetaData(new BlockHeight(1234), 123L, Hash.ZERO);
		final TransactionMetaDataPair entity = new TransactionMetaDataPair(transaction, metaData);

		// Assert:
		Assert.assertThat(entity.getTransaction(), IsSame.sameInstance(transaction));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();

		// Act:
		final TransactionMetaDataPair metaDataPair = createRoundTrippedPair(signer, 123, 8756, 5678);

		// Assert:
		Assert.assertThat(metaDataPair.getTransaction().getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
		final TransferTransaction transaction = (TransferTransaction)metaDataPair.getTransaction();
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));

		Assert.assertThat(metaDataPair.getMetaData().getHeight(), IsEqual.equalTo(new BlockHeight(8756)));
		Assert.assertThat(metaDataPair.getMetaData().getId(), IsEqual.equalTo(5678L));
	}

	private static TransactionMetaDataPair createRoundTrippedPair(
			final Account signer,
			final long amount,
			final long blockHeight,
			final long transactionId) {
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
		final TransactionMetaData metaData = new TransactionMetaData(new BlockHeight(blockHeight), transactionId, Hash.ZERO);
		final TransactionMetaDataPair metaDataPair = new TransactionMetaDataPair(transaction, metaData);

		// Act:
		return new TransactionMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, accountLookup));
	}
}
