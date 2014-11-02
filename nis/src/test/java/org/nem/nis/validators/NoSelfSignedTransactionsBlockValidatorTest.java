package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.poi.*;
import org.nem.nis.test.NisUtils;

public class NoSelfSignedTransactionsBlockValidatorTest {

	@Test
	public void blockWithNoTransactionsIsValid() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void blockWithTransactionsSignedByHarvesterIsNotValid() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransaction();
		context.addTransaction(context.harvester);
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
	}

	@Test
	public void blockWithTransactionsSignedByAccountOwningRemoteAccountIsNotValid() {
		// Arrange:
		final Account owningRemoteAccount = Utils.generateRandomAccount();
		final TestContext context = new TestContext();
		context.setHarvesterOwner(owningRemoteAccount.getAddress());
		context.addTransaction();
		context.addTransaction(owningRemoteAccount);
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_SELF_SIGNED_TRANSACTION));
	}

	@Test
	public void blockWithTransactionsSignedByOtherAccountsIsValid() {
		// Arrange:
		final TestContext context = new TestContext();
		context.addTransaction();
		context.addTransaction();
		context.addTransaction();

		// Act:
		final ValidationResult result = context.validator.validate(context.block);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final Account harvester = Utils.generateRandomAccount();
		private final Block block = NisUtils.createRandomBlockWithHeight(this.harvester, 12);
		private final PoiFacade poiFacade = Mockito.mock(PoiFacade.class);
		private final BlockValidator validator = new NoSelfSignedTransactionsBlockValidator(this.poiFacade);

		private TestContext() {
			Mockito.when(this.poiFacade.findForwardedStateByAddress(Mockito.any(), Mockito.eq(new BlockHeight(12))))
					.then(invocationOnMock -> new PoiAccountState((Address)invocationOnMock.getArguments()[0]));
		}

		private void addTransaction() {
			this.addTransaction(Utils.generateRandomAccount());
		}

		private void addTransaction(final Account account) {
			this.block.addTransaction(new MockTransaction(account));
		}

		private void setHarvesterOwner(final Address ownerAddress) {
			Mockito.when(this.poiFacade.findForwardedStateByAddress(this.harvester.getAddress(), new BlockHeight(12)))
					.thenReturn(new PoiAccountState(ownerAddress));
		}
	}
}