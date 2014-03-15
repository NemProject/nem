package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

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
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(TimeInstant.ZERO));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(6));
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
        Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(TimeInstant.ZERO));
        Assert.assertThat(transaction.getCustomField(), IsEqual.equalTo(7));
    }

    //endregion

    // Deadline

    @Test
	public void transactionDeadlineCanBeSet() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
        transaction.setDeadline(new TimeInstant(726));

		// Assert:
		Assert.assertThat(transaction.getDeadline(), IsEqual.equalTo(new TimeInstant(726)));
	}

	@Test
	public void transactionWithDeadlineInRangeIsValid() {
		// Act:
		final MockTransaction transaction = new MockTransaction();
		transaction.setDeadline(transaction.getTimeStamp().addSeconds(726));

		// Assert:
		Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
	}

    @Test
    public void transactionWithLessThanMinimumDeadlineIsInvalid() {
        // Act:
        final MockTransaction transaction = new MockTransaction();
        transaction.setDeadline(transaction.getTimeStamp());

        // Assert:
        Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
    }

    @Test
    public void transactionWithMinimumDeadlineIsValid() {
        // Act:
        final MockTransaction transaction = new MockTransaction();
        transaction.setDeadline(transaction.getTimeStamp().addSeconds(1));

        // Assert:
        Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
    }

    @Test
    public void transactionWithMaximumDeadlineIsValid() {
        // Act:
        final MockTransaction transaction = new MockTransaction();
        transaction.setDeadline(transaction.getTimeStamp().addDays(1));

        // Assert:
        Assert.assertThat(transaction.isValid(), IsEqual.equalTo(true));
    }

    @Test
    public void transactionWithGreaterThanMaximumDeadlineIsInvalid() {
        // Act:
        final MockTransaction transaction = new MockTransaction();
        transaction.setDeadline(transaction.getTimeStamp().addDays(1).addSeconds(1));

        // Assert:
        Assert.assertThat(transaction.isValid(), IsEqual.equalTo(false));
    }

    //endregion

	//region Comparable
	@Test
	public void transactionSameAreEven() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction();
		final MockTransaction transaction2 = new MockTransaction();

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(0));
	}

	@Test
	public void transactionWithSmallerFeeIsEarlier() {
		// Arrange:
		final MockTransaction transaction1 = new MockTransaction();
		final MockTransaction transaction2 = new MockTransaction();

		transaction1.setFee(10);
		transaction2.setFee(5);

		// Assert:
		Assert.assertThat(transaction1.compareTo(transaction2), IsEqual.equalTo(1));
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