package org.nem.nis.pox.pos;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.Collectors;

public class PosImportanceCalculatorTest {
	private static final BlockHeight HEIGHT = new BlockHeight(123);

	@Before
	public void setup() {
		NemStateGlobals.setWeightedBalancesSupplier(null);
		NemStateGlobals.setWeightedBalancesSupplier(AlwaysVestedBalances::new);
	}

	@After
	public void teardown() {
		NemStateGlobals.setWeightedBalancesSupplier(null);
		NemStateGlobals.setWeightedBalancesSupplier(TimeBasedVestingWeightedBalances::new);
	}

	@Test
	public void recalculateSetsHeightInAccountImportance() {
		// Act:
		final Collection<AccountState> states = recalculate(1, 2, 3);

		// Assert:
		states.stream().map(AccountState::getImportanceInfo)
				.forEach(accountImportance -> MatcherAssert.assertThat(accountImportance.getHeight(), IsEqual.equalTo(HEIGHT)));
	}

	@Test
	public void recalculateSetsLastPageRankToZero() {
		// Act:
		final Collection<AccountState> states = recalculate(1, 2, 3);

		// Assert:
		states.stream().map(AccountState::getImportanceInfo)
				.forEach(accountImportance -> MatcherAssert.assertThat(accountImportance.getLastPageRank(), IsEqual.equalTo(0.0)));
	}

	@Test
	public void recalculateCalculatesImportanceAccordingToBalance() {
		// Act:
		final Collection<AccountState> states = recalculate(5, 2, 3);
		final Collection<Double> importances = states.stream().map(s -> s.getImportanceInfo().getImportance(HEIGHT))
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(importances, IsEqual.equalTo(Arrays.asList(0.5, 0.2, 0.3)));
	}

	@Test
	public void recalculateCalculatesImportancesThatSumToOne() {
		// Act:
		final Collection<AccountState> states = recalculate(1, 2, 3, 4, 5);
		final Double sum = states.stream().map(state -> state.getImportanceInfo().getImportance(HEIGHT)).reduce(0.0, Double::sum);

		// Assert:
		MatcherAssert.assertThat(sum, IsEqual.equalTo(1.0));
	}

	@Test
	public void recalculateAddsImportanceToHistoricalImportances() {
		// Act:
		final Collection<AccountState> states = recalculate(5, 2, 3);

		// Assert:
		final Collection<Double> importances = states.stream().map(s -> s.getHistoricalImportances().getHistoricalImportance(HEIGHT))
				.collect(Collectors.toList());

		// Assert:
		MatcherAssert.assertThat(importances, IsEqual.equalTo(Arrays.asList(0.5, 0.2, 0.3)));
	}

	private static Collection<AccountState> recalculate(final long... balances) {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(balances);

		// Act:
		calculator.recalculate(HEIGHT, states);
		return states;
	}

	private static AccountState createAccountState(final long balance) {
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		state.getWeightedBalances().addReceive(HEIGHT, Amount.fromNem(balance));
		return state;
	}

	private static Collection<AccountState> createStates(final long... balances) {
		return Arrays.stream(balances).mapToObj(PosImportanceCalculatorTest::createAccountState).collect(Collectors.toList());
	}
}
