package org.nem.nis.validators.block;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.NisUtils;
import org.nem.nis.validators.*;
import org.nem.nis.ForkConfiguration;

import java.util.*;

public class TreasuryReissuanceForkTransactionBlockValidatorTest {

	private static List<Hash> generateRandomHashes(int count) {
		final List<Hash> hashes = new ArrayList<Hash>();
		for (int i = 0; i < count; ++i)
			hashes.add(Utils.generateRandomHash());

		return hashes;
	}

	@Test
	public void successWhenNotForkBlock() {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(1233);

		final List<Hash> hashes = generateRandomHashes(3);
		final List<Hash> fallbackHashes = generateRandomHashes(4);

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);
		final BlockValidator validator = new TreasuryReissuanceForkTransactionBlockValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void failureWhenForkBlockDoesNotContainExpectedTransactions() {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(1234);
		block.addTransaction(new MockTransaction(new TimeInstant(12)));
		block.addTransaction(new MockTransaction(new TimeInstant(24)));

		final List<Hash> hashes = generateRandomHashes(3);
		final List<Hash> fallbackHashes = generateRandomHashes(4);

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);
		final BlockValidator validator = new TreasuryReissuanceForkTransactionBlockValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
	}

	@Test
	public void failureWhenForkBlockContainsExpectedTransactionsOutOfOrder() {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(1234);
		block.addTransaction(new MockTransaction(new TimeInstant(12)));
		block.addTransaction(new MockTransaction(new TimeInstant(24)));
		block.addTransaction(new MockTransaction(new TimeInstant(36)));

		final List<Hash> hashes = new ArrayList<Hash>();
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(0)));
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(2)));
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(1)));

		final List<Hash> fallbackHashes = generateRandomHashes(4);

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);
		final BlockValidator validator = new TreasuryReissuanceForkTransactionBlockValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_UNKNOWN));
	}

	@Test
	public void successWhenForkBlockContainsExpectedTransactionsInOrder() {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(1234);
		block.addTransaction(new MockTransaction(new TimeInstant(12)));
		block.addTransaction(new MockTransaction(new TimeInstant(24)));
		block.addTransaction(new MockTransaction(new TimeInstant(36)));

		final List<Hash> hashes = new ArrayList<Hash>();
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(0)));
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(1)));
		hashes.add(HashUtils.calculateHash(block.getTransactions().get(2)));

		final List<Hash> fallbackHashes = generateRandomHashes(4);

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);
		final BlockValidator validator = new TreasuryReissuanceForkTransactionBlockValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void successWhenForkBlockContainsExpectedFallbackTransactionsInOrder() {
		// Arrange:
		final Block block = NisUtils.createRandomBlockWithHeight(1234);
		block.addTransaction(new MockTransaction(new TimeInstant(12)));
		block.addTransaction(new MockTransaction(new TimeInstant(24)));
		block.addTransaction(new MockTransaction(new TimeInstant(36)));

		final List<Hash> hashes = generateRandomHashes(4);

		final List<Hash> fallbackHashes = new ArrayList<Hash>();
		fallbackHashes.add(HashUtils.calculateHash(block.getTransactions().get(0)));
		fallbackHashes.add(HashUtils.calculateHash(block.getTransactions().get(1)));
		fallbackHashes.add(HashUtils.calculateHash(block.getTransactions().get(2)));

		final ForkConfiguration forkConfiguration = new ForkConfiguration(new BlockHeight(1234), hashes, fallbackHashes);
		final BlockValidator validator = new TreasuryReissuanceForkTransactionBlockValidator(forkConfiguration);

		// Act:
		final ValidationResult result = validator.validate(block);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}
}
