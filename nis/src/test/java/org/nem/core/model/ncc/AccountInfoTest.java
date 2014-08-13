package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.util.function.Function;

public class AccountInfoTest {

	@Test
	public void infoCanBeCreatedWithoutPublicKey() {
		// Act:
		final AccountInfo info = new AccountInfo(
				Address.fromEncoded("test"),
				Amount.fromNem(1234),
				new BlockAmount(7),
				"my account",
				2.3);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(Address.fromEncoded("test")));
		Assert.assertThat(info.getKeyPair(), IsNull.nullValue());
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
	}

	@Test
	public void infoCanBeCreatedWithPublicKey() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final AccountInfo info = new AccountInfo(
				address,
				Amount.fromNem(1234),
				new BlockAmount(7),
				"my account",
				2.3);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getKeyPair().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
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

		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
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
		Assert.assertThat(deserializer.readDouble("importance"), IsEqual.equalTo(2.3));

		// 6 "real" properties and 1 "hidden" (ordering) property
		final int expectedProperties = 6 + 1;
		Assert.assertThat(serializer.getObject().size(), IsEqual.equalTo(expectedProperties));
	}

	private static AccountInfo createAccountInfoForSerializationTests(final Address address) {
		// Arrange:
		return new AccountInfo(
				address,
				Amount.fromNem(747),
				new BlockAmount(3),
				"alpha gamma",
				2.3);
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {

		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountInfo info = this.createAccountInfo(address, 17, 5, "foo", 2.3);

		// Assert:
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "foo", 2.3)));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(Address.fromEncoded(address.getEncoded()), 17, 5, "foo", 2.3)));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(Address.fromPublicKey(address.getPublicKey()), 17, 5, "foo", 2.3)));

		Assert.assertThat(info, IsNot.not(IsEqual.equalTo(this.createAccountInfo(Utils.generateRandomAddress(), 17, 5, "foo", 2.3))));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(address, 22, 5, "foo", 2.3)));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(address, 17, 9, "foo", 2.3)));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "bar", 2.3)));
		Assert.assertThat(info, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "foo", 3.3)));

		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)info)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();
		final AccountInfo info = this.createAccountInfo(address, 17, 5, "foo", 2.3);
		final int hashCode = info.hashCode();

		// Assert:
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "foo", 2.3).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(Address.fromEncoded(address.getEncoded()), 17, 5, "foo", 2.3).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(Address.fromPublicKey(address.getPublicKey()), 17, 5, "foo", 2.3).hashCode()));

		Assert.assertThat(hashCode, IsNot.not(IsEqual.equalTo(this.createAccountInfo(Utils.generateRandomAddress(), 17, 5, "foo", 2.3).hashCode())));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(address, 22, 5, "foo", 2.3).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(address, 17, 9, "foo", 2.3).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "bar", 2.3).hashCode()));
		Assert.assertThat(hashCode, IsEqual.equalTo(this.createAccountInfo(address, 17, 5, "foo", 3.3).hashCode()));
	}

	private AccountInfo createAccountInfo(final Address address, final long balance, final int blockAmount, final String label, final double importance) {
		return new AccountInfo(address, Amount.fromNem(balance), new BlockAmount(blockAmount), label, importance);
	}

	//endregion
}