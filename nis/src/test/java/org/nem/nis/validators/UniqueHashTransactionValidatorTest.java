package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;

public class UniqueHashTransactionValidatorTest {
	private static final long MARKER_HEIGHT = BlockMarkerConstants.FATAL_TX_BUG_HEIGHT;

	@Test
	public void validateReturnsSuccessIfTransactionAlreadyExistsInDaoBeforeMarkerHeight() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(Mockito.mock(Transfer.class), MARKER_HEIGHT - 1, ValidationResult.SUCCESS);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInDaoAtMarkerHeight() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(Mockito.mock(Transfer.class), MARKER_HEIGHT, ValidationResult.NEUTRAL);
	}

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInDaoAfterMarkerHeight() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(Mockito.mock(Transfer.class), MARKER_HEIGHT + 1, ValidationResult.NEUTRAL);
	}

	@Test
	public void validateReturnsSuccessIfTransactionDoesNotExistInDaoAfterMarkerHeight() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(null, MARKER_HEIGHT + 1, ValidationResult.SUCCESS);
	}

	private static void assertFindByHashResultIsMappedToStatus(
			final Transfer findByHashResult,
			final long height,
			final ValidationResult expectedResult) {
		// Arrange:
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7);
		final Hash hash = HashUtils.calculateHash(transaction);

		final TransferDao transferDao = Mockito.mock(TransferDao.class);
		Mockito.when(transferDao.findByHash(hash.getRaw())).thenReturn(findByHashResult);
		final TransactionValidator validator = new UniqueHashTransactionValidator(transferDao);

		// Act:
		final ValidationResult result = validator.validate(transaction, new ValidationContext(new BlockHeight(height)));

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}