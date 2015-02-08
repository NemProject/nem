package org.nem.core.model;

import org.junit.*;
import org.nem.core.crypto.Hash;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;

import java.util.List;
import java.util.stream.Stream;

public class BlockExtensionsTest {

	@Test
	public void canStreamDirectTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDirectTransactions(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 100, 150));
	}

	@Test
	public void canStreamDirectAndFirstChildTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDirectAndFirstChildTransactions(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80, 100, 110, 120, 130, 150, 160, 170, 180));
	}

	@Test
	public void canStreamDefaultTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamDefault(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(50, 60, 70, 80, 100, 110, 120, 130, 150, 160, 170, 180));
	}

	@Test
	public void canStreamAllTransactions() {
		// Arrange:
		final Block block = createTestBlock();

		// Act:
		final List<Integer> customFields = getCustomFields(BlockExtensions.streamAllTransactions(block));

		// Assert:
		Assert.assertThat(
				customFields,
				IsEquivalent.equivalentTo(
						50, 60, 61, 62, 70, 80, 81, 82,
						100, 110, 111, 112, 120, 130, 131, 132,
						150, 160, 161, 162, 170, 180, 181, 182));
	}

	private static List<Integer> getCustomFields(final Stream<Transaction> stream) {
		return MockTransactionUtils.getCustomFields(stream);
	}

	private static Block createTestBlock() {
		final Block block = new Block(Utils.generateRandomAccount(), Hash.ZERO, Hash.ZERO, TimeInstant.ZERO, BlockHeight.ONE);
		block.addTransaction(MockTransactionUtils.createMockTransactionWithNestedChildren(50));
		block.addTransaction(MockTransactionUtils.createMockTransactionWithNestedChildren(100));
		block.addTransaction(MockTransactionUtils.createMockTransactionWithNestedChildren(150));
		return block;
	}
}