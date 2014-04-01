package org.nem.core.serialization;

import net.minidev.json.JSONObject;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;

/**
 * Notice that this the write* tests are validating that the values were actually written to the
 * underlying serializer (JSON in this case) and are thus dependent on the storage details of the
 * JsonSerializer. Although the tests have this extra dependency, they give us a way to validate
 * that a single property value is written for each object.
 */
public class SerializationUtilsTest {

	//region Address

	@Test
	public void canWriteAddress() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		SerializationUtils.writeAddress(serializer, "Address", address);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((String)object.get("Address"), IsEqual.equalTo(address.getEncoded()));
	}

	@Test
	public void canRoundtripAddress() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		SerializationUtils.writeAddress(serializer, "Address", Address.fromEncoded("MockAcc"));

		final JsonDeserializer deserializer = createDeserializer(serializer.getObject());
		final Address address = SerializationUtils.readAddress(deserializer, "Address");

		// Assert:
		Assert.assertThat(address, IsEqual.equalTo(address));
	}

	//endregion

	//region Account

	@Test
	public void canWriteAccount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Address address = Address.fromEncoded("MockAcc");

		// Act:
		SerializationUtils.writeAccount(serializer, "Account", new MockAccount(address));

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

	private static void assertCanWriteAccountWithEncoding(
			final Account account,
			final AccountEncoding encoding,
			final String expectedSerializedString) {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		SerializationUtils.writeAccount(serializer, "Account", account, encoding);

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
		SerializationUtils.writeAccount(serializer, "Account", new MockAccount(address));

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = SerializationUtils.readAccount(deserializer, "Account");

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
		SerializationUtils.writeAccount(serializer, "Account", originalAccount, encoding);

		final JsonDeserializer deserializer = new JsonDeserializer(
				serializer.getObject(),
				new DeserializationContext(accountLookup));
		final Account account = SerializationUtils.readAccount(deserializer, "Account", encoding);

		// Assert:
		Assert.assertThat(account.getAddress(), IsEqual.equalTo(originalAccount.getAddress()));
		Assert.assertThat(accountLookup.getNumFindByIdCalls(), IsEqual.equalTo(1));
	}

	//endregion

	//region Signature

	@Test
	public void canWriteSignature() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Signature signature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

		// Act:
		SerializationUtils.writeSignature(serializer, "Signature", signature);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((String)object.get("Signature"), IsEqual.equalTo(Base64Encoder.getString(signature.getBytes())));
	}

	@Test
	public void canRoundtripSignature() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Signature originalSignature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

		// Act:
		SerializationUtils.writeSignature(serializer, "Signature", originalSignature);

		final JsonDeserializer deserializer = createDeserializer(serializer.getObject());
		final Signature signature = SerializationUtils.readSignature(deserializer, "Signature");

		// Assert:
		Assert.assertThat(signature, IsEqual.equalTo(originalSignature));
	}

	//endregion

	//region TimeInstant

	@Test
	public void canWriteTimeInstant() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TimeInstant instant = new TimeInstant(77124);

		// Act:
		SerializationUtils.writeTimeInstant(serializer, "TimeInstant", instant);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((Integer)object.get("TimeInstant"), IsEqual.equalTo(77124));
	}

	@Test
	public void canRoundtripTimeInstant() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final TimeInstant originalInstant = new TimeInstant(77124);

		// Act:
		SerializationUtils.writeTimeInstant(serializer, "TimeInstant", originalInstant);

		final JsonDeserializer deserializer = createDeserializer(serializer.getObject());
		final TimeInstant instant = SerializationUtils.readTimeInstant(deserializer, "TimeInstant");

		// Assert:
		Assert.assertThat(instant, IsEqual.equalTo(originalInstant));
	}

	//endregion

	//region Amount

	@Test
	public void canWriteAmount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Amount amount = new Amount(0x7712411223456L);

		// Act:
		SerializationUtils.writeAmount(serializer, "Amount", amount);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((Long)object.get("Amount"), IsEqual.equalTo(0x7712411223456L));
	}

	@Test
	public void canRoundtripAmount() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Amount originalAmount = new Amount(0x7712411223456L);

		// Act:
		SerializationUtils.writeAmount(serializer, "Amount", originalAmount);

		final JsonDeserializer deserializer = createDeserializer(serializer.getObject());
		final Amount amount = SerializationUtils.readAmount(deserializer, "Amount");

		// Assert:
		Assert.assertThat(amount, IsEqual.equalTo(originalAmount));
	}

	//endregion

	private JsonDeserializer createDeserializer(final JSONObject object) {
		return new JsonDeserializer(
				object,
				new DeserializationContext(new MockAccountLookup()));
	}
}
