package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

public class TransactionTest {

    //region Constructors

    @Test
    public void ctorCanCreateTransactionForAccountWithSignerPrivateKey() {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account signer = new Account(publicPrivateKeyPair);

		// Act:
        final MockTransaction transaction = new MockTransaction(signer, 6);

		// Assert:
        Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
        Assert.assertThat(transaction.getFee(), IsEqual.equalTo(0L));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(MockTransaction.TIMESTAMP));
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(0));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
    }

	@Test
	public void transactionWithWrongDeadlineIsInvalid() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer, 6);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(0L));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
	}

	@Test
	public void transactionWithCorrectDeadlineIsValid() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer, 6);
		transaction.setDeadline(transaction.getTimeStamp() + 1);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(0L));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

	@Test
	public void transactionWithNegativeTimestampIsInvalid() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer, 6, -10);

		// Assert:
		Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(transaction.getFee(), IsEqual.equalTo(0L));
		Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
	}

    // TODO: fix this test
//	@Test
//	public void transactionWithTooDistantDeadlineIsInvalid() {
//		// Arrange:
//		final KeyPair publicPrivateKeyPair = new KeyPair();
//		final Account signer = new Account(publicPrivateKeyPair);
//
//		// Act:
//		final MockTransaction transaction = new MockTransaction(signer, 6, 5*24*60*60);
////		transaction.setTimeStamp(5*24*60*60);
//		transaction.setDeadline(6*24*60*60);
//
//		// Assert:
//		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
//	}

	@Test
	public void transactionMaxDeadlineIsValid() {
		// Arrange:
		final KeyPair publicPrivateKeyPair = new KeyPair();
		final Account signer = new Account(publicPrivateKeyPair);

		// Act:
		final MockTransaction transaction = new MockTransaction(signer, 6, 5*24*60*60);
		transaction.setDeadline(6*24*60*60 - 1);

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

    @Test
    public void transactionCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockTransaction originalTransaction = new MockTransaction(signer, 7);
        originalTransaction.setFee(130);
        final MockTransaction transaction = createRoundTrippedTransaction(originalTransaction, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(transaction.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
        Assert.assertThat(transaction.getFee(), IsEqual.equalTo(130L));
		Assert.assertThat(transaction.getTimeStamp(), IsEqual.equalTo(MockTransaction.TIMESTAMP));
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(0));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(7));
    }

    //endregion

    //region Fees

    @Test
    public void feeIsMaximumOfMinimumFeeAndCurrentFee() {
        // Assert:
        Assert.assertThat(getFee(15L, 50L), IsEqual.equalTo(50L));
        Assert.assertThat(getFee(130L, 50L), IsEqual.equalTo(130L));
    }

    private long getFee(long minimumFee, long fee) {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account signer = new Account(publicPrivateKeyPair);

        // Act:
        final MockTransaction transaction = new MockTransaction(signer);
        transaction.setMinimumFee(minimumFee);
        transaction.setFee(fee);
        return transaction.getFee();
    }

    //endregion

    private MockTransaction createRoundTrippedTransaction(
        Transaction originalTransaction,
        final Account deserializedSigner) {
        // Act:
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalTransaction, deserializedSigner);
        return new MockTransaction(deserializer);
    }
}