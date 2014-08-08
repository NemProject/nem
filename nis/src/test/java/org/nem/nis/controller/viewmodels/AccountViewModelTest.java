package org.nem.nis.controller.viewmodels;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.PublicKey;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.nis.secret.AccountImportance;

import java.util.function.Function;

public class AccountViewModelTest {

	@Test
	public void viewModelCanBeCreated() {
		// Arrange:
		final AccountImportance importance = new AccountImportance();

		// Act:
		final AccountViewModel viewModel = new AccountViewModel(
				Address.fromEncoded("test"),
				Amount.fromNem(1234),
				new BlockAmount(7),
				"my account",
				importance);

		// Assert:
		Assert.assertThat(viewModel.getAddress(), IsEqual.equalTo(Address.fromEncoded("test")));
		Assert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(1234)));
		Assert.assertThat(viewModel.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(7)));
		Assert.assertThat(viewModel.getLabel(), IsEqual.equalTo("my account"));
		Assert.assertThat(viewModel.getImportanceInfo(), IsEqual.equalTo(importance));
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
		assertAccountRoundTrip(address, AccountViewModel::new, expectedPublicKey);
	}

	private static void assertAccountRoundTrip(
			final Address address,
			final Function<Deserializer, AccountViewModel> viewModelDeserializer,
			final PublicKey expectedPublicKey) {
		// Arrange:
		final AccountViewModel originalViewModel = createAccountViewModelForSerializationTests(address);

		// Act:
		final AccountViewModel viewModel = viewModelDeserializer.apply(Utils.roundtripSerializableEntity(originalViewModel, null));

		// Assert:
		Assert.assertThat(viewModel.getAddress(), IsEqual.equalTo(address));
		Assert.assertThat(viewModel.getAddress().getPublicKey(), IsEqual.equalTo(expectedPublicKey));

		if (null == expectedPublicKey) {
			Assert.assertThat(viewModel.getKeyPair(), IsNull.nullValue());
		} else {
			Assert.assertThat(viewModel.getKeyPair().hasPrivateKey(), IsEqual.equalTo(false));
			Assert.assertThat(viewModel.getKeyPair().getPublicKey(), IsEqual.equalTo(expectedPublicKey));
		}

		Assert.assertThat(viewModel.getBalance(), IsEqual.equalTo(Amount.fromNem(747L)));
		Assert.assertThat(viewModel.getNumForagedBlocks(), IsEqual.equalTo(new BlockAmount(3L)));
		Assert.assertThat(viewModel.getLabel(), IsEqual.equalTo("alpha gamma"));

		Assert.assertThat(viewModel.getImportanceInfo(), IsNull.notNullValue());
	}

	private static void assertAccountSerialization(final Address address, final byte[] expectedPublicKey) {
		// Arrange:
		final AccountViewModel originalViewModel = createAccountViewModelForSerializationTests(address);

		// Act:
		final JsonSerializer serializer = new JsonSerializer(true);
		originalViewModel.serialize(serializer);
		final JsonDeserializer deserializer = new JsonDeserializer(serializer.getObject(), null);

		// Assert:
		Assert.assertThat(deserializer.readString("address"), IsEqual.equalTo(originalViewModel.getAddress().getEncoded()));
		Assert.assertThat(deserializer.readOptionalBytes("publicKey"), IsEqual.equalTo(expectedPublicKey));
		Assert.assertThat(deserializer.readLong("balance"), IsEqual.equalTo(747000000L));
		Assert.assertThat(deserializer.readLong("foragedBlocks"), IsEqual.equalTo(3L));
		Assert.assertThat(deserializer.readString("label"), IsEqual.equalTo("alpha gamma"));

		final AccountImportance importance = deserializer.readObject("importance", obj -> new AccountImportance(obj));
		Assert.assertThat(importance.getHeight(), IsEqual.equalTo(new BlockHeight(123)));
		Assert.assertThat(importance.getImportance(importance.getHeight()), IsEqual.equalTo(0.796));

		// 6 "real" properties and 1 "hidden" (ordering) property
		final int expectedProperties = 6 + 1;
		Assert.assertThat(serializer.getObject().size(), IsEqual.equalTo(expectedProperties));
	}

	private static AccountViewModel createAccountViewModelForSerializationTests(final Address address) {
		// Arrange:
		final AccountImportance importance = new AccountImportance();
		importance.setImportance(new BlockHeight(123), 0.796);
		return new AccountViewModel(
				address,
				Amount.fromNem(747),
				new BlockAmount(3),
				"alpha gamma",
				importance);
	}

	//endregion
}