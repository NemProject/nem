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
	// TODO 20150313 BR -> J,G: that works indeed i tested it:
	// > Account A1 made account A2 a multisig account with A1 being the only cosignatory.
	// > Account B made A1 a multisig account with B the only cosignatory.
	// > With the GUI we have now, account A2 is now dead (no account can initiate a transaction for account A2).
	// > Change validators to forbid those things?
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
		final Collection<Address> multisigAddresses = context.multisigLinks.getCosignatoriesOf();

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
