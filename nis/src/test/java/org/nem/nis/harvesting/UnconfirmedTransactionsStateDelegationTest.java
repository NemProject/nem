package org.nem.nis.harvesting;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.UnconfirmedTransactionsTestUtils;

import java.util.*;
import java.util.function.BiFunction;

public abstract class UnconfirmedTransactionsStateDelegationTest implements UnconfirmedTransactionsTestUtils.UnconfirmedTransactionsTest {

	// region 1:1 delegation

	@Test
	public void getUnconfirmedBalanceDelegatesToState() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = Utils.generateRandomAccount();
		Mockito.when(context.state.getUnconfirmedBalance(Mockito.any())).thenReturn(Amount.fromNem(757));

		// Act:
		final Amount result = context.transactions.getUnconfirmedBalance(account);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(Amount.fromNem(757)));
		Mockito.verify(context.state, Mockito.only()).getUnconfirmedBalance(account);
	}

	@Test
	public void getUnconfirmedMosaicBalanceDelegatesToState() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Account account = Utils.generateRandomAccount();
		final MosaicId mosaicId = Utils.createMosaicId(12);
		Mockito.when(context.state.getUnconfirmedMosaicBalance(Mockito.any(), Mockito.any())).thenReturn(new Quantity(898));

		// Act:
		final Quantity result = context.transactions.getUnconfirmedMosaicBalance(account, mosaicId);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(new Quantity(898)));
		Mockito.verify(context.state, Mockito.only()).getUnconfirmedMosaicBalance(account, mosaicId);
	}

	@Test
	public void addNewBatchDelegatesToState() {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Collection<Transaction> transactions = createMockTransactions(4, 7);
		Mockito.when(context.state.addNewBatch(Mockito.any())).thenReturn(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);

		// Act:
		final ValidationResult result = context.transactions.addNewBatch(transactions);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
		Mockito.verify(context.state, Mockito.only()).addNewBatch(transactions);
	}

	@Test
	public void addNewDelegatesToState() {
		// Assert:
		this.addDelegatesToState(UnconfirmedState::addNew);
	}

	@Test
	public void addExistingDelegatesToState() {
		// Assert:
		this.addDelegatesToState(UnconfirmedState::addExisting);
	}

	private void addDelegatesToState(final BiFunction<UnconfirmedState, Transaction, ValidationResult> add) {
		// Arrange:
		final TestContext context = this.createTestContext();
		final Transaction transaction = createMockTransaction(5);
		Mockito.when(add.apply(context.state, Mockito.any())).thenReturn(ValidationResult.FAILURE_ENTITY_INVALID_VERSION);

		// Act:
		final ValidationResult result = add.apply(context.transactions, transaction);

		// Assert:
		MatcherAssert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_INVALID_VERSION));
		add.apply(Mockito.verify(context.state, Mockito.only()), transaction);
	}

	// endregion

	// region create transactions

	private static MockTransaction createMockTransaction(final int customField) {
		return new MockTransaction(Utils.generateRandomAccount(), customField, new TimeInstant(customField));
	}

	private static List<Transaction> createMockTransactions(final int startCustomField, final int endCustomField) {
		final List<Transaction> transactions = new ArrayList<>();

		for (int i = startCustomField; i <= endCustomField; ++i) {
			transactions.add(createMockTransaction(i));
		}

		return transactions;
	}

	// endregion

	// region TestContext

	private TestContext createTestContext() {
		return new TestContext(this::createUnconfirmedTransactions);
	}

	private static class TestContext {
		private final UnconfirmedState state = Mockito.mock(UnconfirmedState.class);
		private final UnconfirmedTransactions transactions;

		public TestContext(final BiFunction<UnconfirmedStateFactory, ReadOnlyNisCache, UnconfirmedTransactions> creator) {
			final UnconfirmedStateFactory factory = Mockito.mock(UnconfirmedStateFactory.class);
			Mockito.when(factory.create(Mockito.any(), Mockito.any())).thenReturn(this.state);
			this.transactions = creator.apply(factory, Mockito.mock(NisCache.class));
		}
	}

	// endregion
}
