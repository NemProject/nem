package org.nem.nis.state;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;

import java.util.Collection;

public class MultisigLinksTest {

	//region MultisigLinks

	@Test
	public void emptyMultisigLinksIsNeitherCosignatoryNorMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void addingCosignatoryMakesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatory(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	@Test
	public void addingToAccountMakesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatoryOf(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddCosignatoryAfterCosignatoryOf() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatoryOf(context.address);
		ExceptionAssert.assertThrows(
				v -> context.addCosignatory(context.address),
				IllegalArgumentException.class);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddCosignatoryOfAfterCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatory(context.address);
		ExceptionAssert.assertThrows(
				v -> context.addCosignatoryOf(context.address),
				IllegalArgumentException.class);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	@Test
	public void addCosignatoryIncreasesMinCosignatoriesByOne() {
		// Arrange:
		final TestContext context = new TestContext();
		final int oldMinCosignatories = context.multisigLinks.minCosignatories();

		// Act:
		context.addCosignatory(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(oldMinCosignatories + 1));
	}

	//endregion

	//region removal

	@Test
	public void canRemoveCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void canRemoveMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryOf(context.address);

		// Act:
		context.removeCosignatoryOf(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void removeCosignatoryDecreasesMinCosignatoriesByOneIfMinCosignatoriesIsLargerThanOne() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);
		context.addCosignatory(Utils.generateRandomAddress());
		final int oldMinCosignatories = context.multisigLinks.minCosignatories();

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		Assert.assertThat(oldMinCosignatories, IsEqual.equalTo(2));
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(1));
	}

	@Test
	public void removeCosignatoryDecreasesMinCosignatoriesByOneIfMinCosignatoriesIsOneAndTheNumberOfCosignatoriesIsOne() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);
		final int oldMinCosignatories = context.multisigLinks.minCosignatories();

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		Assert.assertThat(oldMinCosignatories, IsEqual.equalTo(1));
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(0));
	}

	@Test
	public void removeCosignatoryDoesNotChangeMinCosignatoriesIfMinCosignatoriesIsOneAndCosignatoryListIsNonEmptyAfterRemoval() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);
		context.addCosignatory(Utils.generateRandomAddress());
		context.incrementMinCosignatoriesBy(-1);
		final int oldMinCosignatories = context.multisigLinks.minCosignatories();

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		Assert.assertThat(oldMinCosignatories, IsEqual.equalTo(1));
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(1));
	}

	//endregion

	//region copy

	@Test
	public void copyCopiesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertThat(multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	@Test
	public void copyCopiesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryOf(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertThat(multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void copyPreservesMinCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertThat(multisigLinks.minCosignatories(), IsEqual.equalTo(context.multisigLinks.minCosignatories()));
	}

	//endregion

	//region getCosignatories

	@Test
	public void getCosignatoriesIsReadOnly() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Collection<Address> cosignatories = context.multisigLinks.getCosignatories();
		ExceptionAssert.assertThrows(
				v -> cosignatories.add(Utils.generateRandomAddress()),
				UnsupportedOperationException.class);
	}

	@Test
	public void getCosignatoriesReturnsAllCosignatoryAccounts() {
		// Arrange:
		final Address cosignatory1 = Utils.generateRandomAddress();
		final Address cosignatory2 = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		context.addCosignatory(cosignatory1);
		context.addCosignatory(cosignatory2);

		// Act:
		final Collection<Address> cosignatories = context.multisigLinks.getCosignatories();

		// Assert:
		Assert.assertThat(cosignatories, IsEquivalent.equivalentTo(cosignatory1, cosignatory2));
	}

	//endregion

	//region getCosignatoriesOf

	@Test
	public void getCosignatoriesOfIsReadOnly() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoriesOf();
		ExceptionAssert.assertThrows(
				v -> multisigAddresses.add(Utils.generateRandomAddress()),
				UnsupportedOperationException.class);
	}

	@Test
	public void getCosignatoriesOfReturnsAllMultisigAccounts() {
		// Arrange:
		final Address multisig1 = Utils.generateRandomAddress();
		final Address multisig2 = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		context.addCosignatoryOf(multisig1);
		context.addCosignatoryOf(multisig2);

		// Act:
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoriesOf();

		// Assert:
		Assert.assertThat(multisigAddresses, IsEquivalent.equivalentTo(multisig1, multisig2));
	}

	//endregion

	//region setMinCosignatories

	@Test
	public void incrementCosignatoriesByFailsIfResultingMinCosignatoriesIsNegative() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		ExceptionAssert.assertThrows(v -> context.multisigLinks.incrementMinCosignatoriesBy(-1), IllegalArgumentException.class);
	}

	@Test
	public void incrementCosignatoriesByFailsIfResultingMinCosignatoriesIsZeroAndCosignatoriesIsNonEmptySet() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());

		// Assert:
		ExceptionAssert.assertThrows(v -> context.multisigLinks.incrementMinCosignatoriesBy(-1), IllegalArgumentException.class);
	}

	@Test
	public void incrementCosignatoriesByFailsIfResultingMinCosignatoriesIsLargerThanNumberOfCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());

		// Assert:
		ExceptionAssert.assertThrows(v -> context.multisigLinks.incrementMinCosignatoriesBy(3), IllegalArgumentException.class);
	}

	@Test
	public void incrementCosignatoriesBySucceedsIfMinCosignatoriesIsWithinAllowedRange() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());

		// Act + Assert:
		context.multisigLinks.incrementMinCosignatoriesBy(-2);
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(1));
		context.multisigLinks.incrementMinCosignatoriesBy(2);
		Assert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(3));
	}

	//endregion

	private class TestContext {
		final MultisigLinks multisigLinks = new MultisigLinks();
		final Address address = Utils.generateRandomAddress();

		public void addCosignatory(final Address address) {
			this.multisigLinks.addCosignatory(address);
		}

		public void removeCosignatory(final Address address) {
			this.multisigLinks.removeCosignatory(address);
		}

		public void addCosignatoryOf(final Address address) {
			this.multisigLinks.addCosignatoryOf(address);
		}

		public void removeCosignatoryOf(final Address address) {
			this.multisigLinks.removeCosignatoryOf(address);
		}

		private void incrementMinCosignatoriesBy(final int minCosignatories) {
			this.multisigLinks.incrementMinCosignatoriesBy(minCosignatories);
		}

		public MultisigLinks makeCopy() {
			return this.multisigLinks.copy();
		}
	}
}
