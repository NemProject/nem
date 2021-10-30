package org.nem.nis.pox;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

import java.util.Collection;

/**
 * Interface for calculating the importance of a collection of accounts at a specific block height.
 */
public interface ImportanceCalculator {

	/**
	 * Recalculates the importance scores for the specified accounts.
	 *
	 * @param blockHeight The block height.
	 * @param accountStates The account states.
	 */
	void recalculate(final BlockHeight blockHeight, final Collection<AccountState> accountStates);
}
