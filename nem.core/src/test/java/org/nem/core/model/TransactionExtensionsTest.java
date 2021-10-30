package org.nem.core.model;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;
import java.util.stream.*;

public class TransactionExtensionsTest {

	// region getChildSignatures

	@Test
	public void getSignaturesForNonMultisigReturnsEmptyStream() {
		// Arrange:
		final Transaction transaction = RandomTransactionFactory.createTransfer();

		// Act:
		final List<Transaction> signatures = TransactionExtensions.getChildSignatures(transaction).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(signatures.isEmpty(), IsEqual.equalTo(true));
	}

	@Test
	public void getSignaturesForMultisigOnlyReturnsOnlyChildSignatureTransactions() {
		// Arrange:
		final MultisigTransaction multisigTransaction = RandomTransactionFactory.createMultisigTransferWithThreeSignatures();

		// Act:
		final List<Transaction> signatures = TransactionExtensions.getChildSignatures(multisigTransaction).collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(signatures.size(), IsEqual.equalTo(3));
		MatcherAssert.assertThat(signatures, IsEquivalent.equivalentTo(new ArrayList<>(multisigTransaction.getCosignerSignatures())));
	}

	// endregion

	@Test
	public void canStreamSelfAndFirstChildTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamSelfAndFirstChildTransactions(transaction));

		// Assert:
		MatcherAssert.assertThat(customFields, IsEquivalent.equivalentTo(50, 60, 70, 80));
	}

	@Test
	public void canStreamSelfAndAllTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamSelfAndAllTransactions(transaction));

		// Assert:
		MatcherAssert.assertThat(customFields, IsEquivalent.equivalentTo(50, 60, 61, 62, 70, 80, 81, 82));
	}

	@Test
	public void canStreamDefaultTransactions() {
		// Arrange:
		final Transaction transaction = createTestTransaction();

		// Act:
		final List<Integer> customFields = getCustomFields(TransactionExtensions.streamDefault(transaction));

		// Assert:
		MatcherAssert.assertThat(customFields, IsEquivalent.equivalentTo(50, 60, 70, 80));
	}

	private static List<Integer> getCustomFields(final Stream<Transaction> stream) {
		return MockTransactionUtils.getCustomFields(stream);
	}

	private static Transaction createTestTransaction() {
		return MockTransactionUtils.createMockTransactionWithNestedChildren(50);
	}
}
