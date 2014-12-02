package org.nem.nis.secret;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.Address;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;

public class MultisigLinksTest {
	//region MultisigLinks
	@Test
	public void emptyMultisigLinksIsNeitherCosignatoryNorMultisig() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		Assert.assertFalse(context.multisigLinks.isCosignatory());
		Assert.assertFalse(context.multisigLinks.isMultisig());
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
		context.addMultisig(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}

	@Test
	public void addingBothMakesMultisigAndCosignatory() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.addMultisig(context.address);
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
		context.addMultisig(context.address);

		// Act:
		context.removeMultisig(context.address);

		// Assert:
		Assert.assertThat(context.multisigLinks.isCosignatory(), IsEqual.equalTo(false));
		Assert.assertThat(context.multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}
	//endregion

	//region
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
		context.addMultisig(context.address);

		// Act:
		final MultisigLinks multisigLinks = context.makeCopy();

		// Assert:
		Assert.assertThat(multisigLinks.isCosignatoryOf(context.address), IsEqual.equalTo(true));
		Assert.assertThat(multisigLinks.isCosignatory(), IsEqual.equalTo(true));
		Assert.assertThat(multisigLinks.isMultisig(), IsEqual.equalTo(false));
	}
	//endregion

	private class TestContext {
		final MultisigLinks multisigLinks = new MultisigLinks();
		final Address address = Utils.generateRandomAddress();
		final BlockHeight blockHeight = new BlockHeight(1234L);

		public void addCosignatory(final Address address) {
			multisigLinks.addCosignatory(address, blockHeight);
		}

		public void removeCosignatory(final Address address) {
			multisigLinks.removeCosignatory(address, blockHeight);
		}

		public void addMultisig(final Address address) {
			multisigLinks.addMultisig(address, blockHeight);
		}

		public void removeMultisig(final Address address) {
			multisigLinks.removeMultisig(address, blockHeight);
		}

		public MultisigLinks makeCopy() {
			return multisigLinks.copy();
		}
	}
}
