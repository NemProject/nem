package org.nem.nis.dbmodel;

import org.hamcrest.core.*;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nem.core.model.Transaction;
import org.nem.core.test.TestTransactionRegistry;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.mappers.TransactionRegistry;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.*;
import java.util.stream.*;

@RunWith(Enclosed.class)
public class DbBlockTest {

	@RunWith(Parameterized.class)
	public static class PerTransaction {
		private final TransactionRegistry.Entry<? extends AbstractTransfer, ? extends Transaction> entry;

		public PerTransaction(final int type) {
			this.entry = TransactionRegistry.findByType(type);
		}

		@Parameterized.Parameters
		public static Collection<Object[]> data() {
			return TestTransactionRegistry.getBlockEmbeddableTypeParameters();
		}

		@Test
		public void setBlockTransactionsFilterTransactionsWithNullSignature() {
			// Arrange:
			@SuppressWarnings("unchecked")
			final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry =
					(TransactionRegistry.Entry<AbstractBlockTransfer, ?>) this.entry;

			// Assert:
			assertTransactionsWithNullSignatureGetFiltered(
					entry.getFromBlock,
					entry.setInBlock,
					() -> ExceptionUtils.propagate((Callable<AbstractBlockTransfer>)entry.dbModelClass::newInstance));
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
			Assert.assertThat(getFromBlock.apply(dbBlock).size(), IsEqual.equalTo(10));
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
}
