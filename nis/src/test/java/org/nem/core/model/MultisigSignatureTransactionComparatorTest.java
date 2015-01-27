package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

public class MultisigSignatureTransactionComparatorTest {

	// TODO 20140126 J-J: should use defaults
	// TODO 20140126 J-J: check behavior when multisig is different

	@Test
	public void comparingEqualTransactionsYieldsEqual() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentFeeYieldsEqual() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash);

		lhs.setFee(Amount.fromNem(12345));

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentTimeStampYieldsEqual() {
		// Arrange:
		final Account sender = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(new TimeInstant(123), sender, multisig, hash);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(new TimeInstant(987), sender, multisig, hash);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(0));
	}

	@Test
	public void comparingTransactionsWithDifferentSenderYieldsDifferent() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender1 = Utils.generateRandomAccount();
		final Account sender2 = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(timeInstant, sender1, multisig, hash);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(timeInstant, sender2, multisig, hash);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}

	@Test
	public void comparingTransactionsWithDifferentHashYieldsDifferent() {
		// Arrange:
		final TimeInstant timeInstant = new TimeInstant(123);
		final Account sender = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Hash hash1 = Utils.generateRandomHash();
		final Hash hash2 = Utils.generateRandomHash();
		final MultisigSignatureTransaction lhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash1);
		final MultisigSignatureTransaction rhs = new MultisigSignatureTransaction(timeInstant, sender, multisig, hash2);

		// Act
		final int result = compare(lhs, rhs);

		// Assert:
		Assert.assertThat(result, IsNot.not(IsEqual.equalTo(0)));
	}

	private static int compare(final MultisigSignatureTransaction lhs, final MultisigSignatureTransaction rhs) {
		return new MultisigSignatureTransactionComparator().compare(lhs, rhs);
	}
}