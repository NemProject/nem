package org.nem.nis.pox.pos;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.*;
import org.nem.nis.state.*;

import java.util.*;
import java.util.stream.*;

public class PosImportanceCalculatorTest {

	@Test
	public void recalculateSetsHeightInAccountImportance() {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(1, 2, 3);

		// Act:
		calculator.recalculate(new BlockHeight(123), states);

		// Assert:
		states.stream().map(AccountState::getImportanceInfo).forEach(accountImportance -> Assert.assertThat(
				accountImportance.getHeight(),
				IsEqual.equalTo(new BlockHeight(123))));
	}

	@Test
	public void recalculateSetsLastPageRankToZero() {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(1, 2, 3);

		// Act:
		calculator.recalculate(new BlockHeight(123), states);

		// Assert:
		states.stream().map(AccountState::getImportanceInfo).forEach(accountImportance -> Assert.assertThat(
				accountImportance.getLastPageRank(),
				IsEqual.equalTo(0.0)));
	}

	@Test
	public void recalculateCalculatesImportanceAccordingToBalance() {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(1, 2, 3);

		// Act:
		calculator.recalculate(new BlockHeight(123), states);

		// Assert:
		states.stream().forEach(state -> Assert.assertThat(
				state.getImportanceInfo().getImportance(new BlockHeight(123)),
				IsEqual.equalTo(state.getAccountInfo().getBalance().getNumNem() / 6.0)));
	}

	@Test
	public void recalculateCalculatesImportancesThatSumToOne() {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(1, 2, 3, 4, 5);

		// Act:
		calculator.recalculate(new BlockHeight(123), states);
		final Double sum = states.stream()
				.map(state -> state.getImportanceInfo().getImportance(new BlockHeight(123)))
				.reduce(0.0, Double::sum);

		// Assert:
		Assert.assertThat(sum, IsEqual.equalTo(1.0));
	}

	@Test
	public void recalculateAddsImportanceToHistoricalImportances() {
		// Arrange:
		final PosImportanceCalculator calculator = new PosImportanceCalculator();
		final Collection<AccountState> states = createStates(1, 2, 3);

		// Act:
		calculator.recalculate(new BlockHeight(123), states);

		// Assert:
		states.stream().forEach(state -> Assert.assertThat(
				state.getHistoricalImportances().getHistoricalImportance(new BlockHeight(123)),
				IsEqual.equalTo(state.getAccountInfo().getBalance().getNumNem() / 6.0)));
	}

	private static AccountState createAccountState(final long balance) {
		final AccountState state = new AccountState(Utils.generateRandomAddress());
		final AccountInfo info = state.getAccountInfo();
		info.incrementBalance(Amount.fromNem(balance));
		return state;
	}

	private static Collection<AccountState> createStates(final long... balances) {
		return Arrays.stream(balances).mapToObj(PosImportanceCalculatorTest::createAccountState).collect(Collectors.toList());
	}
}
