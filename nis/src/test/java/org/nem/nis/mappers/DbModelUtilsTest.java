package org.nem.nis.mappers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;
import org.nem.nis.dbmodel.*;

import java.util.function.BiConsumer;

public class DbModelUtilsTest {

	// region getInnerTransaction

	@Test
	public void getInnerTransactionReturnsTransferWhenPresent() {
		// Assert:
		assertGetInnerTransactionReturnsTransferWhenPresent(new DbTransferTransaction(), DbMultisigTransaction::setTransferTransaction);
	}

	@Test
	public void getInnerTransactionReturnsImportanceTransferWhenPresent() {
		// Assert:
		assertGetInnerTransactionReturnsTransferWhenPresent(new DbImportanceTransferTransaction(),
				DbMultisigTransaction::setImportanceTransferTransaction);
	}

	@Test
	public void getInnerTransactionReturnsMultisigModificationWhenPresent() {
		// Assert:
		assertGetInnerTransactionReturnsTransferWhenPresent(new DbMultisigAggregateModificationTransaction(),
				DbMultisigTransaction::setMultisigAggregateModificationTransaction);
	}

	private static <T extends AbstractTransfer> void assertGetInnerTransactionReturnsTransferWhenPresent(final T dbTransfer,
			final BiConsumer<DbMultisigTransaction, T> setInnerTransaction) {
		// Arrange:
		final DbMultisigTransaction dbMultisig = new DbMultisigTransaction();
		setInnerTransaction.accept(dbMultisig, dbTransfer);

		// Act:
		final AbstractTransfer dbInnerTransfer = DbModelUtils.getInnerTransaction(dbMultisig);

		// Assert:
		MatcherAssert.assertThat(dbInnerTransfer, IsSame.sameInstance(dbTransfer));
	}

	@Test
	public void getInnerTransactionThrowsExceptionWhenNoInnerTransferIsSet() {
		// Arrange:
		final DbMultisigTransaction dbMultisig = new DbMultisigTransaction();

		// Act:
		ExceptionAssert.assertThrows(v -> DbModelUtils.getInnerTransaction(dbMultisig), IllegalArgumentException.class);
	}

	// endregion

	// region isInnerTransaction

	@Test
	public void isInnerTransactionReturnsTrueIfSignatureIsNull() {
		// Arrange:
		final DbTransferTransaction dbTransfer = new DbTransferTransaction();

		// Act:
		final boolean isInnerTransaction = DbModelUtils.isInnerTransaction(dbTransfer);

		// Assert:
		MatcherAssert.assertThat(isInnerTransaction, IsEqual.equalTo(true));
	}

	@Test
	public void isInnerTransactionReturnsFalseIfSignatureIsPresent() {
		// Arrange:
		final DbTransferTransaction dbTransfer = new DbTransferTransaction();
		dbTransfer.setSenderProof(Utils.generateRandomSignature().getBytes());

		// Act:
		final boolean isInnerTransaction = DbModelUtils.isInnerTransaction(dbTransfer);

		// Assert:
		MatcherAssert.assertThat(isInnerTransaction, IsEqual.equalTo(false));
	}

	// endregion
}
