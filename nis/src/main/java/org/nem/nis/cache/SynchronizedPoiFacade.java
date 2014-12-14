package org.nem.nis.cache;

import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.state.AccountState;

import java.util.Collection;

/**
 * A synchronized PoiFacade implementation.
 */
public class SynchronizedPoiFacade implements PoiFacade, CopyableCache<SynchronizedPoiFacade> {
	private final DefaultPoiFacade poiFacade;
	private final Object lock = new Object();

	/**
	 * Creates a new cache.
	 *
	 * @param poiFacade The wrapped cache.
	 */
	public SynchronizedPoiFacade(final DefaultPoiFacade poiFacade) {
		this.poiFacade = poiFacade;
	}

	@Override
	public int getLastPoiVectorSize() {
		synchronized (this.lock) {
			return this.poiFacade.getLastPoiVectorSize();
		}
	}

	@Override
	public BlockHeight getLastPoiRecalculationHeight() {
		synchronized (this.lock) {
			return this.poiFacade.getLastPoiRecalculationHeight();
		}
	}

	@Override
	public void recalculateImportances(final BlockHeight blockHeight, final Collection<AccountState> accountStates) {
		synchronized (this.lock) {
			this.poiFacade.recalculateImportances(blockHeight, accountStates);
		}
	}

	//region CopyableCache

	@Override
	public void shallowCopyTo(final SynchronizedPoiFacade rhs) {
		synchronized (rhs.lock) {
			this.poiFacade.shallowCopyTo(rhs.poiFacade);
		}
	}

	@Override
	public SynchronizedPoiFacade copy() {
		synchronized (this.lock) {
			return new SynchronizedPoiFacade(this.poiFacade.copy());
		}
	}

	//endregion
}
