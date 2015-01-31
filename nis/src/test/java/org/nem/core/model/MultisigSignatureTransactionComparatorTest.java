package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class MultisigSignatureTransactionComparatorTest {
	private static final TimeInstant TIME_INSTANT = new TimeInstant(123);
	private static final Account SENDER = Utils.generateRandomAccount();
	private static final Account MULTISIG = Utils.generateRandomAccount();
	private static final Hash HASH = Utils.generateRandomHash();

	@Test
	public void comparingEqualTransactionsYieldsEqual() {
		// Arrange:
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, HASH);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, HASH);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentFeeYieldsEqual() {
		// Arrange:
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, HASH);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, HASH);
		lhs.setFee(Amount.fromNem(12345));
		rhs.setFee(Amount.fromNem(44444));

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentTimeStampYieldsEqual() {
		// Arrange:
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(new TimeInstant(123), SENDER, MULTISIG, HASH);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(new TimeInstant(987), SENDER, MULTISIG, HASH);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentMultisigYieldsEqual() {
		// Arrange:
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, Utils.generateRandomAccount(), HASH);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, Utils.generateRandomAccount(), HASH);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentSenderYieldsDifferent() {
		// Arrange:
		final Account sender1 = Utils.generateRandomAccount();
		final Account sender2 = Utils.generateRandomAccount();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(TIME_INSTANT, sender1, MULTISIG, HASH);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(TIME_INSTANT, sender2, MULTISIG, HASH);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}

	@Test
	public void comparingTransactionsWithDifferentHashYieldsDifferent() {
		// Arrange:
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, hash1);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(TIME_INSTANT, SENDER, MULTISIG, hash2);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}

	private static int compare(final MultisigSignatureTransaction lhs, final MultisigSignatureTransaction rhs) {
		return new MultisigSignatureTransactionComparator().compare(lhs, rhs);
	}
}