package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.math.BigInteger;

public class AccountTest {

	//region constructor

	@Test
	public void accountCanBeCreatedAroundKeyPairWithPrivateKey() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Address expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
		final Account account = new Account(kp);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getAddress().getPublicKey(), IsEqual.equalTo(kp.getPublicKey()));
		Assert.assertThat(account.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.hasPrivateKey(), IsEqual.equalTo(true));
	}

	@Test
	public void accountCanBeCreatedAroundKeyPairWithoutPrivateKey() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Address expectedAccountId = Address.fromPublicKey(kp.getPublicKey());
		final Account account = new Account(new KeyPair(kp.getPublicKey()));

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getAddress().getPublicKey(), IsEqual.equalTo(kp.getPublicKey()));
		Assert.assertThat(account.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.hasPrivateKey(), IsEqual.equalTo(false));
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithPublicKey() {
		// Arrange:
		final PublicKey publicKey = new KeyPair().getPublicKey();
		final Address expectedAccountId = Address.fromPublicKey(publicKey);
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getAddress().getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(account.hasPublicKey(), IsEqual.equalTo(true));
		Assert.assertThat(account.hasPrivateKey(), IsEqual.equalTo(false));
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithoutPublicKey() {
		// Arrange:
		final Address expectedAccountId = Utils.generateRandomAddress();
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getAddress().getPublicKey(), IsNull.nullValue());
		Assert.assertThat(account.hasPublicKey(), IsEqual.equalTo(false));
		Assert.assertThat(account.hasPrivateKey(), IsEqual.equalTo(false));
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Account account = new Account(kp);

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp)) {
			Assert.assertThat(account2, IsEqual.equalTo(account));
		}

		for (final Account account2 : createNonEquivalentAccounts(kp)) {
			Assert.assertThat(account2, IsNot.not(IsEqual.equalTo(account)));
		}

		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(account)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)account)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final KeyPair kp = new KeyPair();
		final Account account = new Account(kp);
		final int hashCode = account.hashCode();

		// Assert:
		for (final Account account2 : createEquivalentAccounts(kp)) {
			Assert.assertThat(account2.hashCode(), IsEqual.equalTo(hashCode));
		}

		for (final Account account2 : createNonEquivalentAccounts(kp)) {
			Assert.assertThat(account2.hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		}
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

	//region toString

	@Test
	public void toStringReturnsEncodedAddress() {
		// Arrange:
		final Account account = new Account(Address.fromEncoded("Sigma Gamma"));

		// Assert:
		Assert.assertThat(account.toString(), IsEqual.equalTo("SIGMA GAMMA"));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAccountWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		Account.writeTo(serializer, "Account", new Account(address));

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Account"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canWriteAccountWithAddressEncoding() {
		// Arrange:
		final Address address = Address.fromEncoded("MockAcc");

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(address),
				AddressEncoding.COMPRESSED,
				address.getEncoded());
	}

	@Test
	public void canWriteAccountWithPublicKeyEncoding() {
		// Arrange:
		final KeyPair kp = new KeyPair();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(kp),
				AddressEncoding.PUBLIC_KEY,
				kp.getPublicKey().toString());
	}

	@Test
	public void canWriteAccountThatDoesNotHavePublicKeyWithPublicKeyEncoding() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(address),
				AddressEncoding.PUBLIC_KEY,
				null);
	}

	private static void assertCanWriteAccountWithEncoding(
			final Account account,
			final AddressEncoding encoding,
			final String expectedSerializedString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		Account.writeTo(serializer, "Account", account, encoding);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat(object.get("Account"), IsEqual.equalTo(expectedSerializedString));
	}

	@Test
	public void canRoundtripAccountWithDefaultEncoding() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		Account.writeTo(serializer, "Account", new Account(address));

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = Account.readFrom(deserializer, "Account");

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	@Test
	public void canRoundtripAccountWithAddressEncoding() {
		// Assert:
		this.assertAccountRoundTripInMode(AddressEncoding.COMPRESSED);
	}

	@Test
	public void canRoundtripAccountWithPublicKeyEncoding() {
		// Assert:
		this.assertAccountRoundTripInMode(AddressEncoding.PUBLIC_KEY);
	}

	private void assertAccountRoundTripInMode(final AddressEncoding encoding) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Account originalAccount = Utils.generateRandomAccountWithoutPrivateKey();
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		Account.writeTo(serializer, "Account", originalAccount, encoding);

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = Account.readFrom(deserializer, "Account", encoding);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(originalAccount.getAddress()));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	//endregion

	//region createSigner

	@Test
	public void cannotCreateSignerWhenAccountDoesNotHavePublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddress());

		// Act:
		ExceptionAssert.assertThrows(
				v -> account.createSigner(),
				CryptoException.class);
	}

	@Test
	public void canCreateSignerWhenAccountHasPublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddressWithPublicKey());

		// Act:
		final Signer signer = account.createSigner();

		// Assert:
		Assert.assertThat(signer, IsNull.notNullValue());
	}

	@Test
	public void canSignAndVerifyDataWithSigner() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final Account account = new Account(keyPair);
		final Account accountWithOnlyPublicKey = new Account(new KeyPair(keyPair.getPublicKey()));
		final byte[] payload = Utils.generateRandomBytes();

		// Act:
		final Signer signer = account.createSigner();
		final Signature signature = signer.sign(payload);
		final Signer verifier = accountWithOnlyPublicKey.createSigner();
		final boolean isVerified = verifier.verify(payload, signature);

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(true));
	}

	//endregion

	//region createCipher

	@Test
	public void cannotCreateCipherIfNeitherAccountHasPrivateKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddressWithPublicKey());
		final Account otherAccount = new Account(Utils.generateRandomAddressWithPublicKey());

		// Act:
		ExceptionAssert.assertThrows(
				v -> account.createCipher(otherAccount, false),
				CryptoException.class);
	}

	@Test
	public void cannotCreateCipherIfAnyAccountDoesNotHavePublicKey() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final Account otherAccount = new Account(Utils.generateRandomAddress());

		// Act:
		ExceptionAssert.assertThrows(
				v -> account.createCipher(otherAccount, false),
				CryptoException.class);
		ExceptionAssert.assertThrows(
				v -> otherAccount.createCipher(account, false),
				CryptoException.class);
	}

	@Test
	public void canEncryptAndDecryptDataWithSignerWhenFirstAccountHasPrivateKey() {
		// Assert:
		assertCanEncryptAndDecrypt(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccountWithoutPrivateKey());
	}

	@Test
	public void canEncryptAndDecryptDataWithSignerWhenSecondAccountHasPrivateKey() {
		// Assert:
		assertCanEncryptAndDecrypt(
				Utils.generateRandomAccountWithoutPrivateKey(),
				Utils.generateRandomAccount());
	}

	@Test
	public void canEncryptAndDecryptDataWithSignerWhenBothAccountsHavePrivateKey() {
		// Assert:
		assertCanEncryptAndDecrypt(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
	}

	private static void assertCanEncryptAndDecrypt(final Account account1, final Account account2) {
		// Arrange:
		final byte[] payload = Utils.generateRandomBytes();

		// Act:
		final Cipher encryptCipher = account1.createCipher(account2, true);
		final byte[] encryptedPayload = encryptCipher.encrypt(payload);
		final Cipher decryptCipher = account1.createCipher(account2, false);
		final byte[] decryptedPayload = decryptCipher.decrypt(encryptedPayload);

		// Assert:
		Assert.assertThat(decryptedPayload, IsEqual.equalTo(payload));
	}

	//endregion
}
