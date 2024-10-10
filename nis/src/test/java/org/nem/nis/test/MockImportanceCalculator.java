package org.nem.nis.test;

import java.util.Collection;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.state.AccountState;

/**
 * A mock ImportanceCalculator implementation.
 */
public class MockImportanceCalculator implements ImportanceCalculator {

	@Override
	public void recalculate(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		accountStates.stream().forEach(a -> a.getImportanceInfo().setImportance(blockHeight, 1.0 / accountStates.size()));
	}
}
