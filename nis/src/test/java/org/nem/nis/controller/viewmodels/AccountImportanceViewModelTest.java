package org.nem.nis.controller.viewmodels;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.nis.state.*;

public class AccountImportanceViewModelTest {

	// region basic operations

	@Test
	public void viewModelCanBeCreated() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final ReadOnlyAccountImportance importance = new AccountImportance();

		// Act:
		final AccountImportanceViewModel viewModel = new AccountImportanceViewModel(address, importance);

		// Assert:
		MatcherAssert.assertThat(viewModel.getAddress(), IsSame.sameInstance(address));
		MatcherAssert.assertThat(viewModel.getImportance(), IsSame.sameInstance(importance));
	}

	@Test
	public void viewModelCanBeRoundTripped() {
		// Arrange:
		final Address address = Utils.generateRandomAddress();
		final AccountImportance importance = new AccountImportance();
		importance.setImportance(BlockHeight.ONE, 123);
		final AccountImportanceViewModel originalViewModel = new AccountImportanceViewModel(address, importance);

		// Act:
		final AccountImportanceViewModel viewModel = new AccountImportanceViewModel(
				Utils.roundtripSerializableEntity(originalViewModel, null));

		// Assert:
		MatcherAssert.assertThat(viewModel.getAddress(), IsEqual.equalTo(address));
		MatcherAssert.assertThat(viewModel.getImportance().getImportance(BlockHeight.ONE), IsEqual.equalTo(123.0));
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final AccountImportanceViewModel viewModel = createViewModel("foo", 5, 1);

		// Assert:
		MatcherAssert.assertThat(createViewModel("foo", 5, 1), IsEqual.equalTo(viewModel));
		MatcherAssert.assertThat(createViewModel("bar", 5, 1), IsNot.not(IsEqual.equalTo(viewModel)));
		MatcherAssert.assertThat(createViewModel("foo", 2, 1), IsNot.not(IsEqual.equalTo(viewModel)));
		MatcherAssert.assertThat(createViewModel("foo", 5, 7), IsNot.not(IsEqual.equalTo(viewModel)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(viewModel)));
		MatcherAssert.assertThat(5L, IsNot.not(IsEqual.equalTo((Object) viewModel)));
	}

	@Test
	public void equalsCanCompareSetAndUnsetAccountImportances() {
		// Arrange:
		final AccountImportanceViewModel viewModelWithSetImportance = createViewModel("foo", 5, 1);
		final AccountImportanceViewModel viewModelWithUnsetImportance = createViewModel("foo");

		// Assert:
		MatcherAssert.assertThat(viewModelWithSetImportance, IsEqual.equalTo(createViewModel("foo", 5, 1)));
		MatcherAssert.assertThat(viewModelWithSetImportance, IsNot.not(IsEqual.equalTo(createViewModel("foo"))));
		MatcherAssert.assertThat(viewModelWithUnsetImportance, IsNot.not(IsEqual.equalTo(createViewModel("foo", 5, 1))));
		MatcherAssert.assertThat(viewModelWithUnsetImportance, IsEqual.equalTo(createViewModel("foo")));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final AccountImportanceViewModel viewModel = createViewModel("foo", 5, 1);
		final int hashCode = viewModel.hashCode();

		// Assert:
		MatcherAssert.assertThat(createViewModel("foo", 5, 1).hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(createViewModel("bar", 5, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createViewModel("foo", 2, 1).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(createViewModel("foo", 5, 7).hashCode(), IsEqual.equalTo(hashCode));
	}

	@Test
	public void hashCodesCanBeCalculatedForSetAndUnsetAccountImportances() {
		// Arrange:
		final AccountImportanceViewModel viewModelWithSetImportance = createViewModel("foo", 5, 1);
		final AccountImportanceViewModel viewModelWithUnsetImportance = createViewModel("foo");

		// Assert:
		MatcherAssert.assertThat(viewModelWithSetImportance.hashCode(), IsEqual.equalTo(createViewModel("foo", 5, 1).hashCode()));
		MatcherAssert.assertThat(viewModelWithSetImportance.hashCode(), IsNot.not(IsEqual.equalTo(createViewModel("foo").hashCode())));
		MatcherAssert.assertThat(viewModelWithUnsetImportance.hashCode(),
				IsNot.not(IsEqual.equalTo(createViewModel("foo", 5, 1).hashCode())));
		MatcherAssert.assertThat(viewModelWithUnsetImportance.hashCode(), IsEqual.equalTo(createViewModel("foo").hashCode()));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsAppropriateStringRepresentation() {
		// Arrange:
		final AccountImportanceViewModel viewModel = createViewModel("foo", 5, 1);

		// Assert:
		MatcherAssert.assertThat(viewModel.toString(), IsEqual.equalTo("FOO -> (5 : 1.000000)"));
	}

	// endregion

	private static AccountImportanceViewModel createViewModel(final String encodedAddress) {
		final ReadOnlyAccountImportance importance = new AccountImportance();
		return new AccountImportanceViewModel(Address.fromEncoded(encodedAddress), importance);
	}

	private static AccountImportanceViewModel createViewModel(final String encodedAddress, final int blockHeight,
			final double rawImportance) {
		final AccountImportance importance = new AccountImportance();
		importance.setImportance(new BlockHeight(blockHeight), rawImportance);
		return new AccountImportanceViewModel(Address.fromEncoded(encodedAddress), importance);
	}
}
