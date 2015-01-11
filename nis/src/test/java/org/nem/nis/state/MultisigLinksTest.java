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

	// TODO 20150103 J-G: we should probably not allow this
	@Test
	public void addingBothMakesMultisigAndCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addCosignatoryOf(context.address);
		context.addCosignatory(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(true));
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
		final Address multisig1 = Utils.generateRandomAddress();
		final Address multisig2 = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		context.addCosignatory(cosignatory1);
		context.addCosignatory(cosignatory2);
		context.addCosignatoryOf(multisig1);
		context.addCosignatoryOf(multisig2);

		// Act:
		final Collection<Address> cosignatories = context.multisigLinks.getCosignatories();

		// Assert:
		Assert.assertThat(cosignatories, IsEquivalent.equivalentTo(cosignatory1, cosignatory2));
	}

	//endregion

	// TODO 20150111 J-G: should rename this probably getCosignatoriesOf or getMultisigs
	//region getCosignatoriesOf

	@Test
	public void getCosignatoryOfIsReadOnly() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoryOf();
		ExceptionAssert.assertThrows(
				v -> multisigAddresses.add(Utils.generateRandomAddress()),
				UnsupportedOperationException.class);
	}

	@Test
	public void getCosignatoryOfReturnsAllMultisigAccounts() {
		// Arrange:
		final Address cosignatory1 = Utils.generateRandomAddress();
		final Address cosignatory2 = Utils.generateRandomAddress();
		final Address multisig1 = Utils.generateRandomAddress();
		final Address multisig2 = Utils.generateRandomAddress();
		final TestContext context = new TestContext();
		context.addCosignatory(cosignatory1);
		context.addCosignatory(cosignatory2);
		context.addCosignatoryOf(multisig1);
		context.addCosignatoryOf(multisig2);

		// Act:
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoryOf();

		// Assert:
		Assert.assertThat(multisigAddresses, IsEquivalent.equivalentTo(multisig1, multisig2));
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

		public MultisigLinks makeCopy() {
			return this.multisigLinks.copy();
		}
	}
}
