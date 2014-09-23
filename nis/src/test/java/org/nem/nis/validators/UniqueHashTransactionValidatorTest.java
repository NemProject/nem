package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.test.*;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;

public class UniqueHashTransactionValidatorTest {

	@Test
	public void validateReturnsNeutralIfTransactionAlreadyExistsInDao() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(Mockito.mock(Transfer.class), ValidationResult.NEUTRAL);
	}

	@Test
	public void validateReturnsSuccessIfTransactionDoesNotExistInDao() {
		// Assert:
		assertFindByHashResultIsMappedToStatus(null, ValidationResult.SUCCESS);
	}

	private static void assertFindByHashResultIsMappedToStatus(final Transfer findByHashResult, final ValidationResult expectedResult) {
		// Arrange:
		final Transaction transaction = new MockTransaction(Utils.generateRandomAccount(), 7);
		final Hash hash = HashUtils.calculateHash(transaction);

		final TransferDao transferDao = Mockito.mock(TransferDao.class);
		Mockito.when(transferDao.findByHash(hash.getRaw())).thenReturn(findByHashResult);
		final TransactionValidator validator = new UniqueHashTransactionValidator(transferDao);

		// Act:
		final ValidationResult result = validator.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(expectedResult));
	}
}