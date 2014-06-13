package org.nem.core.model;

import net.minidev.json.JSONObject;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.messages.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockAmount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.model.primitive.ReferenceCounter;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;
import java.util.Arrays;
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
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
		Assert.assertThat(account.getHeight(), IsNull.nullValue());

		Assert.assertThat(account.getImportanceInfo(), IsNull.notNullValue());
		Assert.assertThat(account.getWeightedBalances(), IsNull.notNullValue());
	}

	@Test
	public void accountCanBeCreatedAroundAddressWithoutPublicKey() {
		// Arrange:
		final Address expectedAccountId = Utils.generateRandomAddress();
		final Account account = new Account(expectedAccountId);

		// Assert:
		Assert.assertThat(account.getKeyPair(), IsNull.nullValue());
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(expectedAccountId));
		Assert.assertThat(account.getBalance(), IsEqual.equalTo(Amount.ZERO));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(BlockAmount.ZERO));
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(0));
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
        Assert.assertThat(account.getHeight(), IsNull.nullValue());

		Assert.assertThat(account.getImportanceInfo(), IsNull.notNullValue());
		Assert.assertThat(account.getWeightedBalances(), IsNull.notNullValue());
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
		Assert.assertThat(account.getLabel(), IsNull.nullValue());
        Assert.assertThat(account.getHeight(), IsNull.nullValue());

		Assert.assertThat(account.getImportanceInfo(), IsNull.notNullValue());
		Assert.assertThat(account.getWeightedBalances(), IsNull.notNullValue());
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

	//region height

	@Test
	public void accountHeightCanBeSetIfNull() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.setHeight(new BlockHeight(17));

		// Assert:
		Assert.assertThat(account.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	@Test
	public void accountHeightCannotBeUpdatedIfNonNull() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Act:
		account.setHeight(new BlockHeight(17));
		account.setHeight(new BlockHeight(32));

		// Assert:
		Assert.assertThat(account.getHeight(), IsEqual.equalTo(new BlockHeight(17)));
	}

	//endregion

	//region refCount

	@Test
	public void referenceCountIsZeroForNewAccount() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		
		// Assert:
		Assert.assertThat(account.getReferenceCounter(), IsEqual.equalTo(new ReferenceCounter(0)));
	}

	@Test
	public void referenceCounterCanBeIncremented() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		
		// Act:
		final ReferenceCounter result = account.incrementReferenceCounter();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCounter(1)));
		Assert.assertThat(account.getReferenceCounter(), IsEqual.equalTo(new ReferenceCounter(1)));
	}

	@Test
	public void referenceCounterCanBeDecrementedIfPositive() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.incrementReferenceCounter();
		
		// Act:
		final ReferenceCounter result = account.decrementReferenceCounter();

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(new ReferenceCounter(0)));
		Assert.assertThat(account.getReferenceCounter(), IsEqual.equalTo(new ReferenceCounter(0)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void referenceCounterCannotBeDecrementedIfZero() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		
		// Act:
		account.decrementReferenceCounter();
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

	@Test
	public void messageCanBeRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.removeMessage(new PlainMessage(input1));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	@Test
	public void lastMatchingMessageIsRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.addMessage(new PlainMessage(input1));
		account.addMessage(new PlainMessage(input2));
		account.removeMessage(new PlainMessage(input1));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(3));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
		Assert.assertThat(account.getMessages().get(1).getDecodedPayload(), IsEqual.equalTo(input2));
		Assert.assertThat(account.getMessages().get(2).getDecodedPayload(), IsEqual.equalTo(input2));
	}

	@Test
	public void nothingHappensIfMessageNotAssociatedWithAccountIsRemoved() {
		// Arrange:
		final byte[] input1 = Utils.generateRandomBytes();
		final byte[] input2 = Utils.generateRandomBytes();
		final Account account = new Account(new KeyPair());

		// Act:
		account.addMessage(new PlainMessage(input1));
		account.removeMessage(new PlainMessage(input2));

		// Assert:
		Assert.assertThat(account.getMessages().size(), IsEqual.equalTo(1));
		Assert.assertThat(account.getMessages().get(0).getDecodedPayload(), IsEqual.equalTo(input1));
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
	public void hashCodesAreEqualForEquivalentObjects() {
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

	@Test
	public void accountWithPublicKeyCanBeRoundTripped() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Assert:
		assertAccountRoundTrip(address, address.getPublicKey());
	}

	@Test
	public void accountWithoutPublicKeyCanBeRoundTripped() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();

		// Assert:
		assertAccountRoundTrip(address, null);
	}

	@Test
	public void canRoundTripUnsetAccountImportance() {
		// Arrange:
		final Account original = Utils.generateRandomAccount();

		// Act:
		final Account account = new Account(Utils.roundtripSerializableEntity(original, null));

		// Assert:
		Assert.assertThat(account.getImportanceInfo().isSet(), IsEqual.equalTo(false));
	}

	@Test
	public void secureMessagesCanBeRoundTrippedWithPrivateKey() {
		// Arrange: create 3 secure messages from three different senders
		final List<Account> senders = Arrays.asList(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
		final Account recipient = Utils.generateRandomAccount();

		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(0), recipient, new byte[] { 1, 1, 1 }));
		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(1), recipient, new byte[] { 2, 4, 8 }));
		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(2), recipient, new byte[] { 3, 9, 27 }));

		// add the recipient (private key) and senders (public key only)
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(recipient);
		senders.stream().forEach(s -> accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(s)));

		// Act:
		final Account roundTrippedRecipient = new Account(Utils.roundtripSerializableEntity(recipient, accountLookup));

		// Assert:
		final List<Message> messages = roundTrippedRecipient.getMessages();
		Assert.assertThat(messages.size(), IsEqual.equalTo(3));
		Assert.assertThat(messages.get(0).getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 1, 1 }));
		Assert.assertThat(messages.get(1).getDecodedPayload(), IsEqual.equalTo(new byte[] { 2, 4, 8 }));
		Assert.assertThat(messages.get(2).getDecodedPayload(), IsEqual.equalTo(new byte[] { 3, 9, 27 }));
	}

	@Test
	public void secureMessagesRequireAtLeastOnePrivateKeyToBeRoundTripped() {
		// Arrange: create 3 secure messages from three different senders
		final List<Account> senders = Arrays.asList(
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount());
		final Account recipient = Utils.generateRandomAccount();

		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(0), recipient, new byte[] { 1, 1, 1 }));
		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(1), recipient, new byte[] { 2, 4, 8 }));
		recipient.addMessage(SecureMessage.fromDecodedPayload(senders.get(2), recipient, new byte[] { 3, 9, 27 }));

		// add the recipient (public key only) and senders ([0, 2] - public key only, [1] - private key)
		final MockAccountLookup accountLookup = new MockAccountLookup();
		accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(recipient));
		senders.stream().forEach(s -> accountLookup.setMockAccount(Utils.createPublicOnlyKeyAccount(s)));
		accountLookup.setMockAccount(senders.get(1));

		// Act:
		final Account roundTrippedRecipient = new Account(Utils.roundtripSerializableEntity(recipient, accountLookup));

		// Assert:
		final List<Message> messages = roundTrippedRecipient.getMessages();
		Assert.assertThat(messages.size(), IsEqual.equalTo(3));
		Assert.assertThat(messages.get(0).getDecodedPayload(), IsEqual.equalTo(null));
		Assert.assertThat(messages.get(1).getDecodedPayload(), IsEqual.equalTo(new byte[] { 2, 4, 8 }));
		Assert.assertThat(messages.get(2).getDecodedPayload(), IsEqual.equalTo(null));
	}

	private static void assertAccountRoundTrip(final Address address, final PublicKey expectedPublicKey) {
		// Arrange:
		final Account originalAccount = createAccountForSerializationTests(address);

		// Act:
		final Account account = new Account(Utils.roundtripSerializableEntity(originalAccount, null));

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(account.getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));

		if (null == expectedPublicKey) {
			Assert.assertThat(account.getKeyPair(), IsNull.nullValue());
		} else {
			Assert.assertThat(account.getKeyPair().hasPrivateKey(), IsEqual.equalTo(false));
			Assert.assertThat(account.getKeyPair().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		}

		Assert.assertThat(account.getBalance(), IsEqual.equalTo(new Amount(747L)));
		Assert.assertThat(account.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3L)));
		Assert.assertThat(account.getLabel(), IsEqual.equalTo("alpha gamma"));

		final List<Message> messages = account.getMessages();
		Assert.assertThat(messages.size(), IsEqual.equalTo(2));
		Assert.assertThat(messages.get(0).getDecodedPayload(), IsEqual.equalTo(new byte[] { 1, 4, 5 }));
		Assert.assertThat(messages.get(1).getDecodedPayload(), IsEqual.equalTo(new byte[] { 8, 12, 4 }));

		Assert.assertThat(account.getImportanceInfo(), IsNull.notNullValue());
		Assert.assertThat(account.getWeightedBalances(), IsNull.notNullValue());
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

		final AccountImportance importance = deserializer.readObject("importance", AccountImportance::new);
		Assert.assertThat(importance.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(importance.getImportance(importance.getHeight()), IsEqual.equalTo(0.796));

		final List<Message> messages = deserializer.readObjectArray("messages", MessageFactory.DESERIALIZER);
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
		account.getImportanceInfo().setImportance(new BlockHeight(123), 0.796);
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

	//region copy

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithoutPublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddress());

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.nullValue());
		Assert.assertThat(copyAccount.getKeyPair(), IsNull.nullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithPublicKey() {
		// Arrange:
		final Account account = new Account(Utils.generateRandomAddressWithPublicKey());

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPrivateKey(), IsNull.nullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountWithPrivateKey() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();

		// Assert:
		final Account copyAccount = assertCopyCreatesUnlinkedAccount(account);
		Assert.assertThat(copyAccount.getAddress().getEncoded(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPublicKey(), IsNull.notNullValue());
		Assert.assertThat(copyAccount.getKeyPair().getPrivateKey(), IsNull.notNullValue());
	}

	@Test
	public void copyCreatesUnlinkedCopyOfMessages() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		account.addMessage(new PlainMessage(new byte[] { 1, 2, 3 }));
		account.addMessage(new PlainMessage(new byte[] { 7, 9, 8 }));

		// Act:
		final Account copyAccount = account.copy();

		// Assert:
		Assert.assertThat(copyAccount.getMessages(), IsNot.not(IsSame.sameInstance(account.getMessages())));
		Assert.assertThat(copyAccount.getMessages().size(), IsEqual.equalTo(2));
		Assert.assertThat(getEncodedMessageAt(copyAccount, 0), IsEqual.equalTo(new byte[] { 1, 2, 3 }));
		Assert.assertThat(getEncodedMessageAt(copyAccount, 1), IsEqual.equalTo(new byte[] { 7, 9, 8 }));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfWeightedBalances() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final WeightedBalances balances = account.getWeightedBalances();
		balances.addReceive(new BlockHeight(17), Amount.fromNem(1234));

		// Act:
		final Account copyAccount = account.copy();
		final WeightedBalances copyBalances = copyAccount.getWeightedBalances();

		// Assert:
		Assert.assertThat(copyBalances, IsNot.not(IsSame.sameInstance(balances)));
		Assert.assertThat(copyBalances.getUnvested(new BlockHeight(17)), IsEqual.equalTo(Amount.fromNem(1234)));
	}

	@Test
	public void copyCreatesUnlinkedCopyOfAccountImportance() {
		// Arrange:
		final Account account = Utils.generateRandomAccount();
		final AccountImportance importance = account.getImportanceInfo();
		importance.setImportance(BlockHeight.ONE, 0.03125);
		importance.addOutlink(new AccountLink(BlockHeight.ONE, Amount.fromNem(12), Utils.generateRandomAddress()));

		// Act:
		final Account copyAccount = account.copy();
		final AccountImportance copyImportance = copyAccount.getImportanceInfo();
		copyAccount.getImportanceInfo().setImportance(new BlockHeight(2), 0.0234375);

		// Assert:
		Assert.assertThat(copyImportance, IsNot.not(IsSame.sameInstance(importance)));
		Assert.assertThat(importance.getImportance(BlockHeight.ONE), IsEqual.equalTo(0.03125));
		Assert.assertThat(copyImportance.getImportance(new BlockHeight(2)), IsEqual.equalTo(0.0234375));
		Assert.assertThat(copyImportance.getOutlinksSize(BlockHeight.ONE), IsEqual.equalTo(1));
		Assert.assertThat(
				copyImportance.getOutlinksIterator(BlockHeight.ONE).next(),
				IsEqual.equalTo(importance.getOutlinksIterator(BlockHeight.ONE).next()));
	}

	public static Account assertCopyCreatesUnlinkedAccount(final Account account) {
		// Arrange:
		setAccountValuesForCopyTests(account);

		// Act:
		final Account copyAccount = account.copy();

		// Assert:
		Assert.assertThat(copyAccount.getAddress(), IsEqual.equalTo(account.getAddress()));
		Assert.assertThat(copyAccount.getAddress().getPublicKey(), IsEqual.equalTo(account.getAddress().getPublicKey()));
		assertKeyPairsAreEquivalent(copyAccount.getKeyPair(), account.getKeyPair());

		Assert.assertThat(copyAccount.getBalance(), IsEqual.equalTo(new Amount(1000)));
		Assert.assertThat(copyAccount.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(copyAccount.getForagedBlocks(), IsEqual.equalTo(new BlockAmount(3)));
		Assert.assertThat(copyAccount.getLabel(), IsEqual.equalTo("Alpha Sigma"));

		// verify that the mutable objects are not the same
		Assert.assertThat(copyAccount.getMessages(), IsNot.not(IsSame.sameInstance(account.getMessages())));
		Assert.assertThat(copyAccount.getWeightedBalances(), IsNot.not(IsSame.sameInstance(account.getWeightedBalances())));
		Assert.assertThat(copyAccount.getImportanceInfo(), IsNot.not(IsSame.sameInstance(account.getImportanceInfo())));
		return copyAccount;
	}

	private static void setAccountValuesForCopyTests(final Account account) {
		account.incrementBalance(new Amount(1000));
		account.setHeight(new BlockHeight(123));
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.incrementForagedBlocks();
		account.setLabel("Alpha Sigma");
		account.addMessage(new PlainMessage(new byte[] { 1, 2, 3 }));
		account.addMessage(new PlainMessage(new byte[] { 7, 9, 8 }));
	}

	private static void assertKeyPairsAreEquivalent(final KeyPair actual, final KeyPair expected) {
		if (null == actual || null == expected) {
			Assert.assertThat(actual, IsEqual.equalTo(expected));
		}
		else {
			Assert.assertThat(actual.getPublicKey(), IsEqual.equalTo(expected.getPublicKey()));
			Assert.assertThat(actual.getPrivateKey(), IsEqual.equalTo(expected.getPrivateKey()));
		}
	}

	private static byte[] getEncodedMessageAt(final Account account, final int index) {
		return account.getMessages().get(index).getEncodedPayload();
	}

	//endregion

}
