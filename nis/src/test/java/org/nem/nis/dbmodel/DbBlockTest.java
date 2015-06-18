package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.nis.mappers.TransactionRegistry;

import java.util.List;
import java.util.function.*;
import java.util.stream.*;

public class DbBlockTest {
	private static int NUM_TESTS = 0;

	@After
	public void increment() {
		++NUM_TESTS;
	}

	@AfterClass
	public static void assertCount() {
		// one entry in the registry is for multisig transactions which is not tested.
		Assert.assertThat(TransactionRegistry.size(), IsEqual.equalTo(NUM_TESTS + 1));
	}

	@Test
	public void setBlockTransferTransactionsFilterTransactionsWithNullSignature() {
		// Assert:
		assertTransactionsWithNullSignatureGetFiltered(
				DbBlock::getBlockTransferTransactions,
				DbBlock::setBlockTransferTransactions,
				DbTransferTransaction::new);
	}

	@Test
	public void setBlockImportanceTransferTransactionsFilterTransactionsWithNullSignature() {
		// Assert:
		assertTransactionsWithNullSignatureGetFiltered(
				DbBlock::getBlockImportanceTransferTransactions,
				DbBlock::setBlockImportanceTransferTransactions,
				DbImportanceTransferTransaction::new);
	}

	@Test
	public void setBlockMultisigAggregateModificationTransactionsFilterTransactionsWithNullSignature() {
		// Assert:
		assertTransactionsWithNullSignatureGetFiltered(
				DbBlock::getBlockMultisigAggregateModificationTransactions,
				DbBlock::setBlockMultisigAggregateModificationTransactions,
				DbMultisigAggregateModificationTransaction::new);
	}

	@Test
	public void setBlockProvisionNamespaceTransactionsFilterTransactionsWithNullSignature() {
		// Assert:
		assertTransactionsWithNullSignatureGetFiltered(
				DbBlock::getBlockProvisionNamespaceTransactions,
				DbBlock::setBlockProvisionNamespaceTransactions,
				DbProvisionNamespaceTransaction::new);
	}

	private static <T extends AbstractBlockTransfer> void assertTransactionsWithNullSignatureGetFiltered(
			final Function<DbBlock, List<T>> getFromBlock,
			final BiConsumer<DbBlock, List<T>> setInBlock,
			final Supplier<T> activator) {
		// Arrange:
		final DbBlock dbBlock = new DbBlock();
		final List<T> dbTransactions = createTransactions(activator);
		addTransactionsWithNullSignature(dbTransactions, activator);

		// Act:
		setInBlock.accept(dbBlock, dbTransactions);

		// Assert:
		getFromBlock.apply(dbBlock).stream()
				.forEach(t -> Assert.assertThat(t.getSenderProof(), IsNull.notNullValue()));
	}

	private static <T extends AbstractBlockTransfer> List<T> createTransactions(final Supplier<T> activator) {
		return IntStream.range(0, 10)
				.mapToObj(i -> {
					final T t = activator.get();
					t.setSenderProof(new byte[64]);
					return t;
				})
				.collect(Collectors.toList());
	}

	private static <T extends AbstractBlockTransfer> void addTransactionsWithNullSignature(final List<T> dbTransactions, final Supplier<T> activator) {
		dbTransactions.add(0, activator.get());
		dbTransactions.add(5, activator.get());
		dbTransactions.add(activator.get());
	}
}
