package org.nem.core.model;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.PlainMessage;
import org.nem.core.test.Utils;

import java.math.BigInteger;

public class AccountTest {

	//region Constructor

	@Test
	public void accountCanBeCreatedAroundKeyPair() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Address expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
		final Account account = new Account(kp);

		// Assert:
		Assert.assertThat(account.getKeyPair(), IsEqual.equalTo(kp));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithoutPublicKey() {
		// Arrange:
		final Address expectedAccountId = Utils.generateRandomAddress();
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getKeyPair(), IsEqual.equalTo(null));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithPublicKey() {
		// Arrange:
		final PublicKey publicKey = new KeyPair().getPublicKey();
		final Address expectedAccountId = Address.fromPublicKey(publicKey);
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getKeyPair().hasPrivateKey(), IsEqual.equalTo(false));
		Assert.assertThat(account.getKeyPair().getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
	}

	//endregion

	@Test
	public void labelCanBeSet() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.setLabel("Beta Gamma");

		// Assert:
		Assert.assertThat(account.getLabel(), IsEqual.equalTo("Beta Gamma"));
	}

	//region Balance

	@Test
	public void balanceCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(7));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(7)));
	}

	@Test
	public void balanceCanBeDecremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(100));
		account.decrementBalance(new Amount(12));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(88)));
	}

	@Test
	public void balanceCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementBalance(new Amount(100));
		account.decrementBalance(new Amount(12));
		account.incrementBalance(new Amount(22));
		account.decrementBalance(new Amount(25));

		// Assert:
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(85)));
	}

	//endregion

	@Test
	public void singleMessageCanBeAdded() {
		// Arrange:
		final byte[] input = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input));
	}

	@Test
	public void multipleMessagesCanBeAdded() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(2));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
		Assert.assertThat(account.getMessages().get(1).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Account account = new Account(kp);

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp))
			Assert.assertThat(account2, IsEqual.equalTo(account));

		for (final Account account2 : createNonEquivalentAccounts(kp))
			Assert.assertThat(account2, IsNot.not(IsEqual.equalTo(account)));

		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(account)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)account)));
	}

	@Test
	public void hashCodesAreOnlyEqualForEquivalentObjects() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Account account = new Account(kp);
		int hashCode = account.hashCode();

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp))
			Assert.assertThat(account2.hashCode(), IsEqual.equalTo(hashCode));

		for (final Account account2 : createNonEquivalentAccounts(kp))
			Assert.assertThat(account2.hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	private static Account[] createEquivalentAccounts(final KeyPair keyPair) {
		return new Account[] {
				new Account(keyPair),
				new Account(new KeyPair(keyPair.getPublicKey())),
				new Account(new KeyPair(keyPair.getPrivateKey()))
		};
	}

	private static Account[] createNonEquivalentAccounts(final KeyPair keyPair) {
		return new Account[] {
				Utils.generateRandomAccount(),
				new Account(new KeyPair(Utils.mutate(keyPair.getPublicKey()))),
				new Account(new KeyPair(Utils.mutate(keyPair.getPrivateKey())))
		};
	}

	//endregion
}
