package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

import java.util.Collection;

/**
 * A synchronized PoxFacade implementation.
 */
public class SynchronizedPoxFacade implements PoxFacade, CopyableCache<SynchronizedPoxFacade> {
	private final DefaultPoxFacade poxFacade;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param poxFacade The wrapped cache.
	 */
	public SynchronizedPoxFacade(final DefaultPoxFacade poxFacade) {
		this.poxFacade = poxFacade;
	}

	@Override
	public int getLastVectorSize() {
		synchronized (this.lock) {
			return this.poxFacade.getLastVectorSize();
		}
	}

	@Override
	public BlockHeight getLastRecalculationHeight() {
		synchronized (this.lock) {
			return this.poxFacade.getLastRecalculationHeight();
		}
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		synchronized (this.lock) {
			this.poxFacade.recalculateImportances(blockHeight, accountStates);
		}
	}

	// region CopyableCache

	@Override
	public void shallowCopyTo(final SynchronizedPoxFacade rhs) {
		synchronized (rhs.lock) {
			this.poxFacade.shallowCopyTo(rhs.poxFacade);
		}
	}

	@Override
	public SynchronizedPoxFacade copy() {
		synchronized (this.lock) {
			return new SynchronizedPoxFacade(this.poxFacade.copy());
		}
	}

	// endregion
}
