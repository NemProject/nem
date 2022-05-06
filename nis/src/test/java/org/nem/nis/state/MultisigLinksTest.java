package org.nem.nis.state;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.test.*;

import java.util.Collection;
import java.util.stream.IntStream;

public class MultisigLinksTest {

	// region MultisigLinks

	@Test
	public void emptyMultisigLinksIsNeitherCosignatoryNorMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void addingCosignatoryMakesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatory(context.address);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	@Test
	public void addingToAccountMakesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatoryOf(context.address);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddCosignatoryAfterCosignatoryOf() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatoryOf(context.address);
		ExceptionAssert.assertThrows(v -> context.addCosignatory(context.address), IllegalArgumentException.class);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void cannotAddCosignatoryOfAfterCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatory(context.address);
		ExceptionAssert.assertThrows(v -> context.addCosignatoryOf(context.address), IllegalArgumentException.class);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	// endregion

	// region removal

	@Test
	public void canRemoveCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		context.removeCosignatory(context.address);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void canRemoveMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryOf(context.address);

		// Act:
		context.removeCosignatoryOf(context.address);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	// endregion

	// region copy

	@Test
	public void copyCopiesMultisig() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		MatcherAssert.assertThat(multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		MatcherAssert.assertThat(multisigLinks.isMultisig(), IsEqual.equalTo(true));
	}

	@Test
	public void copyCopiesCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatoryOf(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		MatcherAssert.assertThat(multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		MatcherAssert.assertThat(multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		MatcherAssert.assertThat(multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void copyPreservesMinCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());
		context.multisigLinks.incrementMinCosignatoriesBy(2);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		MatcherAssert.assertThat(multisigLinks.minCosignatories(), IsEqual.equalTo(2));
	}

	// endregion

	// region getCosignatories

	@Test
	public void getCosignatoriesIsReadOnly() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Collection<Address> cosignatories = context.multisigLinks.getCosignatories();
		ExceptionAssert.assertThrows(v -> cosignatories.add(Utils.generateRandomAddress()), UnsupportedOperationException.class);
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
		MatcherAssert.assertThat(cosignatories, IsEquivalent.equivalentTo(cosignatory1, cosignatory2));
	}

	// endregion

	// region getCosignatoriesOf

	@Test
	public void getCosignatoriesOfIsReadOnly() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoriesOf();
		ExceptionAssert.assertThrows(v -> multisigAddresses.add(Utils.generateRandomAddress()), UnsupportedOperationException.class);
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
		MatcherAssert.assertThat(multisigAddresses, IsEquivalent.equivalentTo(multisig1, multisig2));
	}

	// endregion

	// region incrementCosignatoriesBy

	@Test
	public void incrementCosignatoriesByFailsIfResultingMinCosignatoriesIsNegative() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		ExceptionAssert.assertThrows(v -> context.multisigLinks.incrementMinCosignatoriesBy(-1), IllegalArgumentException.class);
	}

	@Test
	public void incrementCosignatoriesBySucceedsIfResultingMinCosignatoriesIsLargerThanNumberOfCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());

		// Act:
		context.multisigLinks.incrementMinCosignatoriesBy(3);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(3));
	}

	@Test
	public void incrementCosignatoriesBySucceedsIfResultingMinCosignatoriesIsZero() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.multisigLinks.incrementMinCosignatoriesBy(1);

		// Act:
		context.multisigLinks.incrementMinCosignatoriesBy(-1);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(0));
	}

	@Test
	public void incrementCosignatoriesBySucceedsIfResultingMinCosignatoriesIsTheNumberOfCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());
		context.addCosignatory(Utils.generateRandomAddress());

		// Act:
		context.multisigLinks.incrementMinCosignatoriesBy(3);

		// Assert:
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(3));
	}

	@Test
	public void incrementCosignatoriesBySucceedsIfMinCosignatoriesIsBetweenZeroAndTheNumberOfCosignatories() {
		// Arrange:
		final TestContext context = new TestContext();
		IntStream.range(0, 5).forEach(i -> context.addCosignatory(Utils.generateRandomAddress()));

		// Act + Assert:
		context.multisigLinks.incrementMinCosignatoriesBy(3);
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(3));
		context.multisigLinks.incrementMinCosignatoriesBy(-2);
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(1));
		context.multisigLinks.incrementMinCosignatoriesBy(3);
		MatcherAssert.assertThat(context.multisigLinks.minCosignatories(), IsEqual.equalTo(4));
	}

	// endregion

	private class TestContext {
		final MultisigLinks multisigLinks = new MultisigLinks();
		final Address address = Utils.generateRandomAddress();

		private void addCosignatory(final Address address) {
			this.multisigLinks.addCosignatory(address);
		}

		private void removeCosignatory(final Address address) {
			this.multisigLinks.removeCosignatory(address);
		}

		private void addCosignatoryOf(final Address address) {
			this.multisigLinks.addCosignatoryOf(address);
		}

		private void removeCosignatoryOf(final Address address) {
			this.multisigLinks.removeCosignatoryOf(address);
		}

		private MultisigLinks makeCopy() {
			return this.multisigLinks.copy();
		}
	}
}
