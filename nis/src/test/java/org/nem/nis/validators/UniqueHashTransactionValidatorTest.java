package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.function.Consumer;

public class UniqueHashTransactionValidatorTest {

	//region transaction already exists in transfer dao

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInTransferDao() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDao(TestContext::setTransferDaoForHash);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInImportanceTransferDao() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDao(TestContext::setImportanceTransferDaoForHash);
	}

	private static void assertNeutralIfTransactionAlreadyExistsInDao(final Consumer<TestContext> setTransactionInDao) {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionInDao.accept(context);

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	//endregion

	@Test
	public void validateReturnsSuccessIfTransactionDoesNotExistInAnyDao() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(10);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final BlockHeight confirmedBlockHeight = new BlockHeight(17);
		private final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7);
		private final Hash hash = HashUtils.calculateHash(this.transaction);

		private final TransferDao transferDao = Mockito.mock(TransferDao.class);
		private final ImportanceTransferDao importanceTransferDao = Mockito.mock(ImportanceTransferDao.class);
		private final TransactionValidator validator = new UniqueHashTransactionValidator(this.transferDao, this.importanceTransferDao);

		private void setTransferDaoForHash() {
			Mockito.when(this.transferDao.findByHash(this.hash.getRaw(), this.confirmedBlockHeight.getRaw()))
					.thenReturn(Mockito.mock(Transfer.class));
		}

		private void setImportanceTransferDaoForHash() {
			Mockito.when(this.importanceTransferDao.findByHash(this.hash.getRaw(), this.confirmedBlockHeight.getRaw()))
					.thenReturn(Mockito.mock(ImportanceTransfer.class));
		}

		private ValidationResult validateAtHeight(final long height) {
			final ValidationContext validationContext = new ValidationContext(new BlockHeight(height), this.confirmedBlockHeight);
			return this.validator.validate(this.transaction, validationContext);
		}
	}
}