package org.nem.nis.cache;

import java.util.Collection;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

/**
 * A mutable facade on top of pox.
 */
public interface PoxFacade extends ReadOnlyPoxFacade {

	/**
	 * Recalculates the importance of all accounts at the specified block height.
	 *
	 * @param blockHeight The block height.
	 * @param accountStates The account states.
	 */
	void recalculateImportances(final BlockHeight blockHeight, final Collection<AccountState> accountStates);
}
