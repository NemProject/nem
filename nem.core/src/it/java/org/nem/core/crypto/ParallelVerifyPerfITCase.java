package org.nem.core.crypto;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.test.RandomTransactionFactory;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.*;

public class ParallelVerifyPerfITCase {
	private static final Logger LOGGER = Logger.getLogger(ParallelVerifyPerfITCase.class.getName());

	@Test
	public void parallelVerifyIsFasterThanSequentialVerify() {
		// Arrange:
		LOGGER.info("Creating transactions...");
		final int count = 10000;
		final Collection<Transaction> transactions = createTransactions(count);

		// Warm up
		for (int i = 0; i < 5; i++) {
			LOGGER.info("Warm up round " + (i + 1));
			Assert.assertThat(transactions.parallelStream().allMatch(VerifiableEntity::verify), IsEqual.equalTo(true));
		}

		// Act:
		LOGGER.info("Running parallel test...");
		final long startParallel = System.nanoTime();
		final boolean resultParallel = transactions.parallelStream().allMatch(VerifiableEntity::verify);
		final long stopParallel = System.nanoTime();
		LOGGER.info(String.format("Parallel verify: %d ns/tx", (stopParallel - startParallel) / count));

		LOGGER.info("Running sequential test...");
		final long startSequential = System.nanoTime();
		final boolean resultSequential = transactions.stream().allMatch(VerifiableEntity::verify);
		final long stopSequential = System.nanoTime();
		LOGGER.info(String.format("Sequential verify: %d ns/tx", (stopSequential - startSequential) / count));

		// Assert:
		Assert.assertThat(resultParallel, IsEqual.equalTo(true));
		Assert.assertThat(resultSequential, IsEqual.equalTo(true));
		Assert.assertThat((stopSequential - startSequential) / (stopParallel - startParallel) > 2, IsEqual.equalTo(true));
	}

	private static Collection<Transaction> createTransactions(final int count) {
		final Collection<Transaction> transactions = IntStream.range(0, count)
				.mapToObj(i -> RandomTransactionFactory.createTransfer())
				.collect(Collectors.toList());
		transactions.forEach(VerifiableEntity::sign);
		return transactions;
	}
}
