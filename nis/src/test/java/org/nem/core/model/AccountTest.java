package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;
import java.util.List;

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
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
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
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
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
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo(null));
	}

	//endregion

	//region Label

	@Test
	public void labelCanBeSet() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.setLabel("Beta Gamma");

		// Assert:
		Assert.assertThat(account.getLabel(), IsEqual.equalTo("Beta Gamma"));
	}

	//endregion

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

	//region foraged blocks

	@Test
	public void foragedBlocksCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	@Test
	public void foragedBlocksCanBeDecremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(1)));
	}

	@Test
	public void foragedBlocksCanBeIncrementedAndDecrementedMultipleTimes() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.decrementForagedBlocks();

		// Assert:
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(2)));
	}

	//endregion

	//region Message

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

	//endregion

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

	//region Serialization

	@Test
	public void accountWithPublicKeyCanBeSerialized() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		assertAccountSerialization(address, address.getPublicKey().getRaw());
	}

	@Test
	public void accountWithoutPublicKeyCanBeSerialized() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		assertAccountSerialization(address, null);
	}

	private static void assertAccountSerialization(final Address address, final byte[] expectedPublicKey) {
		// Arrange:
		final Account originalAccount = createAccountForSerializationTests(address);

		// Act:
		final JsonDeserializer deserializer = serializeAccountAndCreateDeserializer(originalAccount);

		// Assert:
		Assert.assertThat(deserializer.readString("address"), IsEqual.equalTo(originalAccount.getAddress().getEncoded()));
		Assert.assertThat(deserializer.readBytes("publicKey"), IsEqual.equalTo(expectedPublicKey));
		Assert.assertThat(deserializer.readLong("balance"), IsEqual.equalTo(747L));
		Assert.assertThat(deserializer.readLong("foragedBlocks"), IsEqual.equalTo(3L));
		Assert.assertThat(deserializer.readString("label"), IsEqual.equalTo("alpha gamma"));

		final List<Message> messages = deserializer.readObjectArray("messages", MessageFactory.createDeserializer(null, null));
		Assert.assertThat(messages.size(), IsEqual.equalTo(2));
		Assert.assertThat(messages.get(0).getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 4, 5 }));
		Assert.assertThat(messages.get(1).getDecodedPayload(), IsEqual.equalTo(new byte[] { 8, 12, 4 }));
	}

	private static Account createAccountForSerializationTests(final Address address) {
		// Arrange:
		final Account account = new Account(address);
		account.setLabel("alpha gamma");
		account.incrementBalance(new Amount(747));
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.addMessage(new PlainMessage(new byte[] { 1, 4, 5 }));
		account.addMessage(new PlainMessage(new byte[] { 8, 12, 4 }));
		return account;
	}

	private static JsonDeserializer serializeAccountAndCreateDeserializer(final Account account) {
		// Act:
		final JsonSerializer serializer = new JsonSerializer(true);
		account.serialize(serializer);
		return new JsonDeserializer(serializer.getObject(), null);
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteAccount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		Account.writeTo(serializer, "Account", new MockAccount(address));

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((String)object.get("Account"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canWriteAccountWithAddressEncoding() {
		// Arrange:
		final Address address = Address.fromEncoded("MockAcc");

		// Assert:
		assertCanWriteAccountWithEncoding(
				new MockAccount(address),
				AccountEncoding.ADDRESS,
				address.getEncoded());
	}

	@Test
	public void canWriteAccountWithPublicKeyEncoding() {
		// Arrange:
		final KeyPair kp = new KeyPair();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(kp),
				AccountEncoding.PUBLIC_KEY,
				Base64Encoder.getString(kp.getPublicKey().getRaw()));
	}

	@Test
	public void canWriteAccountThatDoesNotHavePublicKeyWithPublicKeyEncoding() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		assertCanWriteAccountWithEncoding(
				new Account(address),
				AccountEncoding.PUBLIC_KEY,
				null);
	}

	private static void assertCanWriteAccountWithEncoding(
			final Account account,
			final AccountEncoding encoding,
			final String expectedSerializedString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		Account.writeTo(serializer, "Account", account, encoding);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((String)object.get("Account"), IsEqual.equalTo(expectedSerializedString));
	}

	@Test
	public void canRoundtripAccount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");
		final MockAccountLookup accountLookup = new MockAccountLookup();

		// Act:
		Account.writeTo(serializer, "Account", new MockAccount(address));

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
		assertAccountRoundTripInMode(AccountEncoding.ADDRESS);
	}

	@Test
	public void canRoundtripAccountWithPublicKeyEncoding() {
		// Assert:
		assertAccountRoundTripInMode(AccountEncoding.PUBLIC_KEY);
	}

	private void assertAccountRoundTripInMode(final AccountEncoding encoding) {
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
}
