package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.secret.AccountImportance;

import java.util.function.Function;

public class AccountInfoTest {

	@Test
	public void infoCanBeCreatedWithoutPublicKey() {
		// Arrange:
		final AccountImportance importance = new AccountImportance();

		// Act:
		final AccountInfo info = new AccountInfo(
				Address.fromEncoded("test"),
				Amount.fromNem(1234),
				new BlockAmount(7),
				"my account",
				importance);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(Address.fromEncoded("test")));
		Assert.assertThat(info.getKeyPair(), IsNull.nullValue());
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportanceInfo(), IsEqual.equalTo(importance));
	}

	@Test
	public void infoCanBeCreatedWithPublicKey() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountImportance importance = new AccountImportance();

		// Act:
		final AccountInfo info = new AccountInfo(
				address,
				Amount.fromNem(1234),
				new BlockAmount(7),
				"my account",
				importance);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportanceInfo(), IsEqual.equalTo(importance));
	}


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

	private static void assertAccountRoundTrip(final Address address, final PublicKey expectedPublicKey) {
		// Assert:
		assertAccountRoundTrip(address, AccountInfo::new, expectedPublicKey);
	}

	private static void assertAccountRoundTrip(
			final Address address,
			final Function<Deserializer, AccountInfo> infoDeserializer,
			final PublicKey expectedPublicKey) {
		// Arrange:
		final AccountInfo originalInfo = createAccountInfoForSerializationTests(address);

		// Act:
		final AccountInfo info = infoDeserializer.apply(Utils.roundtripSerializableEntity(originalInfo, null));

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));

		if (null == expectedPublicKey) {
			Assert.assertThat(info.getKeyPair(), IsNull.nullValue());
		} else {
			Assert.assertThat(info.getKeyPair().hasPrivateKey(), IsEqual.equalTo(false));
			Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		}

		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(747L)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(3L)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));

		Assert.assertThat(info.getImportanceInfo(), IsNull.notNullValue());
	}

	private static void assertAccountSerialization(final Address address, final byte[] expectedPublicKey) {
		// Arrange:
		final AccountInfo originalInfo = createAccountInfoForSerializationTests(address);

		// Act:
		final JsonSerializer serializer = new JsonSerializer(true);
		originalInfo.serialize(serializer);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Assert:
		Assert.assertThat(deserializer.readString("address"), IsEqual.equalTo(originalInfo.getAddress().getEncoded()));
		Assert.assertThat(deserializer.readOptionalBytes("publicKey"), IsEqual.equalTo(expectedPublicKey));
		Assert.assertThat(deserializer.readLong("balance"), IsEqual.equalTo(747000000L));
		Assert.assertThat(deserializer.readLong("foragedBlocks"), IsEqual.equalTo(3L));
		Assert.assertThat(deserializer.readString("label"), IsEqual.equalTo("alpha gamma"));

		final AccountImportance importance = deserializer.readObject("importance", AccountImportance::new);
		Assert.assertThat(importance.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(importance.getImportance(importance.getHeight()), IsEqual.equalTo(0.796));

		// 6 "real" properties and 1 "hidden" (ordering) property
		final int expectedProperties = 6 + 1;
		Assert.assertThat(serializer.getObject().size(), IsEqual.equalTo(expectedProperties));
	}

	private static AccountInfo createAccountInfoForSerializationTests(final Address address) {
		// Arrange:
		final AccountImportance importance = new AccountImportance();
		importance.setImportance(new BlockHeight(123), 0.796);
		return new AccountInfo(
				address,
				Amount.fromNem(747),
				new BlockAmount(3),
				"alpha gamma",
				importance);
	}

	//endregion
}