package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.dao.*;
import org.nem.nis.dbmodel.*;

import java.util.function.Consumer;

public class UniqueHashTransactionValidatorTest {
	private static final long MARKER_HEIGHT = BlockMarkerConstants.FATAL_TX_BUG_HEIGHT;

	//region transaction already exists in transfer dao before marker height

	@Test
	public void validateReturnsSuccessIfTransactionAlreadyExistsInTransferDaoBeforeMarkerHeight() {
		// Assert:
		assertSuccessIfTransactionAlreadyExistsInDaoBeforeMarkerHeight(TestContext::setTransferDaoForHash);
	}

	@Test
	public void validateReturnsSuccessIfTransactionAlreadyExistsInImportanceTransferDaoBeforeMarkerHeight() {
		// Assert:
		assertSuccessIfTransactionAlreadyExistsInDaoBeforeMarkerHeight(TestContext::setImportanceTransferDaoForHash);
	}

	private static void assertSuccessIfTransactionAlreadyExistsInDaoBeforeMarkerHeight(final Consumer<TestContext> setTransactionInDao) {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionInDao.accept(context);

		// Act:
		final ValidationResult result = context.validateAtHeight(MARKER_HEIGHT - 1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region transaction already exists in transfer dao at / after market height

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInTransferDaoAtMarkerHeight() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDaoAtHeight(MARKER_HEIGHT, TestContext::setTransferDaoForHash);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInImportanceTransferDaoAtMarkerHeight() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDaoAtHeight(MARKER_HEIGHT, TestContext::setImportanceTransferDaoForHash);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInTransferDaoAfterMarkerHeight() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDaoAtHeight(MARKER_HEIGHT + 1, TestContext::setTransferDaoForHash);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInImportanceTransferDaoAfterMarkerHeight() {
		// Assert:
		assertNeutralIfTransactionAlreadyExistsInDaoAtHeight(MARKER_HEIGHT + 1, TestContext::setImportanceTransferDaoForHash);
	}

	private static void assertNeutralIfTransactionAlreadyExistsInDaoAtHeight(
			final long height,
			final Consumer<TestContext> setTransactionInDao) {
		// Arrange:
		final TestContext context = new TestContext();
		setTransactionInDao.accept(context);

		// Act:
		final ValidationResult result = context.validateAtHeight(height);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.NEUTRAL));
	}

	//endregion

	@Test
	public void validateReturnsSuccessIfTransactionDoesNotExistInAnyDaoAfterMarkerHeight() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ValidationResult result = context.validateAtHeight(MARKER_HEIGHT + 1);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	private static class TestContext {
		private final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7);
		private final Hash hash = HashUtils.calculateHash(this.transaction);

		private final TransferDao transferDao = Mockito.mock(TransferDao.class);
		private final ImportanceTransferDao importanceTransferDao = Mockito.mock(ImportanceTransferDao.class);
		private final TransactionValidator validator = new UniqueHashTransactionValidator(this.transferDao, this.importanceTransferDao);

		private void setTransferDaoForHash() {
			Mockito.when(this.transferDao.findByHash(this.hash.getRaw())).thenReturn(Mockito.mock(Transfer.class));
		}

		private void setImportanceTransferDaoForHash() {
			Mockito.when(this.importanceTransferDao.findByHash(this.hash.getRaw())).thenReturn(Mockito.mock(ImportanceTransfer.class));
		}

		private ValidationResult validateAtHeight(final long height) {
			return this.validator.validate(this.transaction, new ValidationContext(new BlockHeight(height)));
		}
	}
}