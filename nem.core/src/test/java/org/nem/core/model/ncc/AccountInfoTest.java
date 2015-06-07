package org.nem.core.model.ncc;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

public class AccountInfoTest {

	//region constructor

	@Test
	public void infoCanBeCreatedWithoutPublicKey() {
		// Act:
		final AccountInfo info = new AccountInfo(
				Address.fromEncoded("test"),
				Amount.fromNem(1234),
				Amount.fromNem(1222),
				new BlockAmount(7),
				"my account",
				2.3);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(Address.fromEncoded("test")));
		Assert.assertThat(info.getKeyPair(), IsNull.nullValue());
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(1222)));
		Assert.assertThat(info.getNumHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
		Assert.assertThat(info.getMultisigInfo(), IsNull.nullValue());
	}

	@Test
	public void infoCanBeCreatedWithPublicKey() {
		// Arrange:
		final Address address = Utils.generateRandomAddressWithPublicKey();

		// Act:
		final AccountInfo info = new AccountInfo(
				address,
				Amount.fromNem(1234),
				Amount.fromNem(1222),
				new BlockAmount(7),
				"my account",
				2.3);

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(address.getPublicKey()));
		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(info.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(1222)));
		Assert.assertThat(info.getNumHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
		Assert.assertThat(info.getMultisigInfo(), IsNull.nullValue());
	}

	@Test
	public void infoCanBeCreatedWithMultisigInfo() {
		// Arrange:
		final MultisigInfo multisigInfo = new MultisigInfo(5, 8);

		// Act:
		final AccountInfo info = new AccountInfo(
				Address.fromEncoded("test"),
				Amount.fromNem(1234),
				Amount.fromNem(1222),
				new BlockAmount(7),
				"my account",
				2.3,
				multisigInfo);

		// Assert:
		Assert.assertThat(info.getMultisigInfo(), IsEqual.equalTo(multisigInfo));
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
	public void accountWithMultisigInfoCanBeBeRoundTripped() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountInfo originalInfo = new AccountInfo(
				address,
				Amount.fromNem(747),
				Amount.fromNem(727),
				new BlockAmount(3),
				"alpha gamma",
				2.3,
				new MultisigInfo(8, 11));

		// Act:
		final AccountInfo info = new AccountInfo(Utils.roundtripSerializableEntity(originalInfo, null));

		// Assert:
		Assert.assertThat(info.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(info.getMultisigInfo(), IsNull.notNullValue());
		Assert.assertThat(info.getMultisigInfo().getCosignatoriesCount(), IsEqual.equalTo(8));
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
			Assert.assertThat(info.getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		}

		Assert.assertThat(info.getBalance(), IsEqual.equalTo(Amount.fromNem(747L)));
		Assert.assertThat(info.getVestedBalance(), IsEqual.equalTo(Amount.fromNem(727L)));
		Assert.assertThat(info.getNumHarvestedBlocks(), IsEqual.equalTo(new BlockAmount(3L)));
		Assert.assertThat(info.getLabel(), IsEqual.equalTo("alpha gamma"));

		Assert.assertThat(info.getImportance(), IsEqual.equalTo(2.3));
		Assert.assertThat(info.getMultisigInfo(), IsNull.nullValue());
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
		Assert.assertThat(deserializer.readLong("vestedBalance"), IsEqual.equalTo(727000000L));
		Assert.assertThat(deserializer.readLong("harvestedBlocks"), IsEqual.equalTo(3L));
		Assert.assertThat(deserializer.readString("label"), IsEqual.equalTo("alpha gamma"));
		Assert.assertThat(deserializer.readDouble("importance"), IsEqual.equalTo(2.3));
		Assert.assertThat(deserializer.readOptionalObject("multisigInfo", MultisigInfo::new), IsNull.nullValue());

		// 8 "real" properties and 1 "hidden" (ordering) property
		final int expectedProperties = 8 + 1;
		Assert.assertThat(serializer.getObject().size(), IsEqual.equalTo(expectedProperties));
	}

	private static AccountInfo createAccountInfoForSerializationTests(final Address address) {
		// Arrange:
		return new AccountInfo(
				address,
				Amount.fromNem(747),
				Amount.fromNem(727),
				new BlockAmount(3),
				"alpha gamma",
				2.3);
	}

	//endregion

	//region equals / hashCode

	private static final Address DEFAULT_ADDRESS = Utils.generateRandomAddressWithPublicKey();

	private static final Map<String, AccountInfo> DESC_TO_INFO_MAP = new HashMap<String, AccountInfo>() {
		{
			this.put("default", createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "foo", 2.3));
			this.put("same-address-from-encoded", createAccountInfo(Address.fromEncoded(DEFAULT_ADDRESS.getEncoded()), 17, 14, 5, "foo", 2.3));
			this.put("same-address-from-public-key", createAccountInfo(Address.fromPublicKey(DEFAULT_ADDRESS.getPublicKey()), 17, 14, 5, "foo", 2.3));

			this.put("diff-address", createAccountInfo(Utils.generateRandomAddress(), 17, 14, 5, "foo", 2.3));

			this.put("diff-balance", createAccountInfo(DEFAULT_ADDRESS, 22, 14, 5, "foo", 2.3));
			this.put("diff-vested-balance", createAccountInfo(DEFAULT_ADDRESS, 17, 16, 5, "foo", 2.3));
			this.put("diff-block-amount", createAccountInfo(DEFAULT_ADDRESS, 17, 14, 9, "foo", 2.3));
			this.put("diff-label", createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "bar", 2.3));
			this.put("diff-importance", createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "foo", 3.3));
			this.put("diff-multisig-info", createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "foo", 3.3, new MultisigInfo(5, 8)));
		}
	};

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {

		// Arrange:
		final AccountInfo info = createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "foo", 2.3);

		// Assert:
		for (final Map.Entry<String, AccountInfo> entry : DESC_TO_INFO_MAP.entrySet()) {
			if ("diff-address".equals(entry.getKey())) {
				continue;
			}

			Assert.assertThat(entry.getValue(), IsEqual.equalTo(info));
		}

		Assert.assertThat(DESC_TO_INFO_MAP.get("diff-address"), IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(info)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)info)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final int hashCode = createAccountInfo(DEFAULT_ADDRESS, 17, 14, 5, "foo", 2.3).hashCode();

		// Assert:
		for (final Map.Entry<String, AccountInfo> entry : DESC_TO_INFO_MAP.entrySet()) {
			if ("diff-address".equals(entry.getKey())) {
				continue;
			}

			Assert.assertThat(entry.getValue().hashCode(), IsEqual.equalTo(hashCode));
		}

		Assert.assertThat(DESC_TO_INFO_MAP.get("diff-address").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	private static AccountInfo createAccountInfo(
			final Address address,
			final long balance,
			final long vestedBalance,
			final int blockAmount,
			final String label,
			final double importance) {
		return createAccountInfo(address, balance, vestedBalance, blockAmount, label, importance, null);
	}

	private static AccountInfo createAccountInfo(
			final Address address,
			final long balance,
			final long vestedBalance,
			final int blockAmount,
			final String label,
			final double importance,
			final MultisigInfo multisigInfo) {
		return new AccountInfo(address, Amount.fromNem(balance), Amount.fromNem(vestedBalance), new BlockAmount(blockAmount), label, importance, multisigInfo);
	}

	//endregion
}