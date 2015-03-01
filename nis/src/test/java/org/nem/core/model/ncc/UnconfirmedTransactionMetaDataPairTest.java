package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

public class UnconfirmedTransactionMetaDataPairTest {

	@Test
	public void canCreateUnconfirmedTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.sign();
		final UnconfirmedTransactionMetaData metaData = new UnconfirmedTransactionMetaData(Utils.generateRandomHash());

		// Act:
		final UnconfirmedTransactionMetaDataPair entity = new UnconfirmedTransactionMetaDataPair(transaction, metaData);

		// Assert:
		Assert.assertThat(entity.getTransaction(), IsSame.sameInstance(transaction));
		Assert.assertThat(entity.getMetaData(), IsSame.sameInstance(metaData));
	}

	@Test
	public void canRoundTripUnconfirmedTransactionMetaDataPair() {
		// Arrange:
		final Account signer = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();

		// Act:
		final UnconfirmedTransactionMetaDataPair metaDataPair = createRoundTrippedPair(signer, 123, hash);

		// Assert:
		Assert.assertThat(metaDataPair.getTransaction().getType(), IsEqual.equalTo(TransactionTypes.TRANSFER));
		final TransferTransaction transaction = (TransferTransaction)metaDataPair.getTransaction();
		Assert.assertThat(transaction.getAmount(), IsEqual.equalTo(Amount.fromNem(123)));
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));

		Assert.assertThat(metaDataPair.getMetaData().getInnerTransactionHash(), IsEqual.equalTo(hash));
	}

	private static UnconfirmedTransactionMetaDataPair createRoundTrippedPair(
			final Account signer,
			final long amount,
			final Hash innerTransactionHash) {
		// Arrange:
		final Transaction transaction = new TransferTransaction(
				TimeInstant.ZERO,
				signer,
				signer,
				Amount.fromNem(amount),
				null);
		transaction.sign();
		final UnconfirmedTransactionMetaData metaData = new UnconfirmedTransactionMetaData(innerTransactionHash);
		final UnconfirmedTransactionMetaDataPair metaDataPair = new UnconfirmedTransactionMetaDataPair(transaction, metaData);

		// Act:
		return new UnconfirmedTransactionMetaDataPair(Utils.roundtripSerializableEntity(metaDataPair, new MockAccountLookup()));
	}
}
