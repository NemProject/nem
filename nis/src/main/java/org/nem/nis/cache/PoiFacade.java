package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

import java.util.Collection;

/**
 * A repository of all mutable NEM account state.
 */
public interface PoiFacade extends ReadOnlyPoiFacade {

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @param accountStates The account states.
	 */
	public void recalculateImportances(
			final BlockHeight blockHeight,
			final Collection<AccountState> accountStates);
}
